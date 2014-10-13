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
 * JobMeta performs all the metaprogramming for the accessing 
 * the quartz scheduling directly.
 * <p>
 * Developed originally for the University of South Florida
 * 
 * @author <a href='mailto:james@mail.usf.edu'>James Jones</a> 
 */ 
class JobMeta {
    /**
     * The builder method that creates all the metaprogramming methods
     * 
     * @param   quartzScheduler      The Quartz Plugin scheduler object
     */        
    def buildMeta = {quartzScheduler->
        /**
         * Generates a list of Quartz RuleChains jobs
         * 
         * @return     A list of objects containing Quartz RuleChains jobs
         */
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
                                input: jobDataMap.get("input"),
                                emailLog: jobDataMap.get("emailLog")
                            ]
                        }.findAll {
                            it.triggers.size() > 0
                        }
                    ]
                }
            ]            
        }
        /**
         * Creates a Quartz RuleChain job
         * 
         * @param      cronExpression      A string containing a Quartz CRON style expression
         * @param      name                The name of the new job. Either the name of the chain itself or the name of the chain and number
         * @param      input               An optional array containing objects to be used as input on the chain execution
         * @return                         Returns an object with the schedule date
         */
        JobService.metaClass.createChainJob = { String cronExpression,String name,def input = [[:]], String email = "" ->
            if((quartzScheduler.getJobGroupNames().findAll { g -> return quartzScheduler.getJobKeys(groupEquals(g)).collect { it.name }.contains(name) }.size() > 0)) {
                // This Job exists, return error
                return [ error: "Job exists! Cannot be created!" ]
            } else {
                // Create the new job
                try {
                    def suffix = System.currentTimeMillis()
                    if(name.tokenize(':').size() > 1) {
                        (name,suffix) = name.tokenize(':')
                    }
                    def jobDetail = ClosureJob.createJob(name:"${name}:${suffix}",durability:false,concurrent:false,jobData: [input: input,chain: name,gitAuthorInfo: delegate.userInfoHandlerService.getGitAuthorInfo(),emailLog: email]){ jobCtx , appCtx->
                        def chain = Chain.findByName(jobCtx.mergedJobDataMap.get('chain'))                        
                        if(!!chain) {
                            // Attaches a JobInfo map to the chain as a transient
                            chain.jobInfo = [
                                chain:name,
                                suffix:suffix,
                                description: ((!!!!jobCtx.getJobDetail().getDescription())?jobCtx.getJobDetail().getDescription():""),
                                groupName: jobCtx.getJobDetail().getKey().getGroup(),
                                cron: { t ->
                                    return t.metaClass.respondsTo(t, 'getCronExpression')?t.getCronExpression():""
                                }.call(jobCtx.getTrigger()),
                                fireTime: jobCtx.getFireTime(),
                                scheduledFireTime: jobCtx.getScheduledFireTime()   
                            ]
                            def result = chain.execute(jobCtx.mergedJobDataMap.get('input'))
                            delegate.log.info("[Chain:${name}:${suffix}][${name}][RESULT] ${result as JSON}")
                            delegate.log.info("[Chain:${name}:${suffix}][${name}][END_EXECUTE] Chain ${name}:${suffix}")
                        } else {
                            delegate.log.info("[Chain:${name}:${suffix}][${name}][END_EXECUTE] Chain not found: ${name}")
                        }
                        delegate.emailJobLog(jobCtx.mergedJobDataMap.get('emailLog'),"${name}:${suffix}")
                    }
                    try {
                        def trigger = newTrigger()
                        .withSchedule(cronSchedule(cronExpression))
                        .build()
                        return [
                            jobName: "${name}:${suffix}",
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
        /**
         * Updates an email address for log reporting in an existing Quartz RuleChain job
         * 
         * @param      name     The name of the target job
         * @param      email    The new email address for the target job
         * @return              Returns an object with the removal status and job name or error response
         */
        JobService.metaClass.updateChainJobEmailLog { String name, String email ->            
            if((quartzScheduler.getJobGroupNames().findAll { g -> return quartzScheduler.getJobKeys(groupEquals(g)).collect { it.name }.contains(name) }.size() > 0)) {
                def jobKey = quartzScheduler.getJobKeys(
                    groupEquals(quartzScheduler.getJobGroupNames().find { g -> return quartzScheduler.getJobKeys(groupEquals(g)).collect { it.name }.contains(name) })
                ).find { jk -> return (jk.name == name) }
                def(chainName,suffix) = name.tokenize(':')
                def newSuffix = System.currentTimeMillis()
                def jobDataMap = quartzScheduler.getJobDetail(jobKey).getJobDataMap()
                // Get a list of the old CRONs
                def cronList = quartzScheduler.getTriggersOfJob(jobKey).collect { it.getCronExpression() }
                // Schedule a new job with the same attributes
                def createStatus = delegate.createChainJob(cronList.pop(),chainName,jobDataMap.get("input"),email)
                if("jobName" in createStatus) {                    
                    cronList.each { cron ->
                        delegate.addScheduleChainJob(cron,createStatus.jobName)
                    }
                    // Remove the old job
                    createStatus.removed = delegate.removeChainJob(name)?.status
                }
                return createStatus
            } else {
                println "The job ${name} is not matching"
                return [
                    status: [
                        jobName: name,
                        removed: false
                    ]
                ]
            }
        }
        /**
         * Renames an existing Quartz RuleChain job
         * 
         * @param      name     The name of the target job
         * @param      newName  The new name for the target job
         * @return              Returns an object with the schedule date or error response
         */
        JobService.metaClass.updateChainJob { String name,String newName ->
            if((quartzScheduler.getJobGroupNames().findAll { g -> return quartzScheduler.getJobKeys(groupEquals(g)).collect { it.name }.contains(name) }.size() > 0)) {
                def jobKey = quartzScheduler.getJobKeys(
                    groupEquals(quartzScheduler.getJobGroupNames().find { g -> return quartzScheduler.getJobKeys(groupEquals(g)).collect { it.name }.contains(name) })
                ).find { jk -> return (jk.name == name) }
                def(chainName,suffix) = name.tokenize(':')
                def newSuffix = System.currentTimeMillis()
                def jobDataMap = quartzScheduler.getJobDetail(jobKey).getJobDataMap()
                // Get a list of the old CRONs
                def cronList = quartzScheduler.getTriggersOfJob(jobKey).collect { it.getCronExpression() }
                // Schedule a new job with the same attributes
                def createStatus = delegate.createChainJob(cronList.pop(),newName,jobDataMap.get("input"),jobDataMap.get("emailLog"))
                if("jobName" in createStatus) {                    
                    cronList.each { cron ->
                        delegate.addScheduleChainJob(cron,createStatus.jobName)
                    }
                    // Remove the old job
                    createStatus.removed = delegate.removeChainJob(name)?.status
                }
                return createStatus
            } else {
                println "The job ${name} is not matching"
                return [
                    status: [
                        jobName: name,
                        removed: false
                    ]
                ]
            }            
        }
        /**
         * Removes an existing Quartz RuleChain job
         * 
         * @param     name     The name of the Quartz RuleChain job to remove
         * @return             An object with a status containing an array of operation statuses
         */
        JobService.metaClass.removeChainJob { String name ->
            def(chainName,suffix) = name.tokenize(':')
            if((quartzScheduler.getJobGroupNames().findAll { g -> return quartzScheduler.getJobKeys(groupEquals(g)).collect { it.name }.contains(name) }.size() > 0)) {
                def jobKey = quartzScheduler.getJobKeys(
                    groupEquals(quartzScheduler.getJobGroupNames().find { g -> return quartzScheduler.getJobKeys(groupEquals(g)).collect { it.name }.contains(name) })
                ).find { jk -> return (jk.name == name) }
                def deleted = quartzScheduler.deleteJob(jobKey)
                if(deleted) {
                    def comment = "Removing Scheduled Job ${jobKey.name}"
                    delegate.withJGit { rf ->
                        def relativePath = "jobs/${jobKey.name}.json"
                        def f = new File(rf,relativePath)
                        pull().call()
                        if(f.exists()) {
                            f.delete()
                            rm().addFilepattern("${relativePath}").call()
                            if(!status().call().isClean()) {
                                commit().setMessage(comment).call()
                            }
                            push().call()
                            pull().call()
                        }
                    }
                }
                return [ status: deleted ]   
            } else {
                return [ error: "Job not found" ]
            }
        }        
        /**
         * Removes a schedule trigger from an existing Quartz RuleChains job. This
         * will not remove the job itself unless there are no more triggers associated
         * with it.
         * 
         * @param      cronExpression      A string containing a Quartz CRON style expression to be removed
         * @param      name                The name of the job
         * @return                         An object with a status containing an array of operation statuses 
         */
        JobService.metaClass.unscheduleChainJob { String cronExpression, String name ->
            def(chainName,suffix) = name.tokenize(':')
            if((quartzScheduler.getJobGroupNames().findAll { g -> return quartzScheduler.getJobKeys(groupEquals(g)).collect { it.name }.contains(name) }.size() > 0)) {
                def jobKey = quartzScheduler.getJobKeys(
                    groupEquals(quartzScheduler.getJobGroupNames().find { g -> return quartzScheduler.getJobKeys(groupEquals(g)).collect { it.name }.contains(name) })
                ).find { jk -> return (jk.name == name) }
                def cronList = quartzScheduler.getTriggersOfJob(jobKey).collect { it.getCronExpression() }
                if(cronList.contains(cronExpression)) {
                    if(cronList.size() < 2) {
                        // This whole job needs to be deleted
                        return delegate.removeChainJob(name)
                    } else {
                        // Just remove this trigger
                        def unscheduled = quartzScheduler.unscheduleJob(quartzScheduler.getTriggersOfJob(jobKey).find { t -> t.getCronExpression() == cronExpression }.getKey())
                        if(unscheduled) {
                            def comment = "Saving Scheduled Job ${jobKey.name}"
                            delegate.withJGit { rf ->
                                def jobDetail = quartzScheduler.getJobDetail(jobKey) 
                                def dataMap = jobDetail.getJobDataMap()
                                def relativePath = "jobs/${jobKey.name}.json"
                                def f = new File(rf,relativePath)
                                pull().call()
                                f.text = {js->
                                    js.setPrettyPrint(true)
                                    return js                            
                                }.call([
                                    group: jobKey.group,
                                    name: jobKey.name,
                                    triggers: quartzScheduler.getTriggersOfJob(jobKey).collect { it.getCronExpression() },
                                    chain: dataMap.getString("chain"),
                                    input: dataMap.get("input"),
                                    emailLog: dataMap.get("emailLog")
                                ] as JSON)
                                add().addFilepattern("${relativePath}").call()
                                if(!status().call().isClean()) {
                                    commit().setMessage(comment).call()
                                }
                                push().call()
                                pull().call()
                            }
                        }
                        return [ status: unscheduled ]                        
                    }
                } else {
                    return [ error: "Did not match an existing trigger!" ]
                }
            } else {
                return [ error: "Job not found" ]
            }
        }
        /**
         * Reschedules an existing Quartz RuleChains job with a new schedule.
         * 
         * @param      cronExpression      A string containing a Quartz CRON style expression to be replaced
         * @param      newCronExpression   A string containing a Quartz CRON style expression to be applied
         * @param      name                The name of the job
         * @return                         An object with a status containing an array of operation statuses 
         */
        JobService.metaClass.rescheduleChainJob { String cronExpression, String newCronExpression, String name ->
            if((quartzScheduler.getJobGroupNames().findAll { g -> return quartzScheduler.getJobKeys(groupEquals(g)).collect { it.name }.contains(name) }.size() > 0)) {
                def jobKey = quartzScheduler.getJobKeys(
                    groupEquals(quartzScheduler.getJobGroupNames().find { g -> return quartzScheduler.getJobKeys(groupEquals(g)).collect { it.name }.contains(name) })
                ).find { jk -> return (jk.name == name) }
                def cronList = quartzScheduler.getTriggersOfJob(jobKey).collect { it.getCronExpression() }
                if(cronList.contains(cronExpression)) {
                    def newschedule = delegate.addScheduleChainJob(newCronExpression,name)
                    if("date" in newschedule) {
                        def unscheduled = quartzScheduler.unscheduleJob(quartzScheduler.getTriggersOfJob(jobKey).find { t -> t.getCronExpression() == cronExpression }.getKey())
                        if(unscheduled) {
                            def comment = "Updating Scheduled Job ${jobKey.name}"
                            delegate.withJGit { rf ->
                                def jobDetail = quartzScheduler.getJobDetail(jobKey) 
                                def dataMap = jobDetail.getJobDataMap()
                                def relativePath = "jobs/${jobKey.name}.json"
                                def f = new File(rf,relativePath)
                                pull().call()
                                f.text = {js->
                                    js.setPrettyPrint(true)
                                    return js                            
                                }.call([
                                    group: jobKey.group,
                                    name: jobKey.name,
                                    triggers: quartzScheduler.getTriggersOfJob(jobKey).collect { it.getCronExpression() },
                                    chain: dataMap.getString("chain"),
                                    input: dataMap.get("input"),
                                    emailLog: dataMap.get("emailLog")
                                ] as JSON)
                                add().addFilepattern("${relativePath}").call()
                                if(!status().call().isClean()) {
                                    commit().setMessage(comment).call()
                                }
                                push().call()
                                pull().call()
                            }
                            return [ status: unscheduled ]                            
                        }
                    } else {
                        return newschedule
                    }
                } else {
                    return [ error: "Did not match an existing trigger!" ]
                }
            } else {
                return [ error: "Job not found" ]
            }
        }
        /**
         * Adds another schedule trigger to an existing Quartz RuleChains job
         * 
         * @param      cronExpression      A string containing a Quartz CRON style expression
         * @param      name                The name of the job
         */
        JobService.metaClass.addScheduleChainJob { String cronExpression, String name ->
            def(chainName,suffix) = name.tokenize(':')
            if((quartzScheduler.getJobGroupNames().findAll { g -> return quartzScheduler.getJobKeys(groupEquals(g)).collect { it.name }.contains(name) }.size() > 0)) {
                def jobKey = quartzScheduler.getJobKeys(
                    groupEquals(quartzScheduler.getJobGroupNames().find { g -> return quartzScheduler.getJobKeys(groupEquals(g)).collect { it.name }.contains(name) })
                ).find { jk -> return (jk.name == name) }
                try {
                    def trigger = newTrigger()
                    .withSchedule(cronSchedule(cronExpression))
                    .forJob(jobKey)
                    .build()
                    return [
                        date: quartzScheduler.scheduleJob(trigger)                                
                    ]
                } catch(ex) {
                    return [
                        error: ex.getLocalizedMessage()
                    ]
                }
            } else {
                return [ error: "Job not found" ]
            }
        }
        /**
         * Merges two Quartz RuleChains job schedules on a common RuleChain.
         * 
         * @param      mergeName           The name of the job to be removed and give up it's triggers to the other job
         * @param      name                The name of the job to recieve all the merged triggers
         * @return                         An object containing all the merged triggers
         */
        JobService.metaClass.mergeScheduleChainJob { String mergeName, String name ->
            if((quartzScheduler.getJobGroupNames().findAll { g -> return quartzScheduler.getJobKeys(groupEquals(g)).collect { it.name }.contains(name) }.size() > 0)) {
                def jobKey = quartzScheduler.getJobKeys(
                    groupEquals(quartzScheduler.getJobGroupNames().find { g -> return quartzScheduler.getJobKeys(groupEquals(g)).collect { it.name }.contains(name) })
                ).find { jk -> return (jk.name == name) }
                if((quartzScheduler.getJobGroupNames().findAll { g -> return quartzScheduler.getJobKeys(groupEquals(g)).collect { it.name }.contains(mergeName) }.size() > 0)) {
                    def mergeJobKey = quartzScheduler.getJobKeys(
                        groupEquals(quartzScheduler.getJobGroupNames().find { g -> return quartzScheduler.getJobKeys(groupEquals(g)).collect { it.name }.contains(mergeName) })
                    ).find { jk -> return (jk.name == mergeName) }
                    // Get a list of the target CRONs
                    def cronList = quartzScheduler.getTriggersOfJob(jobKey).collect { it.getCronExpression() }
                    quartzScheduler.getTriggersOfJob(mergeJobKey).collect { it.getCronExpression() }.each { ce ->
                        if(!cronList.contains(ce)) {
                            delegate.addScheduleChainJob(ce,name)
                        }
                    }
                    def deleted = removeChainJob(mergeName)                    
                    if(("status" in deleted)?deleted.status:false) {
                        // Git removes this one.
                        def comment = "Removing Scheduled Job ${mergeJobKey.name}"
                        delegate.withJGit { rf ->
                            def relativePath = "jobs/${mergeJobKey.name}.json"
                            def f = new File(rf,relativePath)
                            if(f.exists()) {
                                f.delete()
                                rm().addFilepattern("${relativePath}").call()
                                if(!status().call().isClean()) {
                                    commit().setMessage(comment).call()
                                }
                                push().call()
                                pull().call()
                            }
                        }                        
                    }
                    return deleted
                } else {
                    return [ error: "Merge job not found" ]
                }
            } else {
                return [ error: "Target job not found" ]
            }            
        }        
        /** 
         * Lists out all the currently executing Quartz RuleChains schedules.
         * 
         * @return       A list of all the currently executing job objects properties
         */
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
                        input: jec.getJobDetail().getJobDataMap().get("input"),
                        emailLog: jec.getJobDetail().getJobDataMap().get("emailLog")
                    ]
                }
            ]
        }
        /**
         * A generic method to expose the quartzScheduler using a closure
         * 
         * @param      closure      A closure with the quartzScheduler available to it for custom processing
         */
        RuleChainsSchedulerListener.metaClass.accessScheduler {Closure closure->
            closure.delegate = delegate
            closure.call(quartzScheduler)
        }
    }
}

