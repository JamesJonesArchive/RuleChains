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
        if("ruleSets" in restore) {
            println "Made it this far"
            restore.ruleSets.each { rs ->
                print rs.name
                def ruleSet = { r ->
                    if("error" in r) {
                        r = ruleSetService.addRuleSet(rs.name)
                        if("error" in r) {
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
                                ruleSetService.addRule(ruleSet.name,r.name,r."class")                                
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
                    c.links.each { l ->
                        if(!!!l.sequenceNumber) {
                            chain = { ch ->
                                if("error" in c) {
                                    return chain
                                }
                                l.sequenceNumber = ch.chain.links.max { it.sequenceNumber }
                                return ch.chain
                            }.call(chainService.addChainLink(c.name,l))
                        }
                        chainService.modifyChainLink(c.name,l.sequenceNumber,l)
                    }
                } else {
                    println "error"
                }
            }
        } else {
            println "no chains array"
        }
        return [ status: "complete"]
    }
    
    def downloadChainData() {
        return [
            ruleSets: RuleSet.list(),
            chains: Chain.list()
        ]
    }
}
