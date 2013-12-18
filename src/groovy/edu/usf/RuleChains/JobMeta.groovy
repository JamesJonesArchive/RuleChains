/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.usf.RuleChains
import static org.quartz.impl.matchers.GroupMatcher.*
import static org.quartz.TriggerBuilder.*
import grails.plugin.quartz2.ClosureJob
import org.quartz.*
import static org.quartz.CronScheduleBuilder.cronSchedule
import grails.converters.*

/**
 *
 * @author James Jones
 */
class JobMeta {
    def buildMeta = {quartzScheduler->
        JobService.metaClass.listChainJobs  = {->  
            return [ 
                jobGroups: quartzScheduler.getJobGroupNames().collect { g ->
                    return [
                        name: g,
                        jobs: quartzScheduler.getJobKeys(groupEquals(g)).collect { jk ->
                            def jobDataMap = quartzScheduler.getJobDetail(jk).getJobDataMap()
                            return [
                                name: jk.name,
                                triggers: quartzScheduler.getTriggersOfJob(jk).collect { t ->
                                    return t.getCronExpression() 
                                },
                                chain: jobDataMap.get("chain"),
                                input: jobDataMap.get("input")
                            ]
                        }.findAll {
                            it.triggers.size() > 0
                        }
                    ]
                }
            ]            
        }
        JobService.metaClass.createChainJob = { String cronExpression,String name,def input = [] ->
            def suffix = System.currentTimeMillis()
            name = { parts->
                if(parts.size() > 1) {
                    suffix = parts[1]
                    return parts[0]
                }
                return name
            }.call(name.split(":"))
            if((quartzScheduler.getJobGroupNames().findAll { g -> return quartzScheduler.getJobKeys(groupEquals(g)).collect { it.name }.contains(name) }.size() > 0)) {
                def jobKey = quartzScheduler.getJobKeys(
                    groupEquals(quartzScheduler.getJobGroupNames().find { g -> return quartzScheduler.getJobKeys(groupEquals(g)).collect { it.name }.contains(name) })
                ).find { jk -> return (it.name == name) }                    
                try {
                    try {
                        def trigger = newTrigger()
                        .withIdentity("${name}:${suffix}")
                        .withSchedule(cronSchedule(cronExpression))
                        .forJob(jobKey)
                        .usingJobData("gitAuthorInfo",delegate.getGitAuthorInfo())
                        .build();  

                        return [
                            date: quartzScheduler.scheduleJob(trigger)                                
                        ]                        
                    } catch (ex) {
                        return [
                            error: ex.getLocalizedMessage()
                        ]                        
                    } finally {                                                
                        delegate.handleGitWithComment("Updating Scheduled Job ${jobKey.name}") { git,gitFolder,comment ->
                            def jobDetail = quartzScheduler.getJobDetail(jobKey) 
                            def dataMap = jobDetail.getJobDataMap()
                            def gitAuthorInfo = delegate.getGitAuthorInfo()
                            def relativePath = "jobs/${jobKey.name}.json"
                            def f = new File(gitFolder,relativePath)
                            f.text = {js->
                                js.setPrettyPrint(true)
                                return js                            
                            }.call([
                                group: jobKey.group,
                                name: jobKey.name,
                                triggers: quartzScheduler.getTriggersOfJob(jobKey).collect { it.getCronExpression() },
                                chain: dataMap.getString("chain"),
                                input: dataMap.get("input")
                            ] as JSON)
                            git.add().addFilepattern("${relativePath}").call()
                            if(!git.status().call().isClean()) {
                                git.commit().setAuthor(gitAuthorInfo.user,gitAuthorInfo.email).setMessage(comment).call()
                            }
                        }                                                
                    }
                } catch (ex) {                        
                    return [
                        error: ex.getLocalizedMessage()
                    ]                        
                }
            } else {
                try {
                    def jobDetail = ClosureJob.createJob(name:"${name}:${suffix}",durability:true,concurrent:false,jobData: [input: input,chain: name,gitAuthorInfo: delegate.getGitAuthorInfo()]){ jobCtx , appCtx->
                        log.info "************* it ran ***********"
                        def chain = Chain.findByName(jobCtx.mergedJobDataMap.get('chain'))                        
                        if(!!chain) {
                            // Attaches a JobHistory to the Chain as a transient
                            chain.jobHistory = { jh,d -> 
                                if('error' in jh) {
                                    log.info "Creating a new job history"
                                    jh = d.addJobHistory("${name}:${suffix}")
                                    return ('error' in jh)?null:jh.jobHistory
                                }
                                return jh.jobHistory
                            }.call(delegate.findJobHistory("${name}:${suffix}"),delegate)
                            if(!!chain.jobHistory) {
                                chain.jobHistory.updateJobProperties(jobCtx)
                            } else {
                                log.error "Job History is NULL and won't be used to log execution"
                            }
                            def result = chain.execute(jobCtx.mergedJobDataMap.get('input'))
                            println "Result is ${result}"
                            chain.jobHistory.appendToLog("[Finished] ${name}:${suffix}")                            
                        } else {
                            log.error "Chain not found ${jobCtx.mergedJobDataMap.get('chain')}"
                        }
                    }
                    try {
                        def trigger = newTrigger().withIdentity("${name}:${suffix}")
                        .withSchedule(cronSchedule(cronExpression))
                        .build()
                        return [
                            date: quartzScheduler.scheduleJob(jobDetail, trigger)                                
                        ]
                    } catch (ex) {
                        return [
                            error: ex.getLocalizedMessage()
                        ]                        
                    }
                } catch (ex) {
                    return [
                        error: ex.getLocalizedMessage()
                    ]                        
                }
            }
        }
        JobService.metaClass.updateChainJob { String name,String newName ->
            def suffix = System.currentTimeMillis()
            name = { parts->
                if(parts.size() > 1) {
                    suffix = parts[1]
                    return parts[0]
                }
                return name
            }.call(name.split(":"))
            if((quartzScheduler.getJobGroupNames().findAll { g -> return quartzScheduler.getJobKeys(groupEquals(g)).collect { jk ->
                            jk.name 
                        }.contains(name) }.size() > 0)) {
                def jobKey = quartzScheduler.getJobKeys(
                    groupEquals(quartzScheduler.getJobGroupNames().find { g -> return quartzScheduler.getJobKeys(groupEquals(g)).collect { it.name }.contains(name) })
                ).find { jk -> return (jk.name == name) }
                def jobDataMap = quartzScheduler.getJobDetail(jobKey).getJobDataMap()
                def jobDetail = ClosureJob.createJob(name:"${newName}:${suffix}",durability:true,concurrent:false,jobData: [input: jobDataMap.get("input"),chain: newName,gitAuthorInfo: delegate.getGitAuthorInfo()]){ jobCtx , appCtx->
                    log.info "************* it ran ***********"
                    def chain = Chain.findByName(jobCtx.mergedJobDataMap.get('chain'))
                    if(!!chain) {
                        // Attaches a JobHistory to the Chain as a transient
                        chain.jobHistory = { jh,d -> 
                            if('error' in jh) {
                                log.info "Creating a new job history"
                                jh = d.addJobHistory("${name}:${suffix}")
                                return ('error' in jh)?null:jh.jobHistory
                            }
                            return jh.jobHistory
                        }.call(delegate.findJobHistory("${name}:${suffix}"),delegate)
                        if(!!chain.jobHistory) {
                            chain.jobHistory.updateJobProperties(jobCtx)
                        } else {
                            log.error "Job History is NULL and won't be used to log execution"
                        }
                        def result = chain.execute(jobCtx.mergedJobDataMap.get('input'))
                        println "Result is ${result}"
                    } else {
                        log.error "Chain not found ${jobCtx.mergedJobDataMap.get('chain')}"                        
                    }
                }
                quartzScheduler.scheduleJobs([
                        (jobDetail):  quartzScheduler.getTriggersOfJob(jobKey).collect { t ->
                            return newTrigger()
                            .withIdentity("${name}:${suffix}")
                            .withSchedule(cronSchedule(t.getCronExpression()))
                            .forJob(jobDetail.getKey())
                            .build()
                        }
                    ],true)
                return [
                    updated: quartzScheduler.deleteJob(jobKey)
                ]
            }
            return [
                updated: false
            ]
        }
        JobService.metaClass.removeChainJob { String name ->
            def results = []
            quartzScheduler.getJobGroupNames().findAll { g ->
                if(quartzScheduler.getJobKeys(groupEquals(g)).collect { it.name }.contains(name)) {
                    return !!!!quartzScheduler.getJobKeys(groupEquals(g)).findAll { jk ->
                        return (jk.name == name)                           
                    }
                } else {
                    return false
                }                    
            }.each { g ->
                quartzScheduler.getJobKeys(groupEquals(g)).findAll { jk ->
                    return (jk.name == name)                           
                }.each { jk ->
                    delegate.handleGitWithComment("Removing Scheduled Job ${jk.name}") { git,gitFolder,comment ->
                        def relativePath = "jobs/${jk.name}.json"
                        def gitAuthorInfo = delegate.getGitAuthorInfo()        
                        def f = new File(gitFolder,relativePath)
                        if(f.exists()) {
                            f.delete()
                            git.rm().addFilepattern("${relativePath}").call()
                            if(!git.status().call().isClean()) {
                                git.commit().setAuthor(gitAuthorInfo.user,gitAuthorInfo.email).setMessage(comment).call()
                            }
                        }
                    }                                            
                    // Delete the whole job
                    results << [
                        jobName: jk.name,
                        jobGroup: g,
                        removed: quartzScheduler.deleteJob(jk)
                    ]                            
                }
            }
            return [ status: results ]
        }
        JobService.metaClass.unscheduleChainJob { String cronExpression, String name ->
            def results = []
            println(cronExpression)
            quartzScheduler.getJobGroupNames().findAll { g ->
                if(quartzScheduler.getJobKeys(groupEquals(g)).collect { it.name }.contains(name)) {
                    return !!!!quartzScheduler.getJobKeys(groupEquals(g)).findAll { jk ->
                        return ((jk.name == name) && (quartzScheduler.getTriggersOfJob(jk).collect { it.getCronExpression() }.contains(cronExpression)))                            
                    }
                } else {
                    return false
                }                    
            }.each { g ->    
                println(g)
                quartzScheduler.getJobKeys(groupEquals(g)).findAll { jk ->
                    return ((jk.name == name) && (quartzScheduler.getTriggersOfJob(jk).collect { it.getCronExpression() }.contains(cronExpression)))                            
                }.each { jk ->
                    if(quartzScheduler.getTriggersOfJob(jk).size() > 1) {
                        // Delete the trigger only
                        quartzScheduler.getTriggersOfJob(jk).findAll { it.getCronExpression() == cronExpression }.collect { t ->
                            results << [
                                jobName: jk.name,
                                jobGroup: g,
                                unscheduled: quartzScheduler.unscheduleJob(t.getKey())
                            ]
                        }         
                        delegate.handleGitWithComment("Saving Scheduled Job ${jk.name}") { git,gitFolder,comment ->
                            def jobDetail = quartzScheduler.getJobDetail(jk) 
                            def dataMap = jobDetail.getJobDataMap()
                            def gitAuthorInfo = delegate.getGitAuthorInfo()
                            def relativePath = "jobs/${jk.name}.json"
                            def f = new File(gitFolder,relativePath)
                            f.text = {js->
                                js.setPrettyPrint(true)
                                return js                            
                            }.call([
                                group: jk.group,
                                name: jk.name,
                                triggers: quartzScheduler.getTriggersOfJob(jk).collect { it.getCronExpression() },
                                chain: dataMap.getString("chain"),
                                input: dataMap.get("input")
                            ] as JSON)
                            git.add().addFilepattern("${relativePath}").call()
                            if(!git.status().call().isClean()) {
                                git.commit().setAuthor(gitAuthorInfo.user,gitAuthorInfo.email).setMessage(comment).call()
                            }
                        }                        
                    } else {
                        delegate.handleGitWithComment("Removing Scheduled Job ${jk.name}") { git,gitFolder,comment ->
                            def relativePath = "jobs/${jk.name}.json"
                            def gitAuthorInfo = delegate.getGitAuthorInfo()        
                            def f = new File(gitFolder,relativePath)
                            if(f.exists()) {
                                f.delete()
                                git.rm().addFilepattern("${relativePath}").call()
                                if(!git.status().call().isClean()) {
                                    git.commit().setAuthor(gitAuthorInfo.user,gitAuthorInfo.email).setMessage(comment).call()
                                }
                            }
                        }                        
                        results << [
                            jobName: jk.name,
                            jobGroup: g,
                            removed: quartzScheduler.deleteJob(jk)
                        ]                            
                    }
                }
            }
            if(results.size() > 0) {
                return [ status: results ]
            } else {
                return [ error: "Did not match an existing trigger!" ]
            }
        }
        JobService.metaClass.rescheduleChainJob { String cronExpression, String newCronExpression, String name ->
            def results = []
            quartzScheduler.getJobGroupNames().findAll { g ->
                if(quartzScheduler.getJobKeys(groupEquals(g)).collect { it.name }.contains(name)) {
                    return !!!!quartzScheduler.getJobKeys(groupEquals(g)).findAll { jk ->
                        return ((jk.name == name) && (quartzScheduler.getTriggersOfJob(jk).collect { it.getCronExpression() }.contains(cronExpression)))                            
                    }
                } else {
                    return false
                }                    
            }.each { g ->
                quartzScheduler.getJobKeys(groupEquals(g)).findAll { jk ->
                    return ((jk.name == name) && (quartzScheduler.getTriggersOfJob(jk).collect { it.getCronExpression() }.contains(cronExpression)))                            
                }.each { jk ->
                    quartzScheduler.getTriggersOfJob(jk).findAll { it.getCronExpression() == cronExpression }.each { t ->
                        results << [
                            jobName: jk.name,
                            jobGroup: g,
                            scheduled: quartzScheduler.rescheduleJob(t.getKey(), t.getTriggerBuilder().withSchedule(cronSchedule(newCronExpression)).build())
                        ]                            
                    }
                    delegate.handleGitWithComment("Updating Scheduled Job ${jk.name}") { git,gitFolder,comment ->
                        def jobDetail = quartzScheduler.getJobDetail(jk) 
                        def dataMap = jobDetail.getJobDataMap()
                        def gitAuthorInfo = delegate.getGitAuthorInfo()
                        def relativePath = "jobs/${jk.name}.json"
                        def f = new File(gitFolder,relativePath)
                        f.text = {js->
                            js.setPrettyPrint(true)
                            return js                            
                        }.call([
                            group: jk.group,
                            name: jk.name,
                            triggers: quartzScheduler.getTriggersOfJob(jk).collect { it.getCronExpression() },
                            chain: dataMap.getString("chain"),
                            input: dataMap.get("input")
                        ] as JSON)
                        git.add().addFilepattern("${relativePath}").call()
                        if(!git.status().call().isClean()) {
                            git.commit().setAuthor(gitAuthorInfo.user,gitAuthorInfo.email).setMessage(comment).call()
                        }
                    }                                                
                }
            }
            return [ status: results ]
        }
        JobService.metaClass.addscheduleChainJob { String cronExpression, String name ->
            def suffix = System.currentTimeMillis()
            name = { parts->
                if(parts.size() > 1) {
                    suffix = parts[1]
                    return parts[0]
                }
                return name
            }.call(name.split(":"))
            if((quartzScheduler.getJobGroupNames().findAll { g -> return quartzScheduler.getJobKeys(groupEquals(g)).collect { jk ->
                            println "${jk.name} and ${name}"
                            jk.name 
                        }.contains(name) }.size() > 0)) {
                def jobKey = quartzScheduler.getJobKeys(
                    groupEquals(quartzScheduler.getJobGroupNames().find { g -> return quartzScheduler.getJobKeys(groupEquals(g)).collect { it.name }.contains(name) })
                ).find { jk -> return (jk.name == name) }
                try {
                    def trigger = newTrigger()
                    .withIdentity("${name}:${suffix}")
                    .withSchedule(cronSchedule(cronExpression))
                    .forJob(jobKey)
                    .usingJobData("gitAuthorInfo",delegate.getGitAuthorInfo())
                    .build()
                    return [
                        date: quartzScheduler.scheduleJob(trigger)                                
                    ]
                } catch (ex) {
                    return [
                        error: ex.getLocalizedMessage()
                    ]                        
                } finally {
                    delegate.handleGitWithComment("Updating Scheduled Job ${jobKey.name}") { git,gitFolder,comment ->
                        def jobDetail = quartzScheduler.getJobDetail(jobKey) 
                        def dataMap = jobDetail.getJobDataMap()
                        def gitAuthorInfo = delegate.getGitAuthorInfo()
                        def relativePath = "jobs/${jobKey.name}.json"
                        def f = new File(gitFolder,relativePath)
                        f.text = {js->
                            js.setPrettyPrint(true)
                            return js                            
                        }.call([
                            group: jobKey.group,
                            name: jobKey.name,
                            triggers: quartzScheduler.getTriggersOfJob(jobKey).collect { it.getCronExpression() },
                            chain: dataMap.getString("chain"),
                            input: dataMap.get("input")
                        ] as JSON)
                        git.add().addFilepattern("${relativePath}").call()
                        if(!git.status().call().isClean()) {
                            git.commit().setAuthor(gitAuthorInfo.user,gitAuthorInfo.email).setMessage(comment).call()
                        }
                    }                                                
                }
            }
            return [ error: "Job not found" ]
        }
        JobService.metaClass.mergescheduleChainJob { String mergeName, String name ->
            def suffix = System.currentTimeMillis()
            name = { parts->
                if(parts.size() > 1) {
                    suffix = parts[1]
                    return parts[0]
                }
                return name
            }.call(name.split(":"))
            if((quartzScheduler.getJobGroupNames().findAll { g -> return quartzScheduler.getJobKeys(groupEquals(g)).collect { jk ->
                            jk.name 
                        }.contains(name) }.size() > 0) &&
                (quartzScheduler.getJobGroupNames().findAll { g -> return quartzScheduler.getJobKeys(groupEquals(g)).collect { jk ->
                            jk.name 
                        }.contains(name) }.size() > 0)) {
                def jobKey = quartzScheduler.getJobKeys(
                    groupEquals(quartzScheduler.getJobGroupNames().find { g -> return quartzScheduler.getJobKeys(groupEquals(g)).collect { it.name }.contains(name) })
                ).find { jk -> return (jk.name == name) }
                def removedJobKey = quartzScheduler.getJobKeys(
                    groupEquals(quartzScheduler.getJobGroupNames().find { g -> return quartzScheduler.getJobKeys(groupEquals(g)).collect { it.name }.contains(name) })
                ).find { jk -> return (jk.name == mergeName) }  
                def results = [
                    mergedTriggers: quartzScheduler.getTriggersOfJob(removedJobKey).findAll{ t -> !!!(t.getCronExpression() in quartzScheduler.getTriggersOfJob(jobKey).collect { it.getCronExpression() }) }.collect { t ->
                        return quartzScheduler.scheduleJob(t.getTriggerBuilder().withIdentity("${name}:${suffix}").forJob(jobKey).build())
                    }
                ]
                // Git removes this one.
                delegate.handleGitWithComment("Removing Scheduled Job ${removedJobKey.name}") { git,gitFolder,comment ->
                    def relativePath = "jobs/${removedJobKey.name}.json"
                    def gitAuthorInfo = delegate.getGitAuthorInfo()        
                    def f = new File(gitFolder,relativePath)
                    if(f.exists()) {
                        f.delete()
                        git.rm().addFilepattern("${relativePath}").call()
                        if(!git.status().call().isClean()) {
                            git.commit().setAuthor(gitAuthorInfo.user,gitAuthorInfo.email).setMessage(comment).call()
                        }
                    }
                }
                // Git updates the merged one
                delegate.handleGitWithComment("Updating Scheduled Job ${jobKey.name}") { git,gitFolder,comment ->
                    def jobDetail = quartzScheduler.getJobDetail(jobKey) 
                    def dataMap = jobDetail.getJobDataMap()
                    def gitAuthorInfo = delegate.getGitAuthorInfo()
                    def relativePath = "jobs/${jobKey.name}.json"
                    def f = new File(gitFolder,relativePath)
                    f.text = {js->
                        js.setPrettyPrint(true)
                        return js                            
                    }.call([
                        group: jobKey.group,
                        name: jobKey.name,
                        triggers: quartzScheduler.getTriggersOfJob(jobKey).collect { it.getCronExpression() },
                        chain: dataMap.getString("chain"),
                        input: dataMap.get("input")
                    ] as JSON)
                    git.add().addFilepattern("${relativePath}").call()
                    if(!git.status().call().isClean()) {
                        git.commit().setAuthor(gitAuthorInfo.user,gitAuthorInfo.email).setMessage(comment).call()
                    }
                }                                                                
                results.delete = quartzScheduler.deleteJob(removedJobKey)
                return results
            }
            return [ error: "One of the jobs was not found" ]
        }
        JobService.metaClass.listCurrentlyExecutingJobs {->
            return [
                executingJobs: quartzScheduler.getCurrentlyExecutingJobs().collect { jec->
                    return [
                        chain: jec.getJobDetail().getJobDataMap().get("chain"),
                        name: jec.getJobDetail().getKey().getName(),
                        description: jec.getJobDetail().getDescription(),
                        group: jec.getJobDetail().getKey().getGroup(),
                        cron: { t ->
                            return t.metaClass.respondsTo(t, 'getCronExpression')?t.getCronExpression():""
                        }.call(jec.getTrigger()),
                        fireTime: jec.getFireTime(),
                        scheduledFireTime: jec.getScheduledFireTime(),
                        input: jec.getJobDetail().getJobDataMap().get("input")
                    ]
                }
            ]
        }
        RuleChainsSchedulerListener.metaClass.accessScheduler {Closure closure->
            closure.delegate = delegate
            closure.call(quartzScheduler)
        }
    }
}

