<!DOCTYPE html>
<html>
    <head>
        <meta name="layout" content="main"/>
        <title>RuleChains - Aggregate Processor</title>
    </head>
    <body>
        <div id="tabs">
            <ul>
                <li><a href="#monitor">Monitor</a></li>
                <li><a href="#schedule">Schedule</a></li>
                <li><a href="#chains">Chains</a></li>
                <li><a href="#ruleSets">RuleSets</a></li>
                <li><a href="#handlers">Service Handlers</a></li>
                <li><a href="#backup">Backup</a></li>
            </ul>  
            <div id="monitor" role="main">
                <div id="monitorTabs">
                    <ul>
                        <li><a href="#runningJobs">Running Jobs</a></li>
                        <li><a href="#jobHistories">Job Histories</a></li>
                        <li><a href="#jobChainRuleTimings">Job ChainRule Timings</a></li>
                    </ul>
                    <div id="runningJobs" role="main">
                        <div id="runningJobsButtonSet" style="display: inline-block;font-size:70%;">
                            <button id="refreshRunningJobsButton">Refresh Running Jobs</button> 
                        </div>
                        <table id="runningJobsTable" style="font-size:70%;">
                            <thead>
                                <tr>
                                    <th>&nbsp;</th>
                                    <th>TargetChain</th>
                                    <th>JobName</th>
                                    <th>JobGroup</th>
                                    <th>JobDescription</th>
                                    <th>CRON</th>
                                    <th>FireTime</th>
                                    <th>ScheduledFireTime</th>
                                    <th>EmailLog</th>
                                </tr>
                            </thead>
                        </table>
                    </div>
                    <div id="jobHistories" role="main">
                        <job:jobHistorySelect />
                        <div id="jobHistoryButtonSet" style="display: inline-block;font-size:70%;">
                            <button id="refreshJobHistoryButton">Refresh Job History</button> 
                            <!-- <button id="addChain">Add new Chain</button> 
                            <button id="modifyChain">Modify Chain</button> -->
                            <button id="removeJobHistoryButton">Delete Selected Job History</button> 
                        </div>                        
                        <table id="jobHistoriesTable" style="font-size:70%;">
                            <thead>
                                <tr>
                                </tr>
                                <tr>
                                    <th>Date</th>
                                    <th>Log&nbsp;Entry</th>
                                </tr>
                            </thead>
                        </table>                    
                    </div>
                    <div id="jobChainRuleTimings" role="main">
                        <job:jobHistorySelect />
                        <div id="jobChainRuleTimingButtonSet" style="display: inline-block;font-size:70%;">
                            <button id="refreshJobChainRuleTimingButton">Refresh Job History</button> 
                        </div>                        
                        <table id="jobChainRuleTimingsTable" style="font-size:70%;">
                            <thead>
                                <tr>
                                </tr>
                                <tr>
                                    <th>ExecutionTime</th>
                                    <th>RuleName</th>
                                    <th>Duration</th>
                                </tr>
                            </thead>
                        </table>                    
                    </div>
                </div>
            </div>
            <div id="schedule" role="main">              
                <div id="jobsButtonSet" style="display: inline-block;font-size:70%;">
                    <button id="refreshScheduledJobs">Refresh Scheduled Jobs</button> 
                    <button id="addScheduledJob">Schedule new Job</button> 
                    <button id="scheduleMergeButton">Merge Common Chain Schedules</button> 
                    <button id="scheduleDeleteButton">Delete Scheduled Job</button> 
                </div>
                <table id="scheduleTable" style="font-size:70%;">
                    <thead>
                      <!-- <tr>
                        <th colspan="3">
                          <div id="chainButtonSet">
                            <button id="addLink">Add new Link</button> 
                            <button id="moveLink">Move Link</button> 
                            <button id="deleteLink">Delete Link</button> 
                          </div>
                        </th>
                      </tr> -->
                        <tr>
                          <!-- <th>&nbsp;</th> -->
                            <th>Job</th>
                            <th>Chain</th>
                            <th>Triggers</th>
                            <th>Email&nbsp;Log</th>
                        </tr>
                    </thead>
                </table>
            </div>
            <div id="chains" role="main">
                <chain:chainSelect />
                <div id="chainButtonSet" style="display: inline-block;font-size:70%;">
                    <button id="refreshChain">Refresh Chain</button> 
                    <button id="addChain">Add new Chain</button> 
                    <button id="modifyChain">Modify Chain</button> 
                    <button id="deleteChain">Delete Chain</button> 
                </div>
                <table id="chainTable" style="font-size:70%;">
                    <thead>
                        <tr>
                            <th colspan="7">
                                <div id="chainButtonSet">
                                    <button id="addLink">Add new Link</button> 
                                    <button id="moveLink">Move Link</button> 
                                    <button id="deleteLink">Delete Link</button> 
                                </div>
                            </th>
                        </tr>
                        <tr>
                            <th>&nbsp;</th>
                            <th>Description</th>
                            <th>Id</th>
                            <th>Type</th>
                            <th>SourceName</th>
                            <th>Execute Action</th>
                            <th>Result Action</th>
                            <th>Link Action</th>                    
                        </tr>
                    </thead>
                </table>
            </div>
            <div id="ruleSets" role="main">
                <ruleSet:ruleSetSelect />
                <div id="ruleSetButtonSet" style="display: inline-block;font-size:70%;">
                    <button id="refreshSet">Refresh Set</button> 
                    <button id="addSet">Add new Set</button> 
                    <button id="modifySet">Modify Set</button> 
                    <button id="deleteSet">Delete Set</button> 
                </div>
                <table id="ruleTable" style="font-size:70%;">
                    <thead>
                        <tr>
                            <th colspan="4">
                    <div id="ruleButtonSet">
                        <button id="addRule">Add new Rule</button> 
                        <button id="moveRule">Move Rule</button> 
                        <button id="deleteRule">Delete Rule</button> 
                    </div>
                    </th>
                    </tr>
                    <tr>
                        <th>&nbsp;</th>
                        <th>Id</th>
                        <th>Name</th>
                        <th>Type</th>
                    </tr>
                    </thead>
                </table>
            </div>
            <div id="handlers" role="main">
                <label for="chainServiceHandlerSet" style="padding-right:15px;">Service Handler Options</label>
                <table id="chainServiceHandlerTable" style="font-size:70%;">
                    <thead>
                        <tr>
                            <th colspan="4">
                    <div id="chainServiceHandlerSet" style="display: inline-block;font-size:70%;">
                        <button id="refreshHandlers">Refresh Handlers</button> 
                        <button id="addHandler">Add new Handler</button> 
                        <button id="deleteHandler">Delete Handler</button> 
                    </div>
                    </th>
                    </tr>
                    <tr>
                        <th>&nbsp;</th>
                        <th>Name</th>
                        <th>Chain</th>
                        <th>Method</th>
                    </tr>
                    </thead>
                </table>
            </div>
            <div id="backup" role="main">
                <div id="backupButtonSet" style="display: inline-block;font-size:70%;">
                    <a id="backupButton" href="backup/download">Make a Backup</a>
                    <button id="restoreButton">Restore a Backup</button> 
                    <input type="file" id="restore" name="restore" size="chars" style="display:none;">
                </div>              
            </div>            
        </div>
    </body>
</html>
