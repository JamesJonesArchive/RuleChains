package edu.usf.RuleChains



import grails.converters.JSON
import grails.test.mixin.*
import org.junit.*
import org.hibernate.criterion.CriteriaSpecification
import groovy.time.*
/**
 * Testing JobController handling of tracking quartz job execution,
 * chain service handler execution and quartz scheduling.
 * <p>
 * Developed originally for the University of South Florida
 * 
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(JobController)
@Mock([JobService,JobEventLog])
class JobControllerTests {
    /**
     * Tests a list of available quartz jobs
     * 
     */            
    void testListChainJobs() {
        controller.request.method = "GET"
        def control = mockFor(JobService)
        control.demand.listChainJobs { -> 
            return [
                jobGroups: [
                    [
                        name: 'default',
                        jobs: [
                            [
                                name: 'test1',
                                triggers: '0 0 0 0 ? 2014',
                                chain: 'testchain',
                                input: []
                            ]
                        ]
                    ]
                ]
            ]
        }
        controller.jobService = control.createMock()

        controller.request.contentType = "text/json"
        // controller.request.content = (["pattern": null] as JSON).toString().getBytes()
        def model = controller.listChainJobs()
        assert model.jobGroups[0].name == "default"
    }
    /**
     * Tests creation of a new schedule for a rule chain in quartz
     * 
     */
    void testCreateChainJob() {
        controller.params << [
            cronExpression: "0 0 0 0 ? 2015",
            name: "newChainJob",
            input: []
        ]
        controller.request.method = "PUT"
        JobService.metaClass.createChainJob = { String cronExpression,String name,def input = [[:]] -> }
        def control = mockFor(JobService)
        control.demand.createChainJob { cronExpression,name,input -> 
            return [
                date: System.currentTimeMillis()
            ]
        }
        controller.jobService = control.createMock()
        
        controller.request.contentType = "text/json"
        // controller.request.content = (["pattern": null] as JSON).toString().getBytes()
        def model = controller.createChainJob()
        assert model.date <= System.currentTimeMillis()        
    }
    /**
     * Tests removing a quartz schedule for a rule chain and deletes the job
     * 
     */
    void testRemoveChainJob() {
        controller.params.name = "testJob"
        controller.request.method = "DELETE"
        JobService.metaClass.removeChainJob = { String name -> }
        def control = mockFor(JobService)
        control.demand.removeChainJob { name ->
            return [
                status: [ 
                    [
                        jobName: name,
                        jobGroup: 'default',
                        removed: System.currentTimeMillis()
                    ] 
                ]
            ]
        }
        controller.jobService = control.createMock()
        
        controller.request.contentType = "text/json"
        // controller.request.content = (["pattern": null] as JSON).toString().getBytes()
        def model = controller.removeChainJob()
        assert model.status[0].jobName == "testJob"      
    }
    /**
     * Tests removing a quartz schedule for a rule chain
     * 
     */
    void testUnscheduleChainJob() {
        controller.params << [
            name: "testJob",
            cronExpression: "0 0 0 0 ? 2014"
        ]
        controller.request.method = "DELETE"
        JobService.metaClass.unscheduleChainJob = { String cronExpression, String name -> }
        def control = mockFor(JobService)
        control.demand.unscheduleChainJob { cronExpression,name ->
            def jobsMock = [
                [
                    jobName: "testJob",
                    jobGroup: "default",
                    triggers: ["0 0 0 0 ? 2014","0 0 0 0 ? 2015"]
                ]
            ]
            return [ 
                status: [ jobsMock.find { it.jobName == "testJob" }.inject([:]) {m,k,v ->
                    switch(k) {
                        case 'triggers':
                            assert v.findAll { it != cronExpression }.size() < 2
                            m["removed"] = System.currentTimeMillis()
                            break
                        default:
                            m[k] = v
                            break
                    }
                    return m
                } ]
            ]
        }
        controller.jobService = control.createMock()
        
        controller.request.contentType = "text/json"
        // controller.request.content = (["pattern": null] as JSON).toString().getBytes()
        def model = controller.unscheduleChainJob()
        assert model.status[0].jobName == "testJob"      
    }
    /**
     * Tests updating a quartz schedule for a rule chain
     * 
     */
    void testRescheduleChainJob() {
        controller.params << [
            cronExpression: "0 0 0 0 ? 2014",
            cron: "0 0 0 0 ? 2015",
            name: "testJob"
        ]
        controller.request.method = "POST"
        JobService.metaClass.rescheduleChainJob = { String cronExpression, String cron, String name -> }
        def control = mockFor(JobService)
        control.demand.rescheduleChainJob { cronExpression,cron,name ->
            def jobsMock = [
                [
                    jobName: "testJob",
                    jobGroup: "default",
                    triggers: ["0 0 0 0 ? 2014"]
                ]
            ]
            return [ 
                status: [ jobsMock.find { it.jobName == name }.inject([:]) {m,k,v ->
                    switch(k) {
                        case 'triggers':
                            assert v.findAll { it == cronExpression }.size() < 2
                            m["scheduled"] = new Date()
                            break
                        default:
                            m[k] = v
                            break
                    }
                    return m
                } ]
            ]
        }
        controller.jobService = control.createMock()
        
        controller.request.contentType = "text/json"
        // controller.request.content = (["pattern": null] as JSON).toString().getBytes()
        def model = controller.rescheduleChainJob()
        assert model.status[0].jobName == "testJob"              
        assert model.status[0].scheduled <= new Date()              
    }
    /**
     * Tests updating a quartz schedule with a different associated rule chain
     * 
     */
    void testUpdateChainJob() {
        controller.params << [
            name: "testJob", 
            newName: "renamedTestJob"
        ]
        controller.request.method = "POST"
        JobService.metaClass.updateChainJob = { String name, String newName -> }
        def control = mockFor(JobService)
        control.demand.updateChainJob { name,newName ->
            def jobsMock = [
                [
                    jobName: "testJob",
                    jobGroup: "default",
                    triggers: ["0 0 0 0 ? 2014"]
                ]
            ]
            return [
                updated: (jobsMock.find { it.jobName == name }.jobName != newName)
            ]
        }
        controller.jobService = control.createMock()
        
        controller.request.contentType = "text/json"
        // controller.request.content = (["pattern": null] as JSON).toString().getBytes()
        def model = controller.updateChainJob()
        assert model.updated == true                      
    }
    /**
     * Tests adding an additional quartz schedule to an existing rule chain job
     * 
     */
    void testAddscheduleChainJob() {
        controller.params << [
            cronExpression: "0 0 0 0 ? 2015",
            name: "testJob"
        ]
        controller.request.method = "PUT"
        JobService.metaClass.addscheduleChainJob = { String cronExpression, String name -> }
        def control = mockFor(JobService)
        control.demand.addscheduleChainJob { cronExpression,name -> 
            def jobsMock = [
                [
                    jobName: "testJob",
                    jobGroup: "default",
                    triggers: ["0 0 0 0 ? 2014"]
                ]
            ]        
            return [
                date: { t ->
                    t << cronExpression
                    assert t.size() > 1
                    return new Date()
                }.call(jobsMock.find { it.jobName == name }.triggers)
            ]
        }
        controller.jobService = control.createMock()
        
        controller.request.contentType = "text/json"
        // controller.request.content = (["pattern": null] as JSON).toString().getBytes()
        def model = controller.addscheduleChainJob()
        assert model.date <= new Date()                              
    }
    /**
     * Tests combining quartz schedules of using a common rule chain
     * 
     */
    void testMergescheduleChainJob() {
        controller.params << [
            mergeName: "testJob",
            name: "testJobDup"
        ]
        controller.request.method = "POST"
        JobService.metaClass.mergescheduleChainJob = { String mergeName, String name -> }
        def control = mockFor(JobService)
        control.demand.mergescheduleChainJob { mergeName,name -> 
            def jobsMock = [
                [
                    jobName: "testJob",
                    jobGroup: "default",
                    triggers: ["0 0 0 0 ? 2014"]
                ],
                [
                    jobName: "testJobDup",
                    jobGroup: "default",
                    triggers: ["0 0 0 0 ? 2015"]
                ]
            ]        
            return [
                mergedTriggers: jobsMock.findAll { it.jobName in [mergeName,name] }.collect { return it.triggers }.flatten(),
                delete: new Date()
            ]
        }
        controller.jobService = control.createMock()
        
        controller.request.contentType = "text/json"
        // controller.request.content = (["pattern": null] as JSON).toString().getBytes()
        def model = controller.mergescheduleChainJob()
        assert model.delete <= new Date()   
        assert model.mergedTriggers == ["0 0 0 0 ? 2014","0 0 0 0 ? 2015"]
    }
    /**
     * Tests listing all quartz schedules currently executing on rule chains
     * 
     */
    void testListCurrentlyExecutingJobs() {
        controller.request.method = "GET"
        JobService.metaClass.listCurrentlyExecutingJobs = { -> }
        def control = mockFor(JobService)
        control.demand.listCurrentlyExecutingJobs { -> 
            return [
                executingJobs: [
                    [
                        chain: "testChain",
                        name: "testJob",
                        description: "a test job description",
                        group: "default",
                        cron: "0 0 0 0 ? 2014",
                        fireTime: new Date(),
                        scheduledFireTime: new Date(),
                        input: []
                    ]
                ]
            ]
        }
        controller.jobService = control.createMock()
        
        controller.request.contentType = "text/json"
        // controller.request.content = (["pattern": null] as JSON).toString().getBytes()
        def model = controller.listCurrentlyExecutingJobs()
        assert model.executingJobs[0].chain == "testChain"           
    }
    /**
     * Tests retrieving a paginated list of job logs for a specified job history
     * 
     */
    void testGetJobLogs() {
        controller.params << [
            name: "testChain:1234",
            records: 3,
            offset: 0
        ]
        controller.request.method = "GET"
        def control = mockFor(JobService)
        control.demand.getJobLogs { String name,Integer records,Integer offset-> 
            [
                [
                    status: "Line 1",
                    currentOperation: "INFO",
                    scheduledChain: "testChain",
                    scheduledUniqueJobId: "1234",
                    dateCreated: new Date()
                ] as JobEventLog,
                [
                    status: "Line 2",
                    currentOperation: "INFO",
                    scheduledChain: "testChain",
                    scheduledUniqueJobId: "1234",
                    dateCreated: new Date()
                ] as JobEventLog,
                [
                    status: "Line 3",
                    currentOperation: "INFO",
                    scheduledChain: "testChain",
                    scheduledUniqueJobId: "1234",
                    dateCreated: new Date()
                ] as JobEventLog,
                [
                    status: """JobInfo ${[
                        name: "testChain:1234",
                        chain: "testChain",
                        groupName: "default",
                        description: "",
                        cron: "0 0 0 0 ? 2014",
                        fireTime: new Date(),
                        scheduledFireTime: new Date()
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
            def (chain, suffix) = name.trim().tokenize( ':' )
            return [
                jobLogs: JobEventLog.createCriteria().list(sort: 'id', order:'desc', max: records, offset: offset) {
                    eq('scheduledChain',chain)
                    eq('scheduledUniqueJobId',suffix)
                },
                jobHistories: JobEventLog.findAllByCurrentOperation('SUMMARY'),
                total: JobEventLog.countByScheduledChainAndScheduledUniqueJobId(chain,suffix)
            ]            
        }
        controller.jobService = control.createMock()
        
        controller.request.contentType = "text/json"
        // controller.request.content = (["pattern": null] as JSON).toString().getBytes()
        def model = controller.getJobLogs()
        assert model.jobLogs.size() == 3          
    }
    /**
     * Tests retrieving a paginated list of calculated job timings for a specified job history
     * 
     */
    void testGetJobRuleTimings() {
        controller.params << [
            name: "testChain:1234",
            records: 3,
            offset: 0
        ]
        controller.request.method = "GET"
        def control = mockFor(JobService)
        control.demand.getJobRuleTimings { String name,Integer records,Integer offset-> 
            def now = new Date()
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
            def (chain, suffix) = name.trim().tokenize( ':' )
            def ruleFilter = ['Groovy','Python','Ruby','SQLQuery','StoredProcedureQuery','DefinedService','Snippet']
            def endTime = JobEventLog.createCriteria().get {
                eq('scheduledChain',chain)
                eq('scheduledUniqueJobId',suffix)
                projections {
                    max('dateCreated')
                }
            }
            return [
                jobLogs: { jls ->
                    jls.reverse().collect { jl ->
                        def jlobj = jl.properties as Map
                        jlobj.duration = TimeCategory.minus(endTime, jl.dateCreated).toString()
                        endTime = jl.dateCreated
                        // jl.ruleName = jl.line.tokenize().last()
                        return jlobj                            
                    }
                }.call(JobEventLog.createCriteria().list(sort: 'id', order:'desc', max: records, offset: offset) {
                    eq('scheduledChain',chain)
                    eq('scheduledUniqueJobId',suffix)
                    inList('currentOperation',ruleFilter)
                    like('status','Detected a % for%')
                }),
                jobHistories: JobEventLog.findAllByCurrentOperation('SUMMARY'),
                total: JobEventLog.findAllByScheduledChainAndScheduledUniqueJobIdAndStatusLikeAndCurrentOperationInList(chain,suffix,'Detected a % for%',ruleFilter).size()
            ]            
        }
        controller.jobService = control.createMock()
        
        controller.request.contentType = "text/json"
        // controller.request.content = (["pattern": null] as JSON).toString().getBytes()
        def model = controller.getJobRuleTimings()
        assert model.jobLogs.size() == 3    
        assert model.jobLogs.last().dateCreated > model.jobLogs.first().dateCreated
    }
    /**
     * Tests returning a list of available Job Histories
     * 
     */
    void testGetJobHistories() {
        controller.request.method = "GET"
        def control = mockFor(JobService)
        control.demand.getJobHistories { -> 
            def now = new Date()
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
                        dateCreated: new Date()
                    ] as JobEventLog                        
                ].each { jl -> 
                    jl.message = jl.status
                    jl.source = "RuleChains"
                    jl.save()
                }
            }
            return [
                jobHistories: JobEventLog.findAllByCurrentOperation('SUMMARY')
            ]            
        }
        controller.jobService = control.createMock()
        
        controller.request.contentType = "text/json"
        // controller.request.content = (["pattern": null] as JSON).toString().getBytes()
        def model = controller.getJobHistories()
        assert model.jobHistories.size() == 1    
    }
    /**
     * Tests removing a specified Job History by name
     * 
     */
    void testDeleteJobHistory() {
        controller.params.name = "testChain:1234"
    
        controller.request.method = "DELETE"
        def control = mockFor(JobService)
        control.demand.deleteJobHistory { name -> 
            def (chain, suffix) = name.tokenize( ':' )
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
            def recordsDeleted = JobEventLog.where { 
                (scheduledChain == chain && scheduledUniqueJobId == suffix)
            }.deleteAll()
            return [ success : "Job History deleted" ]
        }
        controller.jobService = control.createMock()
        
        controller.request.contentType = "text/json"
        // controller.request.content = (["pattern": null] as JSON).toString().getBytes()
        def model = controller.deleteJobHistory()
        assert model.success == "Job History deleted"        
    }
}
