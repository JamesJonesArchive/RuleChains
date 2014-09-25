package edu.usf.RuleChains

import org.hibernate.criterion.CriteriaSpecification
import groovy.time.*
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.impl.matchers.GroupMatcher.*
import grails.plugin.quartz2.InvokeMethodJob    
import grails.plugin.quartz2.ClosureJob    
import grails.util.GrailsUtil
/**
 * JobService provides tracking quartz job execution as well as
 * chain service handler execution. This service is also metaprogrammed
 * to handle quartz scheduling
 * <p>
 * Developed originally for the University of South Florida
 * 
 * @author <a href='mailto:james@mail.usf.edu'>James Jones</a> 
 */ 
class JobService {
    static transactional = true
    def grailsApplication
    def userInfoHandlerService
    /**
     * Adds a new Job History
     * 
     * @param    name    The unique name of the job history
     * @return           An object containing the job history
     */
    def addJobHistory(String name) {
        if(!!name) {
            def jobHistory = [ name: name.trim() ] as JobHistory
            if(!jobHistory.save(failOnError:false, flush: true, insert: true, validate: true)) {
                return [ error : "'${jobHistory.errors.fieldError.field}' value '${jobHistory.errors.fieldError.rejectedValue}' rejected" ]
            } else {
                return [ jobHistory: jobHistory ]
            }
        }
        return [ error: "You must supply a name" ]
    }
    /**
     * Finds a Job History by name
     * 
     * @param     name    The unique name of the job history
     * @return            An object containing the job history
     */
    def findJobHistory(String name) {
        if(!!name) {
            def jobHistory = JobHistory.findByName(name.trim())
            if(!!jobHistory) {
                return [ jobHistory: jobHistory ]
            }
            return [ error : "Job History named ${name} not found!"]            
        }
        return [ error: "You must supply a name" ]
    }
    /**
     * Retrieves a paginated list of job logs for a specified job history
     * 
     * @param     name     The unique name of the job history
     * @param     records  The number of records to return
     * @param     offset   The offset used to return the page of records returned
     * @return             Returns an object containing the requested job logs, available job histories and the total county of job logs for the specified job history
     */
    def getJobLogs(String name,Integer records = 20,Integer offset = 0) {
        if(!!name) {
            def jobHistory = JobHistory.findByName(name.trim())
            if(!!jobHistory) {
                return [
                    jobLogs: JobLog.createCriteria().list(sort: 'id', order:'desc', max: records, offset: offset) {
                        eq('jobHistory',jobHistory)
                        if(!(GrailsUtil.environment in ['test'])) {
                            resultTransformer(CriteriaSpecification.ALIAS_TO_ENTITY_MAP)
                            projections {
                                property('logTime', 'logTime')
                                property('line', 'line')
                                property('id','id')
                            }
                        }
                    },
                    jobHistories: getJobHistories().jobHistories,
                    total: JobLog.countByJobHistory(jobHistory)
                ]
            }
            return [ error : "Job History named ${name} not found!"]  
        }
        return [ error: "You must supply a name" ]
    }
    /**
     * Retrieves a paginated list of calculated job timings for a specified job history
     * 
     * @param     records  The number of records to return
     * @param     offset   The offset used to return the page of records returned
     * @return             Returns an object containing the requested job timings, available job histories and the total county of job logs for the specified job history
     */
    def getJobRuleTimings(String name,Integer records = 20,Integer offset = 0) {
        if(!!name) {
            def jobHistory = JobHistory.findByName(name.trim())
            if(!!jobHistory) {
                def endTime
                return [
                    jobLogs: { jls ->                        
                        endTime = JobLog.createCriteria().get {
                            eq('jobHistory',jobHistory)
                            if(!!jls) {
                                gt('id',jls.last().id)
                            }
                            or {
                                like('line','[%] Detected a % for%')
                                like('line','[Finished] %')
                            }
                            projections {
                                min('logTime')
                            }
                        }
                        if(!!!endTime) {
                            endTime = JobLog.createCriteria().get {
                                eq('jobHistory',jobHistory)
                                if(!!jls) {
                                    gt('id',jls.last().id)
                                }
                                projections {
                                    min('logTime')
                                }
                            }                                                        
                        }
                        return jls.reverse().collect { jl ->
                            jl.duration = TimeCategory.minus(endTime, jl.logTime).toString()
                            endTime = jl.logTime
                            jl.ruleName = jl.line.tokenize().last()
                            return jl                            
                        }.reverse()                        
                    }.call(JobLog.createCriteria().list(sort: 'id', order:'asc', max: records, offset: offset) {
                        eq('jobHistory',jobHistory)
                        like('line','[%] Detected a % for%')
                        if(!(GrailsUtil.environment in ['test'])) {
                            resultTransformer(CriteriaSpecification.ALIAS_TO_ENTITY_MAP)
                            projections {
                                property('logTime', 'logTime')
                                property('line', 'line')
                                property('id','id')
                            }
                        }
                    }),
                    jobHistories: getJobHistories().jobHistories,
                    total: JobLog.countByJobHistoryAndLineLike(jobHistory,'[%] Detected a % for%')
                ]
            }
            return [ error : "Job History named ${name} not found!"]             
        }
        return [ error: "You must supply a name" ]
    }
    /**
     * Returns a list of available Job Histories
     * 
     * @return     A list of job histories
     */
    def getJobHistories() {
        return [ 
            jobHistories: (GrailsUtil.environment in ['test'])?JobHistory.list():JobHistory.withCriteria {
                resultTransformer(CriteriaSpecification.ALIAS_TO_ENTITY_MAP)
                projections {
                    property('id', 'id')
                    property('name', 'name')
                    property('chain', 'chain')
                    property('groupName', 'groupName')
                    property('description', 'description')
                    property('cron', 'cron')
                    property('fireTime', 'fireTime')
                    property('scheduledFireTime', 'scheduledFireTime')
                }                
            }.collect { jh ->
                def jobHistory = JobHistory.findByName(jh.name)
                jh.endTime = JobLog.createCriteria().get {
                    eq('jobHistory',jobHistory)
                    projections {
                        max("logTime")
                    }
                }
                jh.startTime = JobLog.createCriteria().get {
                    eq('jobHistory',jobHistory)
                    projections {
                        min("logTime")
                    }
                }
                jh.duration = TimeCategory.minus(jh.endTime, jh.startTime).toString()
                return jh
            }
        ]
    }
    /**
     * Removes a specified Job History by name
     * 
     * @param     name     The unique name of the job history
     * @return             An object containing the sucess or error of the deletion
     */
    def deleteJobHistory(String name) {
        if(!!name) {
            def jobHistory = JobHistory.findByName(name.trim())
            if(!!jobHistory) {
                jobHistory.delete()
                return [ success : "Job History deleted" ]
            }
            return [ error : "Job History named ${name} not found!"]
        }
        return [ error: "You must supply a name" ]
    }
}
