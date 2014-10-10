package edu.usf.RuleChains
import grails.converters.*

/**
 * JobController provides for REST services handling of tracking quartz job execution,
 * chain service handler execution and quartz scheduling.
 * <p>
 * Developed originally for the University of South Florida
 * 
 * @author <a href='mailto:james@mail.usf.edu'>James Jones</a> 
 */ 
class JobController {
    def jobService
    /**
     * Returns a list of available quartz jobs
     * 
     * @return          An object containing the resulting list of quartz jobs
     */            
    def listChainJobs() {
        withFormat {
            html {
                return jobService.listChainJobs()
            }
            xml {
                render jobService.listChainJobs() as XML
            }
            json {
                JSON.use("deep") { render jobService.listChainJobs() as JSON }
            }
        }   
    }
    /**
     * Creates a new schedule for a rule chain in quartz
     * 
     * @return           Returns the status of quartz after job is created
     */
    def createChainJob() {
        withFormat {
            html {
                return jobService.createChainJob(params.cronExpression,params.name,params.input,params.emailLog)
            }
            xml {
                render jobService.createChainJob(params.cronExpression,params.name,params.input,params.emailLog) as XML
            }
            json {
                JSON.use("deep") { render jobService.createChainJob(params.cronExpression,params.name,params.input,params.emailLog) as JSON }
            }
        }   
    }
    /**
     * Removes a quartz schedule for a rule chain and deletes the job
     * 
     * @return           Returns the status of quartz after job is removed
     */
    def removeChainJob() {
        withFormat {
            html {
                return jobService.removeChainJob(params.name)
            }
            xml {
                render jobService.removeChainJob(params.name) as XML
            }
            json {
                JSON.use("deep") { render jobService.removeChainJob(params.name) as JSON }
            }
        }   
    }
    /**
     * Removes a quartz schedule for a rule chain
     * 
     * @return           Returns the status of quartz after job is removed
     */
    def unscheduleChainJob() {
        withFormat {
            html {
                return jobService.unscheduleChainJob(params.cronExpression,params.name)
            }
            xml {
                render jobService.unscheduleChainJob(params.cronExpression,params.name) as XML
            }
            json {
                JSON.use("deep") { render jobService.unscheduleChainJob(params.cronExpression,params.name) as JSON }
            }
        }   
    }
    /**
     * Updates a quartz schedule for a rule chain
     * 
     * @return           Returns the status of quartz after job is updated
     */
    def rescheduleChainJob() {
        withFormat {
            html {
                return jobService.rescheduleChainJob(params.cronExpression,params.cron,params.name)
            }
            xml {
                render jobService.rescheduleChainJob(params.cronExpression,params.cron,params.name) as XML
            }
            json {
                JSON.use("deep") { render jobService.rescheduleChainJob(params.cronExpression,params.cron,params.name) as JSON }
            }
        }   
    }
    /**
     * Updates a quartz schedule with a different associated rule chain
     * 
     * @return           Returns the status of quartz after job is updated
     */
    def updateChainJob() {
        withFormat {
            html {
                return jobService.updateChainJob(params.name,params.newName)
            }
            xml {
                render jobService.updateChainJob(params.name,params.newName) as XML
            }
            json {
                JSON.use("deep") { render jobService.updateChainJob(params.name,params.newName) as JSON }
            }
        }   
    }
    /**
     * Adds an additional quartz schedule to an existing rule chain job
     * 
     * @return           Returns the status of quartz after job is updated
     */
    def addscheduleChainJob() {
        withFormat {
            html {
                return jobService.addscheduleChainJob(params.cronExpression,params.name)
            }
            xml {
                render jobService.addscheduleChainJob(params.cronExpression,params.name) as XML
            }
            json {
                JSON.use("deep") { render jobService.addscheduleChainJob(params.cronExpression,params.name) as JSON }
            }
        }   
    }
    /**
     * Combines quartz schedules of using a common rule chain
     * 
     * @return           Returns the status of quartz after job is updated
     */
    def mergescheduleChainJob() {
        withFormat {
            html {
                return jobService.mergescheduleChainJob(params.mergeName,params.name)
            }
            xml {
                render jobService.mergescheduleChainJob(params.mergeName,params.name) as XML
            }
            json {
                JSON.use("deep") { render jobService.mergescheduleChainJob(params.mergeName,params.name) as JSON }
            }
        }   
    }
    /**
     * Lists all quartz schedules currently executing on rule chains
     * 
     * @return           Returns a list of objects containing current execution parameters
     */
    def listCurrentlyExecutingJobs() {
        withFormat {
            html {
                return jobService.listCurrentlyExecutingJobs()
            }
            xml {
                render jobService.listCurrentlyExecutingJobs() as XML
            }
            json {
                JSON.use("deep") { render jobService.listCurrentlyExecutingJobs() as JSON }
            }
        }   
    }
    /**
     * Retrieves a paginated list of job logs for a specified job history
     * 
     * @return             Returns an object containing the requested job logs, available job histories and the total county of job logs for the specified job history
     */
    def getJobLogs() {
        withFormat {
            html {   
                return (jobService.getJobLogs(
                    params.name,
                    params.iDisplayLength?(Math.min( params.iDisplayLength ? params.iDisplayLength.toInteger() : 20,  100) ):(Math.min( params.records ? params.records.toInteger() : 20,  100) ),
                    params.iDisplayStart?(params?.iDisplayStart?.toInteger() ?: 0):(params?.offset?.toInteger() ?: 0)
                ) << [ sEcho: params.sEcho ])
            }
            xml {
                render jobService.getJobLogs(
                    params.name,
                    params.iDisplayLength?(Math.min( params.iDisplayLength ? params.iDisplayLength.toInteger() : 20,  100) ):(Math.min( params.records ? params.records.toInteger() : 20,  100) ),
                    params.iDisplayStart?(params?.iDisplayStart?.toInteger() ?: 0):(params?.offset?.toInteger() ?: 0)
                ) as XML
            }
            json {
                JSON.use("deep") { 
                    def jobLogsResponse = jobService.getJobLogs(
                        params.name,
                        params.iDisplayLength?(Math.min( params.iDisplayLength ? params.iDisplayLength.toInteger() : 20,  100) ):(Math.min( params.records ? params.records.toInteger() : 20,  100) ),
                        params.iDisplayStart?(params?.iDisplayStart?.toInteger() ?: 0):(params?.offset?.toInteger() ?: 0)
                    )
                    jobLogsResponse.sEcho = params.sEcho
                    render jobLogsResponse as JSON
                }
            }
        }           
    }
    /**
     * Retrieves a paginated list of calculated job timings for a specified job history
     * 
     * @return             Returns an object containing the requested job timings, available job histories and the total county of job logs for the specified job history
     */
    def getJobRuleTimings() {
        withFormat {
            html {   
                return (jobService.getJobRuleTimings(
                    params.name,
                    params.iDisplayLength?(Math.min( params.iDisplayLength ? params.iDisplayLength.toInteger() : 20,  100) ):(Math.min( params.records ? params.records.toInteger() : 20,  100) ),
                    params.iDisplayStart?(params?.iDisplayStart?.toInteger() ?: 0):(params?.offset?.toInteger() ?: 0)
                ) << [ sEcho: params.sEcho ])
            }
            xml {
                render jobService.getJobRuleTimings(
                    params.name,
                    params.iDisplayLength?(Math.min( params.iDisplayLength ? params.iDisplayLength.toInteger() : 20,  100) ):(Math.min( params.records ? params.records.toInteger() : 20,  100) ),
                    params.iDisplayStart?(params?.iDisplayStart?.toInteger() ?: 0):(params?.offset?.toInteger() ?: 0)
                ) as XML
            }
            json {
                JSON.use("deep") { 
                    def jobRuleTimingsResponse = jobService.getJobRuleTimings(
                        params.name,
                        params.iDisplayLength?(Math.min( params.iDisplayLength ? params.iDisplayLength.toInteger() : 20,  100) ):(Math.min( params.records ? params.records.toInteger() : 20,  100) ),
                        params.iDisplayStart?(params?.iDisplayStart?.toInteger() ?: 0):(params?.offset?.toInteger() ?: 0)
                    )
                    jobRuleTimingsResponse.sEcho = params.sEcho
                    render jobRuleTimingsResponse as JSON
                }
            }
        }           
    }
    /**
     * Returns a list of available Job Histories
     * 
     * @return     A list of job histories
     */
    def getJobHistories() {
        withFormat {
            html {
                return jobService.getJobHistories()
            }
            xml {
                render jobService.getJobHistories() as XML
            }
            json {
                JSON.use("deep") { render jobService.getJobHistories() as JSON }
            }
        }   
    }
    /**
     * Removes a specified Job History by name
     * 
     * @return             An object containing the sucess or error of the deletion
     */
    def deleteJobHistory() {
        withFormat {
            html {
                return jobService.deleteJobHistory(params.name)
            }
            xml {
                render jobService.deleteJobHistory(params.name) as XML
            }
            json {
                JSON.use("deep") { render jobService.deleteJobHistory(params.name) as JSON }
            }
        }
    }
}
