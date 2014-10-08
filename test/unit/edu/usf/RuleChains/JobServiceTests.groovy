package edu.usf.RuleChains



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
@Mock([JobHistory,JobLog])
class JobServiceTests {
    /**
     * Tests retrieving a paginated list of job logs for a specified job history
     * 
     */
    void testGetJobLogs() {
        def jobService = new JobService()
        def h = new JobHistory(name: "testHistory")
        h.save()
        use (TimeCategory) {
            [
                [
                    line: "Line 1",
                    logTime: new Date() + 1.seconds
                ] as JobLog,
                [
                    line: "Line 2",
                    logTime: new Date() + 2.seconds
                ] as JobLog,
                [
                    line: "Line 3",
                    logTime: new Date() + 3.seconds
                ] as JobLog,
                [
                    line: "Line 4",
                    logTime: new Date() + 4.seconds
                ] as JobLog                        
            ].each { jl ->
                h.addToJobLogs(jl)
                h.save()
            }
        }
        def result = jobService.getJobLogs("testHistory",3,0)
        assert result.jobLogs.size() == 3
    }
    /**
     * Tests retrieving a paginated list of calculated job timings for a specified job history
     * 
     */
    void testGetJobRuleTimings() {
        def jobService = new JobService()
        def h = new JobHistory(name: "testHistory")
        h.save()
        use (TimeCategory) {
            [
                [
                    line: "[START_EXECUTE] Chain testChain",
                    logTime: new Date() + 1.seconds
                ] as JobLog,
                [
                    line: "[SQLQuery] Detected a SQLQuery for rule1",
                    logTime: new Date() + 1.seconds
                ] as JobLog,
                [
                    line: "[SQLQuery] Detected a SQLQuery for rule2",
                    logTime: new Date() + 2.seconds
                ] as JobLog,
                [
                    line: "[SQLQuery] Detected a SQLQuery for rule3",
                    logTime: new Date() + 3.seconds
                ] as JobLog,
                [
                    line: "[END_EXECUTE] Chain testChain",
                    logTime: new Date() + 3.seconds
                ] as JobLog,
                [
                    line: "[Finished] testHistory",
                    logTime: new Date() + 4.seconds
                ] as JobLog                        
            ].each { jl ->
                h.addToJobLogs(jl)
                h.save()
            }
        }
        def result = jobService.getJobRuleTimings("testHistory",3,0)
        assert result.jobLogs.size() == 0
    }
    /**
     * Tests returning a list of available Job Histories
     * 
     */
    void testGetJobHistories() {
        def jobService = new JobService()
        def h = new JobHistory(name: "testHistory")
        h.save()
        def result = jobService.getJobHistories()
        assert result.jobHistories.size() == 1
    }
    /**
     * Tests removing a specified Job History by name
     * 
     */
    void testDeleteJobHistory() {
        def jobService = new JobService()
        def h = new JobHistory(name: "testHistory")
        h.save()
        def result = jobService.deleteJobHistory("testHistory")
        assert result.success == "Job History deleted"
    }
}
