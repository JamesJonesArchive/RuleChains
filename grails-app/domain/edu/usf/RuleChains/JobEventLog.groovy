package edu.usf.RuleChains

import grails.converters.*

class JobEventLog {

    final static int SOURCE_MAXSIZE = 255
    final static int MESSAGE_MAXSIZE = 1000
    final static int DETAILS_MAXSIZE = 4000
 
    Date dateCreated
 
    String message
    String details
    String source
    String scheduledChain
    String scheduledUniqueJobId
    String currentRunningChain
    String currentOperation
    String status    
 
    // did someone look at this error?
    boolean cleared = false
    
    static transients = ['jobInfo']
    
    static constraints = {
        source(blank: false, nullable: false, maxSize: SOURCE_MAXSIZE)
        message(blank: false, nullable: false, maxSize: MESSAGE_MAXSIZE)
        details(blank: true, nullable: true, maxSize: DETAILS_MAXSIZE)
        scheduledChain(blank: true, nullable: true)
        scheduledUniqueJobId(blank: true, nullable: true)
        currentRunningChain(blank: true, nullable: true)
        currentOperation(blank: true, nullable: true)
        status(blank:true)
    }
 
    static mapping = {
        status type: 'text'
        sort "dateCreated"
    }        
    
    def getJobInfo() {
        if(currentOperation == "SUMMARY") {
            return JSON.parse(status.minus("JobInfo "))
        }
        return [:]
    }    
}
