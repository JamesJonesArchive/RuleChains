package edu.usf.RuleChains

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import grails.converters.*
import grails.util.DomainBuilder
import groovy.swing.factory.ListFactory
import groovy.json.JsonSlurper
import groovy.io.FileType
import grails.util.GrailsUtil

/**
 * ConfigService provides backup and restoration of rules, chains, chainServiceHandlers
 * and schedules.
 * <p>
 * Developed originally for the University of South Florida
 * 
 * @author <a href='mailto:james@mail.usf.edu'>James Jones</a> 
 */ 
class ConfigService {
    static transactional = true
    def grailsApplication
    def chainService
    def ruleSetService
    def chainServiceHandlerService
    def jobService
    
    /**
     * Initializes the database from a Git repository
     * 
     * @param  isSynced  An optional parameter for syncing to Git. The default value is 'true' keeping sync turned on
     * @return           An object containing the resulting list of Chain objects
     * @see    Chain
     */        
    def syncronizeDatabaseFromGit(boolean isSynced = false) {
        // Clear the Chain/Rule/ChainHandlers data
        ChainServiceHandler.withTransaction { status ->
            ChainServiceHandler.list().each { csh ->
                csh.isSynced = isSynced
                csh.delete()
            }
            status.flush()
        }
        Chain.withTransaction { status ->
            Chain.list().each { c ->
                c.isSynced = isSynced
                c.links*.isSynced = isSynced
                c.delete() 
            }
            status.flush()
        }
        RuleSet.withTransaction { status ->
            RuleSet.list().each { rs ->
                rs.isSynced = isSynced
                rs.rules*.isSynced = isSynced                
                rs.delete() 
            }
            status.flush()
        }
        // Retrieve the Git data and build it into the database
        def gitFolder = new File(grailsApplication.mainContext.getResource('/').file.absolutePath + '/git/')
        def ruleSetsFolder = new File(gitFolder, 'ruleSets')
        def chainsFolder = new File(gitFolder, 'chains')
        def chainServiceHandlersFolder = new File(gitFolder, 'chainServiceHandlers')
        def jobsFolder = new File(gitFolder, 'jobs')
        def restore = [:]
        if(ruleSetsFolder.exists()) {
            restore.ruleSets = []
            RuleSet.withTransaction { status ->
                ruleSetsFolder.eachDir{ ruleSetFolder ->
                    println "Ruleset to create ${ruleSetFolder.name}"
                    ruleSetService.addRuleSet(ruleSetFolder.name,isSynced)
                    def rs = []
                    ruleSetFolder.eachFile(FileType.FILES) { ruleFile ->
                        def rule = JSON.parse(ruleFile.text)
                        rule.name = ruleFile.name[0..<ruleFile.name.lastIndexOf(".json")]
                        rs << rule                    
                        ruleSetService.addRule(ruleSetFolder.name,rule.name,rule["class"].tokenize('.').last(),isSynced)
                        ruleSetService.updateRule(ruleSetFolder.name,rule.name,rule,isSynced)
                    }
                    restore.ruleSets << [ "${ruleSetFolder.name}": rs.collect { rule -> 
                            rule.ruleSet = ruleSetFolder.name
                            rule.isSynced = isSynced
                            return rule
                        },
                        "isSynced": isSynced
                    ]
                }
                status.flush()
            }
        }
        if(chainsFolder.exists()) {
            restore.chains = []
            Chain.withTransaction { status ->
                def chains = []
                chainsFolder.eachDir{ chainFolder ->
                    def links = []
                    println "Chain to create ${chainFolder.name}"
                    chainService.addChain(chainFolder.name,isSynced)
                    chainFolder.eachFile(FileType.FILES) { linkFile ->
                        def link = JSON.parse(linkFile.text)
                        link.sequenceNumber = linkFile.name[0..<linkFile.name.lastIndexOf(".json")].toLong()
                        links << link
                    }
                    restore.chains << [
                        name: chainFolder.name,
                        links: links.sort { a,b -> a.sequenceNumber <=> b.sequenceNumber }.each { l ->
                            chainService.addChainLink(chainFolder.name,l,isSynced)
                        }.collect { l ->
                            l.chain = chainFolder.name
                            l.isSynced = isSynced
                            return l
                        }
                    ]                        
                }
                status.flush()
            }
        }
        if(chainServiceHandlersFolder.exists()) {
            restore.chainServiceHandlers = []
            ChainServiceHandler.withTransaction { status ->
                chainServiceHandlersFolder.eachFile(FileType.FILES) { chainServiceHandlerFile ->
                    def chainServiceHandler = JSON.parse(chainServiceHandlerFile.text)
                    chainServiceHandler.name = chainServiceHandlerFile.name[0..<chainServiceHandlerFile.name.lastIndexOf(".json")]
                    restore.chainServiceHandlers << (chainServiceHandler as Map).inject([isSynced: isSynced]) {c,k,v -> 
                        c[k] = v
                        return c
                    }
                    chainServiceHandlerService.addChainServiceHandler(chainServiceHandler.name,chainServiceHandler.chain,isSynced) 
                    chainServiceHandlerService.modifyChainServiceHandler(chainServiceHandler.name,chainServiceHandler,isSynced)
                }
                status.flush()
            }
        }
        if(jobsFolder.exists()) {
            restore.jobs = []
            jobsFolder.eachFile(FileType.FILES) { jobFile ->
                def job = JSON.parse(jobFile.text)
                job.name = jobFile.name[0..<jobFile.name.lastIndexOf(".json")]
                restore.jobs << job
                def badJob = false
                job.triggers.eachWithIndex { t,i->
                    if(i < 1) {
                        if("error" in jobService.createChainJob(t,job.name,(job.input)?job.input:[[:]],(job.emailLog)?job.emailLog:"")) {
                            // delete the bad schedule
                            badJob = true
                        }
                    } else {
                        jobService.addScheduleChainJob(t,job.name)
                    }
                }
                if(badJob) {
                    def comment = "Syncronizing removal of bad job ${job.name}"
                    withJGit { rf ->
                        pull().call()
                        def relativePath = "jobs/${job.name}.json"
                        jobFile.delete()
                        rm().addFilepattern("${relativePath}").call()
                        commit().setMessage(comment).call()
                        push().call()           
                        pull().call()                        
                    }
                }
            }
        }
        
        println restore as JSON
        // println "Does ruleSets exist? ${ruleSetsFolder.exists()}"
    }
    /**
     * Takes the JSON object from the upload and merges it into the syncronized
     * Git repository and live database
     * 
     * @param   restore     A JSON Object containing rules,chains and chainServiceHandlers
     * @return              Returns a status object indicating the state of the import
     */
    def uploadChainData(def restore,boolean isSynced = false) {
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
                            if("error" in r || !!!r) {
                                r = ruleSetService.addRuleSet(rs.name,isSynced)                                
                                if("error" in r || !!!r) {
                                    println "Error ${(!!!r)?null:r.error}"
                                    return null
                                }
                                return r.ruleSet
                            }
                            return r.ruleSet
                        }.call((GrailsUtil.environment in ['test'])?RuleSet.findByName(rs.name):ruleSetService.getRuleSet(rs.name))                        
                        if(!!!!ruleSet) {
                            (("rules" in rs)?rs.rules:[]).each { r ->
                                { r2 ->
                                    if(r."class".endsWith("Snippet")) {
                                        { c ->
                                            if("error" in c) {
                                                chainService.addChain(r.name,isSynced)
                                            }
                                        }.call(chainService.getChain(r.name))                                
                                    }
                                    if("error" in r2) {                                
                                        ruleSetService.addRule(ruleSet.name,r.name,r."class".tokenize('.').last(),isSynced)                                
                                    }
                                    ruleSetService.updateRule(ruleSet.name,r.name,r,isSynced) 
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
                            if("error" in ch || !!!ch) {
                                if(!!!ch) {
                                    return null
                                }
                                return chainService.addChain(c.name).chain
                            }
                            return ch.chain
                        }.call((GrailsUtil.environment in ['test'])?Chain.findByName(rs.name):chainService.getChain(c.name)) 
                        if(!!!!chain) {
                            c.links.sort { a, b -> a.sequenceNumber <=> b.sequenceNumber }.each { l ->
                                println "${l.sequenceNumber}"
                                l.sequenceNumber = (!!!l.sequenceNumber)?(chain.links.max { it.sequenceNumber } + 1):l.sequenceNumber
                                println c.name
                                println chainService.getChainLink(c.name,l.sequenceNumber) as JSON
                                if("error" in chainService.getChainLink(c.name,l.sequenceNumber)) {
                                    println "Added chain link"
                                    chainService.addChainLink(c.name,l,isSynced)
                                    // chain.refresh()
                                } else {
                                    println "Modified chain link"
                                    chainService.modifyChainLink(c.name,l.sequenceNumber,l,isSynced)
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
    /**
     * Returns an object containing rules,chains and chainServiceHandlers
     * 
     * @return      An object containing rules,chains and chainSeriveHandlers
     */
    def downloadChainData() {
        return [
            ruleSets: RuleSet.list(),
            chains: Chain.list(),
            chainServiceHandlers: ChainServiceHandler.list()
        ]
    }
    /**
     * Iterates through a chain and checks to ensure all sources exist before importing
     * 
     * @param   chains  An object containing an array of chains
     * @return          A boolean which indicates all sources exists
     */
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
    /**
     * Compares existing sources with sources specified in a chain and returns
     * what sources are missing.
     * 
     * @param     chains  An object containing an array of chains
     * @return            An array of source names that don't exist
     */
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
    /**
     * Checks to determine if there are duplicate rule defined
     * 
     * @param  ruleSets  A list of rule sets containing rules belonging to those rule sets
     * @return           True or False on duplicates being detected
     */
    def checkDuplicateMismatchRuleTypes(def ruleSets) {
        return duplicateRules(ruleSets).size() > 0
    }
    /**
     * Iterates through rule sets and finds the duplicates
     * 
     * @param  ruleSets  A list of rule sets containing rules belonging to those rule sets
     * @return           A list of duplicate rules detected
     */
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
