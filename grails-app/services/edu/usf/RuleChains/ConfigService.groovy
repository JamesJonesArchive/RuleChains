package edu.usf.RuleChains

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import grails.converters.*
import grails.util.DomainBuilder
import groovy.swing.factory.ListFactory
import groovy.json.JsonSlurper

class ConfigService {
    static transactional = true
    def grailsApplication
    def chainService
    def ruleSetService
    
    def uploadChainData(restore) {
        // def o = JSON.parse(new File('Samples/import.json').text); // Parse a JSON String
        switch(restore) {
            case { (("ruleSets" in it)?checkDuplicateMismatchRuleTypes(it.ruleSets):false) && (("chains" in it)?!checkSources(it.chains):false) }:
                return [ error: "You have duplicate rules using the same name but of a different type. Modify your import so these are uniquely named in the import: ${duplicateRules(restore.ruleSets).join(',')}. You need to create these missing sources first or remove references to them from the import: ${missingSources(restore.chains).join(',')}"]                
                break
            case { (("ruleSets" in it)?checkDuplicateMismatchRuleTypes(it.ruleSets):false) }:
                return [ error: "You have duplicate rules using the same name but of a different type. Modify your import so these are uniquely named in the import: ${duplicateRules(restore.ruleSets).join(',')}"]
                break
            case { (("chains" in it)?!checkSources(it.chains):false) }:
                return [ error: "You need to create these missing sources first or remove references to them from the import: ${missingSources(restore.chains).join(',')}"]
                break
            default:
                if("ruleSets" in restore) {                        
                    restore.ruleSets.each { rs ->
                        print rs.name
                        def ruleSet = { r ->
                            if("error" in r) {
                                r = ruleSetService.addRuleSet(rs.name)
                                if("error" in r) {
                                    println "Error ${r.error}"
                                    return null
                                }
                                return r.ruleSet
                            }
                            return r.ruleSet
                        }.call(ruleSetService.getRuleSet(rs.name))
                        if(!!!!ruleSet) {
                            rs.rules.each { r ->
                                { r2 ->
                                    if(r."class".endsWith("Snippet")) {
                                        { c ->
                                            if("error" in c) {
                                                chainService.addChain(r.name)
                                            }
                                        }.call(chainService.getChain(r.name))                                
                                    }
                                    if("error" in r2) {                                
                                        ruleSetService.addRule(ruleSet.name,r.name,r."class".tokenize('.').last())                                
                                    }
                                    ruleSetService.updateRule(ruleSet.name,r.name,r) 
                                }.call(ruleSetService.getRule(ruleSet.name,r.name))                        
                            }
                        } else {
                            println "error"
                        }
                    }
                } else {
                    println "no ruleSets array"
                }
                if("chains" in restore) {                        
                    restore.chains.each { c ->
                        def chain = { ch ->
                            if("error" in ch) {
                                return chainService.addChain(c.name).chain
                            }
                            return ch.chain
                        }.call(chainService.getChain(c.name)) 
                        if(!!!!chain) {
                            c.links.sort { a, b -> a.sequenceNumber <=> b.sequenceNumber }.each { l ->
                                println "${l.sequenceNumber}"
                                l.sequenceNumber = (!!!l.sequenceNumber)?(chain.links.max { it.sequenceNumber } + 1):l.sequenceNumber
                                println c.name
                                println chainService.getChainLink(c.name,l.sequenceNumber) as JSON
                                if("error" in chainService.getChainLink(c.name,l.sequenceNumber)) {
                                    println "Added chain link"
                                    chainService.addChainLink(c.name,l)
                                    // chain.refresh()
                                } else {
                                    println "Modified chain link"
                                    chainService.modifyChainLink(c.name,l.sequenceNumber,l)
                                }
                                println "${l.sequenceNumber}"
                            }
                        } else {
                            println "error"
                        }
                    }                                        
                } else {
                    println "no chains array"
                }
                return [ status: "complete"]
                break
        }
    }
    
    def downloadChainData() {
        return [
            ruleSets: RuleSet.list(),
            chains: Chain.list(),
            chainServiceHandlers: ChainServiceHandler.list()
        ]
    }
    
    def checkSources(def chains) {
        String sfRoot = "sessionFactory_"
        return grailsApplication.mainContext.beanDefinitionNames.findAll{ it.startsWith( sfRoot ) }.collect { sf ->
            sf[sfRoot.size()..-1]
        }.containsAll(
            chains.collect { c -> 
                return c.links.collect { l-> 
                    l.sourceName 
                }.unique() 
            }.flatten().unique()
        )
    }
    def missingSources(def chains) {
        String sfRoot = "sessionFactory_"
        return { lsources,sources ->
            return lsources.findAll { s ->
                return !!!sources.contains(s)
            }
        }.call(
            chains.collect { c -> 
                return c.links.collect { l-> 
                    l.sourceName 
                }.unique() 
            }.flatten().unique(),
            grailsApplication.mainContext.beanDefinitionNames.findAll{ it.startsWith( sfRoot ) }.collect { sf ->
                sf[sfRoot.size()..-1]
            }
        )
    }
    def checkDuplicateMismatchRuleTypes(def ruleSets) {
        return duplicateRules(ruleSets).size() > 0
    }
    def duplicateRules(def ruleSets) {
        def testRules = []
        ruleSets.each { rs ->
            testRules.addAll(
                rs.rules.collect { r ->
                    return [
                        name: r.name,
                        type: r."class".tokenize('.').last()
                    ]
                }.unique()
            )
        }
        return {l ->
            return l.findAll{l.count(it) > 1}.unique()
        }.call(testRules.unique().collect { it.name })        
    }
}
