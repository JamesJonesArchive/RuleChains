
import grails.util.GrailsNameUtils
import grails.util.GrailsUtil
import edu.usf.RuleChains.Chain
import edu.usf.RuleChains.JobService
import edu.usf.RuleChains.JobController
import edu.usf.RuleChains.LinkService
import edu.usf.RuleChains.RuleChainsJobListener
import edu.usf.RuleChains.RuleChainsSchedulerListener
import edu.usf.RuleChains.SQLQuery
import edu.usf.RuleChains.RuleSet
import edu.usf.RuleChains.Link
import edu.usf.RuleChains.ConnectionMeta
import edu.usf.RuleChains.JobMeta
import edu.usf.RuleChains.GitMeta
import edu.usf.RuleChains.Groovy
import edu.usf.RuleChains.JobEventLogAppender
import org.quartz.JobListener
import org.quartz.listeners.SchedulerListenerSupport
import grails.converters.*

class BootStrap {
    def grailsApplication
    def quartzScheduler
    def jobService
    def springSecurityService
    def usfCasService
    def configService
    def connectionMeta = new ConnectionMeta()
    def jobMeta = new JobMeta()
    def gitMeta = new GitMeta()
    def init = { servletContext ->
        // Allow the event log appender to start now that the database stuff is up and running
        if(!!!quartzScheduler) {
            print "Didn't get set!"
        } else {
            // Building the Meta Programing
            connectionMeta.buildMeta(grailsApplication)
            jobMeta.buildMeta(quartzScheduler)
            gitMeta.buildMeta(grailsApplication)
            // Added Job Listener for Git Sync'ing
            quartzScheduler.getListenerManager().addJobListener(new RuleChainsJobListener() as JobListener)
            quartzScheduler.getListenerManager().addSchedulerListener(new RuleChainsSchedulerListener())
            println ""
            println "**Defined sessionfactories**"
            def reg = ~/^sessionFactory_/
            grailsApplication.mainContext.beanDefinitionNames.findAll{ it.startsWith( 'sessionFactory_' ) }.each{ sfname ->
                 println "- " + (sfname - reg)
            }
            println ""
            def jGitSettings = grailsApplication.config?.jgit
            if(!jGitSettings.isEmpty()) {
                println "Building Rules from Git Repo using:"
                println "    Git Remote URL: ${jGitSettings?.gitRemoteURL}"
                println "    Git Branch: ${jGitSettings?.branch}"
                configService.syncronizeDatabaseFromGit()
            } else {
                println "Missing jgit section in config!!!"
                println "Check your RuleChains config file!!!"
                println "Cannot syncronize database from Git repo!!!"
            }
            JobEventLogAppender.appInitialized = true
        }
        switch(GrailsUtil.environment){
            case "development":
                println "#### Development Mode (Start Up)"
                break
            case "test":
                println "#### Test Mode (Start Up)"
                break
            case "production":
                println "#### Production Mode (Start Up)"
                break
        }        
    }
    def destroy = {
        switch(GrailsUtil.environment){
            case "development":
                println "#### Development Mode (Shut Down)"
                break
            case "test":
                println "#### Test Mode (Shut Down)"
                break
            case "production":
                println "#### Production Mode (Shut Down)"
                break
        }        
    }
}
