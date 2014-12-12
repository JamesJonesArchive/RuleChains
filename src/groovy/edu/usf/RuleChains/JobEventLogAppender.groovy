package edu.usf.RuleChains

import org.apache.log4j.Appender
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.spi.LoggingEvent
import grails.converters.*
import java.util.regex.*

/**
 * JobEventLogAppender is the logging to database appender
 * for executing chains.
 * <p>
 * Developed originally for the University of South Florida
 * 
 * @author <a href='mailto:james@mail.usf.edu'>James Jones</a> 
 */ 
class JobEventLogAppender extends AppenderSkeleton implements Appender {
    static appInitialized = false
 
    String source
    /**
     * Modified function to handle logging to database
     * 
     * @param   event    A logging event
     */
    @Override
    protected void append(LoggingEvent event) {
        // print "LOG WAS CALLED"
        if (appInitialized) {
            //copied from Log4J's JDBCAppender
            event.getNDC();
            event.getThreadName();
            // Get a copy of this thread's MDC.
            event.getMDCCopy();
            event.getLocationInformation();
            event.getRenderedMessage();
            event.getThrowableStrRep();
 
            def limit = { string, maxLength -> string.substring(0, Math.min(string.length(), maxLength))}
 
            String logStatement = getLayout().format(event);
            String eventMessage = (event.getMessage())?event.getMessage().toString():"";
            
            if(event.getLevel() == org.apache.log4j.Level.INFO && eventMessage.startsWith("[Chain:")) {
                String logEntryPattern = "^\\[Chain:(\\w+):(\\d+)\\]\\[(\\w+)\\]\\[(\\w+)\\] (\\w.*)"
                Pattern p = Pattern.compile(logEntryPattern)
                Matcher matcher = p.matcher(eventMessage)
                def matches = [:]
                if (matcher.matches()) {
                    // use new transaction so that the log entry will be written even if the currently running transaction is rolled back
                    JobEventLog.withNewTransaction {
                        JobEventLog eventLog = new JobEventLog()
                        eventLog.message = limit((eventMessage ?: "Not details available, something is wrong"), JobEventLog.MESSAGE_MAXSIZE)
                        eventLog.details = limit((logStatement ?: "Not details available, something is wrong"), JobEventLog.DETAILS_MAXSIZE)
                        eventLog.source = limit(source ?: "Source not set", JobEventLog.SOURCE_MAXSIZE)
                        eventLog.scheduledChain = matcher.group(1)
                        eventLog.scheduledUniqueJobId = matcher.group(2)
                        eventLog.currentRunningChain = matcher.group(3)
                        eventLog.currentOperation = matcher.group(4)
                        eventLog.status = matcher.group(5)
                        eventLog.save()
                    }
                }             
            }
        } else {
            // println "JobEventLogAppender not initialized"
        }
    }
 
    /**
     * Set the source value for the logger (e.g. which application the logger belongs to)
     * @param source
     */
    public void setSource(String source) {
        this.source = source
    }
 
    @Override
    void close() {
        //noop
    }
 
    @Override
    boolean requiresLayout() {
        return true
    }    
}

