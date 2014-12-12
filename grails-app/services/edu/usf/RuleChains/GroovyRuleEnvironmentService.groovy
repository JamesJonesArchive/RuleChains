package edu.usf.RuleChains

import groovy.sql.Sql
import oracle.jdbc.driver.OracleTypes
import org.apache.camel.builder.RouteBuilder
import groovy.text.*
import grails.converters.*
import edu.usf.cims.emailer.EmailerEngine
import groovy.text.GStringTemplateEngine
import edu.usf.RuleChains.ExecuteEnum
import edu.usf.RuleChains.ResultEnum
import org.grails.plugins.csv.CSVMapReader
import org.grails.plugins.csv.CSVWriter

class GroovyRuleEnvironmentService {
    static transactional = true
    def linkService
    /**
     * Raw method for addToLog
     * 
     * @param   jobInfo    A hashmap containing the current job info
     * @param   message    The text of the log entry itself
     */
    def infoLog(def jobInfo,String message) {
        if(jobInfo) {
            print "I'm trying to log this somewhere" + jobInfo
            log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${jobInfo.name}][INFO] ${message}"
        } else {
            println "Log cannot be appended!"
        }
    }
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
    /**
     * Allows a RuleChain to be executed inside a groovy rule
     *
     * @param   chainName    The name of the target chain to be executed
     * @param   input        An optional array of objects for initializing or looping
     */
    def executeChain(String chainName,def input = [[:]]) {
        def chain = Chain.findByName(chainName)
        if(chain) {
            return chain.execute(input)
        } else {
            return null
        }
    }
    /**
     * Allows a specified Rule to be executed with link style parameters
     * 
     * @param   ruleName     A String to match the rule on
     * @param   source       A String to match the source on
     * @param   executeEnum  A String to match the executeEnum on
     * @param   resultEnum   A String to match the resultEnum on
     * @param   input        An optional map for templating and execute handling
     */
    def executeRule(String ruleName,String source,String executeEnum,String resultEnum,def input = [:]) {
        return executeRule(ruleName,source,ExecuteEnum.byName(executeEnum),ResultEnum.byName(resultEnum),input)
    }
    /**
     * Allows a specified Rule to be executed with link style parameters
     * 
     * @param   ruleName     A String to match the rule on
     * @param   source       A String to match the source on
     * @param   executeEnum  An ExecuteEnum enum type
     * @param   resultEnum   A ResultEnum enum type
     * @param   input        An optional map for templating and execute handling
     */
    def executeRule(String ruleName,String source,ExecuteEnum executeEnum,ResultEnum resultEnum,def input = [:]) {
        def rule = Rule.findByName(ruleName)
        if(rule) {
            def grailsApplication = new Chain().domainClass.grailsApplication
            if(!!!!grailsApplication.mainContext.beanDefinitionNames.findAll{ it.startsWith( 'sessionFactory_' ) }.find{ it.endsWith(source) }) {
                def output = []
                switch(rule) {
                    case { it instanceof SQLQuery }:
                        output = linkService.justSQL(
                            { p ->
                                def gStringTemplateEngine = new GStringTemplateEngine()
                                def r = [:]
                                r << p
                                r << [
                                    rule: gStringTemplateEngine.createTemplate(r.rule).make(getMergedGlobals(input)).toString()
                                ]
                                return r
                            }.call(rule.properties),
                            source,
                            executeEnum,    
                            resultEnum,
                            { e ->
                                switch(e) {
                                case ExecuteEnum.EXECUTE_USING_ROW: 
                                    return input
                                    break
                                default:
                                    return [:]
                                    break
                                }                                        
                            }.call(executeEnum)
                        )
                        break
                    case { it instanceof StoredProcedureQuery }:
                        output = linkService.justStoredProcedure(
                            { p ->
                                def gStringTemplateEngine = new GStringTemplateEngine()
                                def r = [:]
                                r << p
                                r << [
                                    rule: gStringTemplateEngine.createTemplate(r.rule).make(getMergedGlobals(input)).toString()
                                ]
                                println r.rule
                                return r
                            }.call(rule.properties),
                            source,
                            executeEnum,    
                            resultEnum,
                            { e ->
                                switch(e) {
                                case ExecuteEnum.EXECUTE_USING_ROW: 
                                    return input
                                    break
                                default:
                                    return [:]
                                    break
                                }                                        
                            }.call(executeEnum)
                        )
                        break
                    case { it instanceof Groovy }:
                        output = { r ->
                            if([Collection, Object[]].any { it.isAssignableFrom(r.getClass()) }) {
                                switch(r) {
                                case r.isEmpty():
                                    return r
                                    break
                                case [Collection, Object[]].any { it.isAssignableFrom(r[0].getClass()) }:
                                    return r
                                    break
                                default:
                                    return r
                                    break
                                }
                                return r
                            } else {
                                return [ r ] 
                            }
                        }.call(linkService.justGroovy(
                            rule,
                            source,
                            executeEnum,    
                            resultEnum,
                            { e ->
                                switch(e) {
                                case ExecuteEnum.EXECUTE_USING_ROW: 
                                    return input
                                    break
                                default:
                                    return [:]
                                    break
                                }                                        
                            }.call(executeEnum)
                        ))
                        break
                    case { it instanceof Python }:
                        output = { r ->
                            if([Collection, Object[]].any { it.isAssignableFrom(r.getClass()) }) {
                                switch(r) {
                                case r.isEmpty():
                                    return r
                                    break
                                case [Collection, Object[]].any { it.isAssignableFrom(r[0].getClass()) }:
                                    return r
                                    break
                                default:
                                    return r
                                    break
                                }
                                return r
                            } else {
                                return [ r ] 
                            }
                        }.call(linkService.justPython(
                            rule,
                            source,
                            executeEnum,    
                            resultEnum,
                            { e ->
                                switch(e) {
                                case ExecuteEnum.EXECUTE_USING_ROW: 
                                    return input
                                    break
                                default:
                                    return [:]
                                    break
                                }                                        
                            }.call(executeEnum)
                        ))
                        break
                    case { it instanceof Ruby }:
                        output = { r ->
                            if([Collection, Object[]].any { it.isAssignableFrom(r.getClass()) }) {
                                switch(r) {
                                case r.isEmpty():
                                    return r
                                    break
                                case [Collection, Object[]].any { it.isAssignableFrom(r[0].getClass()) }:
                                    return r
                                    break
                                default:
                                    return r
                                    break
                                }
                                return r
                            } else {
                                return [ r ] 
                            }
                        }.call(linkService.justRuby(
                            rule,
                            source,
                            executeEnum,    
                            resultEnum,
                            { e ->
                                switch(e) {
                                case ExecuteEnum.EXECUTE_USING_ROW: 
                                    return input
                                    break
                                default:
                                    return [:]
                                    break
                                }                                        
                            }.call(executeEnum)
                        ))
                        break
                    case { it instanceof DefinedService }:
                        def gStringTemplateEngine = new GStringTemplateEngine()
                        def credentials = [
                            user: gStringTemplateEngine.createTemplate(rule.user).make(getMergedGlobals().rcGlobals).toString(),
                            password: gStringTemplateEngine.createTemplate(rule.password).make(getMergedGlobals().rcGlobals).toString()
                        ]
                        switch(rule.authType) {
                        case AuthTypeEnum.CASSPRING:
                            output = linkService.casSpringSecurityRest(
                                rule.url,
                                rule.method.name(),
                                rule.parse,
                                credentials.user,
                                credentials.password,
                                rule.headers,
                                { e ->
                                    switch(e) {
                                    case ExecuteEnum.EXECUTE_USING_ROW: 
                                        return input
                                        break
                                    default:
                                        return [:]
                                        break
                                    }                                        
                                }.call(executeEnum),
                                rule.springSecurityBaseURL
                            )
                            break;
                        case AuthTypeEnum.CAS:
                            output = linkService.casRest(
                                rule.url,
                                rule.method.name(),
                                rule.parse,
                                credentials.user,
                                credentials.password,
                                rule.headers,
                                { e ->
                                    switch(e) {
                                    case ExecuteEnum.EXECUTE_USING_ROW: 
                                        return input
                                        break
                                    default:
                                        return [:]
                                        break
                                    }                                        
                                }.call(executeEnum)
                            )                                
                            break;
                        case [AuthTypeEnum.BASIC,AuthTypeEnum.DIGEST,AuthTypeEnum.NONE]:
                            output = linkService.justRest(
                                rule.url,
                                rule.method,
                                rule.authType, 
                                rule.parse,
                                credentials.user,
                                credentials.password,
                                rule.headers,
                                { e ->
                                    switch(e) {
                                    case ExecuteEnum.EXECUTE_USING_ROW: 
                                        return input
                                        break
                                    default:
                                        return [:]
                                        break
                                    }                                        
                                }.call(executeEnum)
                            )
                            break;
                        }
                        break
                    case { it instanceof Snippet }:
                        output = rule.chain.execute(
                            { e ->
                                switch(e) {
                                case ExecuteEnum.EXECUTE_USING_ROW: 
                                    return input
                                    break
                                default:
                                    return [[:]]
                                    break
                                }                                        
                            }.call(executeEnum)
                        )                    
                        break
                }
                if(resultEnum in [ ResultEnum.ROW,ResultEnum.APPENDTOROW,ResultEnum.PREPENDTOROW ]) {
                    output = (output)?[output.first()]:[] 
                } else if(resultEnum in [ ResultEnum.NONE,ResultEnum.UPDATE ]) {
                    output = []
                }
                return output.collect {
                    if(resultEnum in [ResultEnum.APPENDTOROW]) {
                        return (([:] << input) << it)
                    } else if(resultEnum in [ResultEnum.PREPENDTOROW]) {
                        return (([:] << it) << input)
                    }
                    return it                    
                }
            } else {
                return null
            }
        } else {
            return null
        }
    }
}
