package edu.usf.RuleChains

import java.io.FileOutputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.util.regex.Matcher
import java.util.regex.Pattern
import grails.converters.*
import grails.util.DomainBuilder
import groovy.swing.factory.ListFactory
import groovy.json.JsonSlurper
import groovy.io.FileType
import grails.util.GrailsUtil
import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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
                        println(linkFile.name)
                        def link = JSON.parse(linkFile.text)
                        link.sequenceNumber = linkFile.name[0..<linkFile.name.lastIndexOf(".json")].toLong()
                        links << link
                    }
                    restore.chains << [
                        name: chainFolder.name,
                        links: links.sort { a,b -> a.sequenceNumber <=> b.sequenceNumber }.each { l ->
                            println("IMPORTING SEQUENCENUMBER for chain: chainFolder.name -> ${l.sequenceNumber}")
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
    }
    /**
     * Takes the Zip from the upload and merges it into the syncronized
     * Git repository and live database
     * 
     * @param   restore     A JSON Object containing rules,chains and chainServiceHandlers
     * @return              Returns a status object indicating the state of the import
     */
    def uploadChainData(def zipis,def merge = true,boolean isSynced = false) {
        def grailsApplication = new Chain().domainClass.grailsApplication
        def ctx = grailsApplication.mainContext
        def gitFolder = ctx.getResource("git").file  
        def ruleSetsFolder = new File(gitFolder, 'ruleSets')
        def chainsFolder = new File(gitFolder, 'chains')
        def chainServiceHandlersFolder = new File(gitFolder, 'chainServiceHandlers')
        def jobsFolder = new File(gitFolder, 'jobs')        
        if(!merge) {
            // Erase all existing stuff
            if(ruleSetsFolder.exists()) {
                ruleSetsFolder.deleteDir()
            }
            if(chainsFolder.exists()) {
                chainsFolder.deleteDir()
            }
            if(chainServiceHandlersFolder.exists()) {
                chainServiceHandlersFolder.deleteDir()
            }
            if(jobsFolder.exists()) {
                jobsFolder.deleteDir()
            }
        }
        // Make sure all the base folders exist
        ruleSetsFolder.mkdirs()
        chainsFolder.mkdirs()
        chainServiceHandlersFolder.mkdirs()
        jobsFolder.mkdirs()
        // Unzip the restore to temporary restore folder
        def restoreFolder = new File(gitFolder, 'restore')
        if(restoreFolder.exists()) {
            restoreFolder.deleteDir()
        }
        def ze
        while ((ze = zipis.getNextEntry()) != null) {
            File file = new File(restoreFolder, ze.getName())
            if (ze.isDirectory()) {
                file.mkdirs()
            } else {
                file.getParentFile().mkdirs()
                def fileoutputstream = new FileOutputStream(file) 
                byte[] buf = new byte[1024]
                def n
                while ((n = zipis.read(buf, 0, 1024)) > -1) {
                    fileoutputstream.write(buf, 0, n);                        
                }
                fileoutputstream.close()
                zipis.closeEntry() 
            }
        }
        // Merge the RuleSets
        def tempRuleSetsFolder = new File(restoreFolder,"ruleSets")
        if(!tempRuleSetsFolder.exists()) {
            tempRuleSetsFolder.mkdirs()
        }
        tempRuleSetsFolder.eachFile(FileType.DIRECTORIES) { d ->
            def targetRuleSetFolder = new File(ruleSetsFolder,d.name) 
            if(!targetRuleSetFolder.exists()) {
                targetRuleSetFolder.mkdirs()
            }
            d.eachFile(FileType.FILES) { f ->
                (new File(targetRuleSetFolder,f.name)).withWriter { w ->
                    w << new FileInputStream(f)
                }                
            }
        }
        // Merge the Chains
        def tempChainsFolder = new File(restoreFolder,"chains")
        if(!tempChainsFolder.exists()) {
            tempChainsFolder.mkdirs()
        }
        tempChainsFolder.eachFile(FileType.DIRECTORIES) { d ->
            def targetChainFolder = new File(chainsFolder,d.name) 
            if(targetChainFolder.exists()) {
                targetChainFolder.deleteDir()
            }
            targetChainFolder.mkdirs()
            d.eachFile(FileType.FILES) { f ->
                (new File(targetChainFolder,f.name)).withWriter { w ->
                    w << new FileInputStream(f)
                }                
            }            
        }
        // Merge the ChainServiceHandlers
        def tempChainServiceHandlersFolder = new File(restoreFolder,"chainServiceHandlers")
        if(!tempChainServiceHandlersFolder.exists()) {
            tempChainServiceHandlersFolder.mkdirs()
        }
        tempChainServiceHandlersFolder.eachFile(FileType.FILES) { f ->
            (new File(chainServiceHandlersFolder,f.name)).withWriter { w ->
                w << new FileInputStream(f)
            }                
        }
        // Merge the Jobs
        def tempJobsFolder = new File(restoreFolder,"jobs")
        if(!tempJobsFolder.exists()) {
            tempJobsFolder.mkdirs()
        }
        tempJobsFolder.eachFile(FileType.FILES) { f ->
            (new File(jobsFolder,f.name)).withWriter { w ->
                w << new FileInputStream(f)
            }                
        }
        // Remove the restore folder
        if(restoreFolder.exists()) {
            restoreFolder.deleteDir()
        }
        return syncronizeDatabaseFromGit(true)
    }
    /**
     * Returns an input stream containing zipped rules,chains and chainServiceHandlers
     * 
     * @return      An input stream containing zipped rules,chains and chainSeriveHandlers
     */
    def downloadChainData() {
        def grailsApplication = new Chain().domainClass.grailsApplication
        def ctx = grailsApplication.mainContext
        def gitFolder = ctx.getResource("git").file
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream()
            ZipOutputStream zos = new ZipOutputStream(bos)
            gitFolder.eachFileRecurse { f ->
                // we need to keep only relative file path, so we used substring on absolute path
                def fileName = f.getAbsolutePath().substring(gitFolder.getAbsolutePath().length()+1, f.getAbsolutePath().length())
                if(f.isFile() && !fileName.startsWith(".git")) {
                    zos.putNextEntry(new ZipEntry(fileName))
                    f.withInputStream { i ->
                        zos << i
                    }
                    // Close the zip entry
                    zos.closeEntry()
                }
            }
            zos.finish()
            return new ByteArrayInputStream( bos.toByteArray() )
        } catch(ex) {
            println "Error creating zip file: " + ex
        }
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
