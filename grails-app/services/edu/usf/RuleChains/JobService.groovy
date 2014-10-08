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
     * Retrieves a paginated list of job logs for a specified job history
     * 
     * @param     name     The unique name of the job history
     * @param     records  The number of records to return
     * @param     offset   The offset used to return the page of records returned
     * @return             Returns an object containing the requested job logs, available job histories and the total county of job logs for the specified job history
     */
    def getJobLogs(String name,Integer records = 20,Integer offset = 0) {
        if(!!name) {
            def (chain, suffix) = name.tokenize( ':' )
            return [
                jobLogs: JobEventLog.createCriteria().list(sort: 'id', order:'desc', max: records, offset: offset) {
                    eq('scheduledChain',chain)
                    eq('scheduledUniqueJobId',suffix)
                    if(!(GrailsUtil.environment in ['test'])) {
                        resultTransformer(CriteriaSpecification.ALIAS_TO_ENTITY_MAP)
                        projections {
                            property('dateCreated', 'logTime')
                            property('status', 'status')
                            property('currentOperation','currentOperation')                                                        
                            property('id','id')
                        }
                    }
                }.collect { jel ->
                    if(GrailsUtil.environment in ['test']) {
                        jel.logTime = jel.dateCreated
                    }
                    jel.line = "[${jel.currentOperation}] ${jel.status}"
                    return (GrailsUtil.environment in ['test'])?jel.properties['logTime','line','id']:jel.subMap(['logTime','line','id'])                            
                },
                jobHistories: getJobHistories().jobHistories,
                total: JobEventLog.countByScheduledChainAndScheduledUniqueJobId(chain,suffix)
            ]
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
            def (chain, suffix) = name.tokenize( ':' )
            def ruleFilter = ['Groovy','Python','Ruby','SQLQuery','StoredProcedureQuery','DefinedService','Snippet']
            def endTime = JobEventLog.createCriteria().get {
                eq('scheduledChain',chain)
                eq('scheduledUniqueJobId',suffix)
                projections {
                    max("dateCreated")
                }
            }
            return [
                jobLogs: JobEventLog.createCriteria().list(sort: 'id', order:'asc', max: records, offset: offset) {
                    eq('scheduledChain',chain)
                    eq('scheduledUniqueJobId',suffix)
                    inList('currentOperation',ruleFilter)
                    like('status','Detected a % for%')
                    if(!(GrailsUtil.environment in ['test'])) {
                        resultTransformer(CriteriaSpecification.ALIAS_TO_ENTITY_MAP)
                        projections {
                            property('dateCreated', 'logTime')
                            property('status', 'status')
                            property('currentOperation','currentOperation')                            
                            property('id','id')
                        }
                    }
                }.reverse().collect { jl ->
                    if(GrailsUtil.environment in ['test']) {
                        jl.logTime = jl.dateCreated
                    }
                    jl.duration = TimeCategory.minus(endTime, jl.logTime).toString()
                    endTime = jl.logTime
                    jl.ruleName = jl.status.tokenize().last()
                    jl.line = "[${jl.currentOperation}] ${jl.status}"
                    return (GrailsUtil.environment in ['test'])?jl.properties['logTime','duration','ruleName','line','id']:jl.subMap(['logTime','duration','ruleName','line','id'])                            
                }.reverse(),
                jobHistories: getJobHistories().jobHistories,
                total: JobEventLog.findAllByScheduledChainAndScheduledUniqueJobIdAndStatusLikeAndCurrentOperationInList(chain,suffix,'Detected a % for%',ruleFilter).size()
            ]
        }
    }
    /**
     * Returns a list of available Job Histories
     * 
     * @return     A list of job histories
     */
    def getJobHistories() {
        return [
            jobHistories: (GrailsUtil.environment in ['test'])?JobEventLog.list():JobEventLog.createCriteria().listDistinct() { 
                eq('currentOperation','SUMMARY')
            }.collect { jel ->
                def jh = jel.getJobInfo()
                jh.endTime = JobEventLog.createCriteria().get {
                    eq('scheduledChain',jel.scheduledChain)
                    eq('scheduledUniqueJobId',jel.scheduledUniqueJobId)
                    projections {
                        max("dateCreated")
                    }
                }
                jh.startTime = JobEventLog.createCriteria().get {
                    eq('scheduledChain',jel.scheduledChain)
                    eq('scheduledUniqueJobId',jel.scheduledUniqueJobId)
                    projections {
                        min("dateCreated")
                    }
                }
                jh.duration = TimeCategory.minus(jh.endTime, jh.startTime).toString()
                jh.name = "${jel.scheduledChain}:${jel.scheduledUniqueJobId}"
                jh.id = jel.id
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
            def (chain, suffix) = name.tokenize( ':' )
            def recordsDeleted = JobEventLog.where { 
                (scheduledChain == chain && scheduledUniqueJobId == suffix)
            }.deleteAll()
            return [
                success: "Job History deleted for ${chain}:${suffix} with ${recordsDeleted} records deleted"
            ]
        }
        return [ error: "You must supply a name" ]
    }
}
