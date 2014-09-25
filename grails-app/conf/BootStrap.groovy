
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
import edu.usf.RuleChains.LinkMeta
import edu.usf.RuleChains.JobMeta
import edu.usf.RuleChains.GitMeta
import edu.usf.RuleChains.Groovy
import org.quartz.JobListener
import org.quartz.listeners.SchedulerListenerSupport

class BootStrap {
    def grailsApplication
    def quartzScheduler
    def jobService
    def springSecurityService
    def usfCasService
    def configService
    def linkMeta = new LinkMeta()
    def jobMeta = new JobMeta()
    def gitMeta = new GitMeta()
    def init = { servletContext ->
        if(!!!quartzScheduler) {
            print "Didn't get set!"
        } else {
            // Building the Meta Programing
            linkMeta.buildMeta(grailsApplication)
            jobMeta.buildMeta(quartzScheduler)
            gitMeta.buildMeta(grailsApplication)
            // Added Job Listener for Git Sync'ing
            quartzScheduler.getListenerManager().addJobListener(new RuleChainsJobListener() as JobListener)
            quartzScheduler.getListenerManager().addSchedulerListener(new RuleChainsSchedulerListener())
            print jobService.listChainJobs()
            configService.syncronizeDatabaseFromGit()
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
