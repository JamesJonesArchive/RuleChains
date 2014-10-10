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
import edu.usf.cims.emailer.EmailerEngine
import java.io.ByteArrayInputStream
import java.io.InputStream
import groovy.util.ConfigObject
import grails.util.Holders
import java.nio.charset.Charset
import groovy.text.*
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
    def groovyPagesTemplateEngine
    def userInfoHandlerService
    def ruleFilter = ['Groovy','Python','Ruby','SQLQuery','StoredProcedureQuery','DefinedService','Snippet']
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
    
    def emailJobLog(String email,String name) {
        if(!!email) {
            def (chain, suffix) = name.tokenize( ':' )
            def logTemplate = '''<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
                <html>
                <head>
                <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
                <title>RuleChain Execution Report for Job ${name}</title>
                </head>

                <body style="margin:0;padding:0">

                <table align="left">

                <h2>RuleChain Execution Report for Job ${name}:</h2>
                <br>

                <table>
                    <thead>
                        <th style="border-bottom: solid 0.25em black;">Job Attribute</th>
                        <th style="border-bottom: solid 0.25em black;">Job Value</th>
                    </thead>
                    <tbody>
                        <tr><td align=center style="padding-left: 5px;padding-right: 5px;">Cron</td><td align=center style="padding-left: 5px;padding-right: 5px;">${cronDetails?.cron}</td></tr>
                        <tr><td align=center style="padding-left: 5px;padding-right: 5px;">Group</td><td align=center style="padding-left: 5px;padding-right: 5px;">${cronDetails?.groupName}</td></tr>
                        <tr><td align=center style="padding-left: 5px;padding-right: 5px;">Description</td><td align=center style="padding-left: 5px;padding-right: 5px;">${cronDetails?.description}</td></tr>
                        <tr><td align=center style="padding-left: 5px;padding-right: 5px;">FireTime</td><td align=center style="padding-left: 5px;padding-right: 5px;">${cronDetails?.fireTime}</td></tr>
                        <tr><td align=center style="padding-left: 5px;padding-right: 5px;">ScheduledFireTime</td><td align=center style="padding-left: 5px;padding-right: 5px;">${cronDetails?.scheduledFireTime}</td></tr>
                        <tr><td align=center style="padding-left: 5px;padding-right: 5px;">JobDuration</td><td align=center style="padding-left: 5px;padding-right: 5px;">${duration}</td></tr>
                    </tbody>
                </table>
                <br><br>
                <b>Your timings log report is as follows:</b>
                <br><br>

                <table>
                    <thead>
                        <th style="border-bottom: solid 0.25em black;">Log Time</th>
                        <th style="border-bottom: solid 0.25em black;">Duration</th>
                        <th style="border-bottom: solid 0.25em black;">Rule Name</th>
                        <th style="border-bottom: solid 0.25em black;">Log Line</th>
                    </thead>
                    <tbody>
                        <% timingEvents.each { timingEvent -> %>
                            <tr><td align=center style="padding-left: 5px;padding-right: 5px;"><%= timingEvent.logTime %></td>
                            <td align=center style="padding-left: 5px;padding-right: 5px;"><%= timingEvent.duration %></td>
                            <td align=center style="padding-left: 5px;padding-right: 5px;"><%= timingEvent.ruleName %></td>
                            <td align=left style="padding-left: 5px;padding-right: 5px;"><%= timingEvent.line %></td></tr>
                        <%} %>
                    </tbody>
                </table>
                <br><br>
                <b>Your verbose log report is as follows:</b>
                <br><br>

                <table>
                    <thead>
                        <th style="border-bottom: solid 0.25em black;">Log Time</th>
                        <th style="border-bottom: solid 0.25em black;">Log Line</th>
                    </thead>
                    <tbody>
                        <% logEvents.each { logEvent -> %>
                            <tr><td align=center style="padding-left: 5px;padding-right: 5px;"><%= logEvent.logTime %></td>
                            <td align=left style="padding-left: 5px;padding-right: 5px;"><%= logEvent.line %></td></tr>
                        <%} %>
                    </tbody>
                </table>
                </body>
                </html>
            '''
            def endTime
            (new EmailerEngine()).sendEmail(
                (ConfigObject) [
                    'fromAddr': Holders.config.ruleChains?.reporting?.noreplyAddress,
                    'recipient': email,
                    'recipientHdr': 'email',
                    'mail.smtp.host': Holders.config.ruleChains?.reporting?.smtp,
                    'subject': "RuleChains Report for Job: ${name}"
                ],
                (new GStringTemplateEngine()).createTemplate(logTemplate).make([
                    name: name, 
                    logEvents: JobEventLog.createCriteria().list(sort: 'id', order:'asc') {
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
                    timingEvents: JobEventLog.createCriteria().list(sort: 'id', order:'asc') {
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
                        jl.duration = TimeCategory.minus(
                            (endTime)?endTime:JobEventLog.createCriteria().get {
                                eq('scheduledChain',chain)
                                eq('scheduledUniqueJobId',suffix)
                                projections {
                                    max("dateCreated")
                                }
                            }, 
                            jl.logTime
                        ).toString()
                        endTime = jl.logTime
                        jl.ruleName = jl.status.tokenize().last()
                        jl.line = "[${jl.currentOperation}] ${jl.status}"
                        return (GrailsUtil.environment in ['test'])?jl.properties['logTime','duration','ruleName','line','id']:jl.subMap(['logTime','duration','ruleName','line','id'])                            
                    }.reverse(),
                    cronDetails: JobEventLog.findByCurrentOperationAndScheduledChainAndScheduledUniqueJobId('SUMMARY',chain,suffix)?.getJobInfo(),
                    duration: TimeCategory.minus(
                        JobEventLog.createCriteria().get {
                            eq('scheduledChain',chain)
                            eq('scheduledUniqueJobId',suffix)
                            projections {
                                max("dateCreated")
                            }
                        }, 
                        JobEventLog.createCriteria().get {
                            eq('scheduledChain',chain)
                            eq('scheduledUniqueJobId',suffix)
                            projections {
                                min("dateCreated")
                            }
                        }
                    ).toString()
                ]).toString()
            )
        } else {
            println "Email was EMPTY!!!"
        }
    }
}
