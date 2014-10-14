package edu.usf.RuleChains



import grails.converters.JSON
import grails.test.mixin.*
import org.junit.*
import groovy.time.*

/**
 * Testing JobService handling of tracking quartz job execution,
 * chain service handler execution and quartz scheduling.
 * <p>
 * Developed originally for the University of South Florida
 * 
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(JobService)
@Mock([JobEventLog])
class JobServiceTests {
    /**
     * Tests retrieving a paginated list of job logs for a specified job history
     * 
     */
    void testGetJobLogs() {
        def jobService = new JobService()
        use (TimeCategory) {
            [                    
                [
                    status: "Detected a SQLQuery for mySQL1",
                    currentOperation: "SQLQuery",
                    scheduledChain: "testChain",
                    scheduledUniqueJobId: "1234",
                    dateCreated: new Date() + 1.seconds
                ] as JobEventLog,
                [
                    status: "Detected a SQLQuery for mySQL2",
                    currentOperation: "SQLQuery",
                    scheduledChain: "testChain",
                    scheduledUniqueJobId: "1234",
                    dateCreated: new Date() + 2.seconds
                ] as JobEventLog,
                [
                    status: "Detected a SQLQuery for mySQL3",
                    currentOperation: "SQLQuery",
                    scheduledChain: "testChain",
                    scheduledUniqueJobId: "1234",
                    dateCreated: new Date() + 3.seconds
                ] as JobEventLog,
                [
                    status: """JobInfo ${[
                        name: 'testChain:1234',
                        chain: 'testChain',
                        groupName: 'default',
                        description: '',
                        cron: '0 0 0 0 ? 2014',
                        fireTime: new Date(),
                        scheduledFireTime: new Date() + 4.seconds
                    ] as JSON}""",
                    currentOperation: "SUMMARY",
                    scheduledChain: "testChain",
                    scheduledUniqueJobId: "1234",
                    dateCreated: new Date() + 4.seconds
                ] as JobEventLog                        
            ].each { jl -> 
                jl.message = jl.status
                jl.source = "RuleChains"
                jl.save()
            }
        }
        def result = jobService.getJobLogs("testHistory:1234",3,0)
        assert result.jobLogs.size() == 3
    }
    /**
     * Tests retrieving a paginated list of calculated job timings for a specified job history
     * 
     */
    void testGetJobRuleTimings() {
        def jobService = new JobService()
        use (TimeCategory) {
            [                    
                [
                    status: "Detected a SQLQuery for mySQL1",
                    currentOperation: "SQLQuery",
                    scheduledChain: "testChain",
                    scheduledUniqueJobId: "1234",
                    dateCreated: new Date() + 1.seconds
                ] as JobEventLog,
                [
                    status: "Detected a SQLQuery for mySQL2",
                    currentOperation: "SQLQuery",
                    scheduledChain: "testChain",
                    scheduledUniqueJobId: "1234",
                    dateCreated: new Date() + 2.seconds
                ] as JobEventLog,
                [
                    status: "Detected a SQLQuery for mySQL3",
                    currentOperation: "SQLQuery",
                    scheduledChain: "testChain",
                    scheduledUniqueJobId: "1234",
                    dateCreated: new Date() + 3.seconds
                ] as JobEventLog,
                [
                    status: """JobInfo ${[
                        name: "testChain:1234",
                        chain: "testChain",
                        groupName: "default",
                        description: "",
                        cron: "0 0 0 0 ? 2014",
                        fireTime: new Date(),
                        scheduledFireTime: new Date() + 4.seconds
                    ] as JSON}""",
                    currentOperation: "SUMMARY",
                    scheduledChain: "testChain",
                    scheduledUniqueJobId: "1234",
                    dateCreated: new Date()
                ] as JobEventLog                        
            ].each { jl -> 
                jl.message = jl.status
                jl.source = "RuleChains"
                jl.save()
            }
        }
        def result = jobService.getJobRuleTimings("testChain:1234",3,0)
        assert result.jobLogs.size() == 3
    }
    /**
     * Tests returning a list of available Job Histories
     * 
     */
    void testGetJobHistories() {
        def jobService = new JobService()
        use (TimeCategory) {
            [                    
                [
                    status: "Detected a SQLQuery for mySQL1",
                    currentOperation: "SQLQuery",
                    scheduledChain: "testChain",
                    scheduledUniqueJobId: "1234",
                    dateCreated: new Date() + 1.seconds
                ] as JobEventLog,
                [
                    status: "Detected a SQLQuery for mySQL2",
                    currentOperation: "SQLQuery",
                    scheduledChain: "testChain",
                    scheduledUniqueJobId: "1234",
                    dateCreated: new Date() + 2.seconds
                ] as JobEventLog,
                [
                    status: "Detected a SQLQuery for mySQL3",
                    currentOperation: "SQLQuery",
                    scheduledChain: "testChain",
                    scheduledUniqueJobId: "1234",
                    dateCreated: new Date() + 3.seconds
                ] as JobEventLog,
                [
                    status: """JobInfo ${[
                        name: "testChain:1234",
                        chain: "testChain",
                        groupName: "default",
                        description: "",
                        cron: "0 0 0 0 ? 2014",
                        fireTime: new Date(),
                        scheduledFireTime: new Date() + 4.seconds
                    ] as JSON}""",
                    currentOperation: "SUMMARY",
                    scheduledChain: "testChain",
                    scheduledUniqueJobId: "1234",
                    dateCreated: new Date()
                ] as JobEventLog                        
            ].each { jl -> 
                jl.message = jl.status
                jl.source = "RuleChains"
                jl.save()
            }
        }
        def result = jobService.getJobHistories()
        assert result.jobHistories.size() == 1
    }
    /**
     * Tests removing a specified Job History by name
     * 
     */
    void testDeleteJobHistory() {
        def jobService = new JobService()
        def jh = [
            status: """JobInfo ${[
                name: 'testChain:1234',
                chain: 'testChain',
                groupName: 'default',
                description: '',
                cron: '0 0 0 0 ? 2014',
                fireTime: new Date(),
                scheduledFireTime: new Date() + 4.seconds
            ] as JSON}""",
            currentOperation: "SUMMARY",
            scheduledChain: "testChain",
            scheduledUniqueJobId: "1234",
            dateCreated: new Date()
        ] as JobEventLog
        jh.save()
        def result = jobService.deleteJobHistory("testChain:1234")
        assert result.success == "Job History deleted for testChain:1234 with 1 records deleted"
    }
}
