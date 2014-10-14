package edu.usf.RuleChains

import groovy.sql.Sql
import oracle.jdbc.driver.OracleTypes
import org.apache.camel.builder.RouteBuilder
import groovy.text.*
import grails.converters.*
import edu.usf.cims.emailer.EmailerEngine
import groovy.text.GStringTemplateEngine
import org.grails.plugins.csv.CSVMapReader
import org.grails.plugins.csv.CSVWriter

class GroovyRuleEnvironmentService {
    static transactional = true
    /**
     * Implements Chance Gray's EmailerEngine sendEmail method with a bind hashmap
     * 
     * @param  config     A formated config object as defined by the EmailerEngine class method
     * @param  emailText  Text of the email
     * @param  bindMap    Optional hashmap to bind values to the email text via the GStringTemplateEngine
     */
    def sendEmail(ConfigObject config, String emailText,Map bindMap = [:]) { 
        if(bindMap) {
            (new EmailerEngine()).sendEmail(config,(new GStringTemplateEngine()).createTemplate(emailText).make(bindMap).toString())
        } else {
            (new EmailerEngine()).sendEmail(config,emailText)
        }
    }
    /**
     * Implements Camel JCIFS to create a RouteBuilder and pass it to a closure
     * 
     * @param   smbUrl    A JCIFS formatted url (http://camel.apache.org/jcifs.html)
     * @param   targetDir  A directory to syncronize (aka: "file://tmp/mysmbstuff")
     * @see     RouteBuilder
     */
    def createCIFSRouteBuilder(String smbUrl,String targetDir,Closure routeBuilderClosure) {
        routeBuilderClosure.call(new RouteBuilder() {
            public void configure() throws Exception {
                from(smbUrl)
                    .to(targetDir);
            }
        })        
    }
}
