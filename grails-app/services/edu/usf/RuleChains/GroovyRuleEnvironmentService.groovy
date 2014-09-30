package edu.usf.RuleChains

import groovy.sql.Sql
import oracle.jdbc.driver.OracleTypes
import groovy.text.*
import grails.converters.*
import edu.usf.cims.emailer.EmailerEngine
import groovy.text.GStringTemplateEngine
import org.grails.plugins.csv.CSVMapReader
import org.grails.plugins.csv.CSVWriter

class GroovyRuleEnvironmentService {
    static transactional = true
    /**
     * Implements Chance Gray's EmailerEngine sendEmail method
     * 
     * @param  config     A formated config object as defined by the EmailerEngine class method
     * @param  emailText  Text of the email
     */
    def sendEmail(ConfigObject config, String emailText) {
        (new EmailerEngine()).sendEmail(config,emailText)
    }
    
}
