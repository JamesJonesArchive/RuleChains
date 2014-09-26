package edu.usf.RuleChains

import javax.script.Bindings
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.ScriptException
import grails.converters.*
import groovy.lang.GroovyShell
import groovy.lang.Binding
import org.hibernate.ScrollMode
import edu.usf.RuleChains.*
import groovy.sql.Sql
import oracle.jdbc.driver.OracleTypes
import groovy.text.*
import au.com.bytecode.opencsv.*
import grails.util.Holders
/**
 * LinkService provides for the execution of various rule types and helper functions.
 * <p>
 * Developed originally for the University of South Florida
 * 
 * @author <a href='mailto:james@mail.usf.edu'>James Jones</a> 
 */ 
class LinkService {
    static transactional = true
    def grailsApplication
    /**
     * Retrieves the global variables hashmap from the config called "rcGlobals"
     * and combines it with an optional provided Map and some local variables on the 
     * current local environment.
     * 
     * @param  map       An optional parameter to add key/value pairs to the merge of global and local variables
     * @return           Returns an Map containing global,local and provided key/value pairs
     */
    def getMergedGlobals(def map = [:]) {
        return [ rcGlobals: (Holders.config.rcGlobals)?Holders.config.rcGlobals:[:] ] + map
    }
    /**
     * Executes a DefinedService with CAS SpringSecurity authentication
     * 
     * @param     serviceUrl             The URL of the REST service
     * @param     method                 GET,POST,DELETE or PUT method for REST submission
     * @param     parseEnum              The enumerator value indicating the result of the REST call will be parsed as that type
     * @param     username               The username for authorization
     * @param     password               The password for authorization
     * @param     headers                Any special headers to pass in the REST call
     * @param     query                  A set of parameters to send in the REST call
     * @param     springSecurityBaseUrl  The URL for SpringSecurity for that service
     * @return                           A parsed response from the REST service call
     * @see       ParseEnum
     */
    def casSpringSecurityRest(String serviceUrl,String method = "GET",ParseEnum parseEnum,String username,String password,def headers=[:],def query=[:],String springSecurityBaseUrl) {
        try {
            return [ { o ->
                switch(parseEnum) {
                    case ParseEnum.TEXT:
                        return [ o ]
                        break
                    case ParseEnum.JSON:
                        return (JSON.parse(o) as Map)
                        break
                    case ParseEnum.XML:
                        return XML.parse(o)
                        break                                        
                }
            }.call(withCasSpringSecurityRest(
                serviceUrl,
                method,
                username,
                password,
                headers,
                query,
                springSecurityBaseUrl
            )) ]  
        } catch(Exception e) {
            log.debug "${method} error: ${e.printStackTrace()} on service ${serviceUrl}"
            System.out.println("${method} error: ${e.printStackTrace()} on service ${serviceUrl}")
            return [
                error: e.message,
                method: method,
                type: "CASREST",
                url: serviceUrl
            ]                
        }        
    }
    /**
     * Executes a DefinedService with CAS authentication
     * 
     * @param     serviceUrl             The URL of the REST service
     * @param     method                 GET,POST,DELETE or PUT method for REST submission
     * @param     parseEnum              The enumerator value indicating the result of the REST call will be parsed as that type
     * @param     username               The username for authorization
     * @param     password               The password for authorization
     * @param     headers                Any special headers to pass in the REST call
     * @param     query                  A set of parameters to send in the REST call
     * @return                           A parsed response from the REST service call
     * @see       ParseEnum
     */
    def casRest(String serviceUrl,String method = "GET",ParseEnum parseEnum,String username,String password,def headers=[:],def query=[:]) {
        try {
            return [ { o ->
                switch(parseEnum) {
                    case ParseEnum.TEXT:
                        return [ o ]
                        break
                    case ParseEnum.JSON:
                        return (JSON.parse(o) as Map)
                        break
                    case ParseEnum.XML:
                        return XML.parse(o)
                        break                                        
                }
            }.call(withCasRest(
                serviceUrl,
                method,
                username,
                password,
                headers,
                query
            )) ]
        } catch(Exception e) {
            log.debug "${method} error: ${e.printStackTrace()} on service ${serviceUrl}"
            System.out.println("${method} error: ${e.printStackTrace()} on service ${serviceUrl}")
            return [
                error: e.message,
                method: method,
                type: "CASREST",
                url: serviceUrl
            ]                
        }
    }
    /**
     * Executes a DefinedService with CAS authentication
     * 
     * @param     serviceUrl             The URL of the REST service
     * @param     methodEnum             GET,POST,DELETE or PUT method for REST submission
     * @param     authTypeEnum           The enumerator value indicating the authorization type of the REST call
     * @param     parseEnum              The enumerator value indicating the result of the REST call will be parsed as that type
     * @param     username               The username for authorization
     * @param     password               The password for authorization
     * @param     headers                Any special headers to pass in the REST call
     * @param     query                  A set of parameters to send in the REST call
     * @return                           A parsed response from the REST service call
     * @see       ParseEnum
     */
    def justRest(String serviceUrl, MethodEnum methodEnum, AuthTypeEnum authTypeEnum,ParseEnum parseEnum, String username,String password,def headers=[:],def query=[:]) {
        try {
            return [ { o ->
                switch(parseEnum) {
                    case ParseEnum.TEXT:
                        return [ o ]
                        break
                    case ParseEnum.JSON:
                        return (JSON.parse(o) as Map)
                        break
                    case ParseEnum.XML:
                        return XML.parse(o)
                        break                                        
                }
            }.call(
                withRest(uri: serviceUrl) {
                    if(authTypeEnum in [AuthTypeEnum.BASIC,AuthTypeEnum.DIGEST]) {
                        auth.basic username, password
                    }
                    switch(methodEnum) {
                        case MethodEnum.GET:
                            return get(query: query ,headers: headers, requestContentType : 'application/x-www-form-urlencoded') { sresp, sreader -> 
                                switch(sresp.statusLine.statusCode) {
                                    case 200: 
                                        return sreader.toString()
                                        break
                                    default:
                                        return ""
                                        break
                                }                                                    
                            }
                            break
                        case MethodEnum.POST:
                            return post(body: query ,headers: headers, requestContentType : 'application/x-www-form-urlencoded') { sresp, sreader -> 
                                switch(sresp.statusLine.statusCode) {
                                    case 200: 
                                        return sreader.toString()
                                        break
                                    default:
                                        return ""
                                        break
                                }                                                    
                            }
                            break
                        case MethodEnum.PUT:
                            return put(body: query ,headers: headers, requestContentType : 'application/x-www-form-urlencoded') { sresp, sreader -> 
                                switch(sresp.statusLine.statusCode) {
                                    case 200: 
                                        return sreader.toString()
                                        break
                                    default:
                                        return ""
                                        break
                                }                                                    
                            }
                            break
                        case MethodEnum.DELETE:
                            return delete(query:query ,headers: headers, requestContentType : 'application/x-www-form-urlencoded') { sresp, sreader -> 
                                switch(sresp.statusLine.statusCode) {
                                    case 200: 
                                        return sreader.toString()
                                        break
                                    default:
                                        return ""
                                        break
                                }                                                    
                            }
                            break
                    }
                }
            ) ]     
        } catch(Exception e) {
            log.debug "${rule.name} error: ${e.printStackTrace()} on service ${serviceUrl}"
            System.out.println("${rule.name} error: ${e.printStackTrace()} on service ${serviceUrl}")
            return [
                error: e.message,
                rule: rule.name,
                type: "REST",
                url: serviceUrl
            ]                
        }
    }
    /**
     * Executes a PHP Script
     * 
     * @param     rule                   The text code of the rule
     * @param     sourceName             The string value of the database source to be executed on
     * @param     executeEnum            The enumerator value indicating how the script will be executed
     * @param     resultEnum             The enumerator value indicating the result will be handled
     * @param     input                  A set of parameters used in execution of the script
     * @return                           An array of objects representing the result of the script
     * @see       ExecuteEnum
     * @see       ResultEnum
     * @see       Rule
     */
    def justPHP(Rule rule,String sourceName,ExecuteEnum executeEnum,ResultEnum resultEnum,def input) {
        return Link.withTransaction{ status ->            
            try {
                return {rows->
                    switch(resultEnum) {
                        case [ ResultEnum.ROW,ResultEnum.APPENDTOROW,ResultEnum.PREPENDTOROW ]:
                            println "Before ${rows as JSON}"
                            println "After ${((rows.size() > 0)?rows[0..0]:rows) as JSON}"
                            return (rows.size() > 0)?rows[0..0]:rows
                            break
                        case [ ResultEnum.RECORDSET ]: 
                            return rows
                            break
                        case [ ResultEnum.NONE,ResultEnum.UPDATE ]:
                            return []
                            break
                    }
                }.call(
                    {
                        def gStringTemplateEngine = new GStringTemplateEngine()
                        def p = gStringTemplateEngine.createTemplate("""php << 'CODE'
                            <?php
                            $f = file("php://stdin");
                            $row = json_decode(urldecode("${input}"));
                            $rows = array();
                            ${rule}
                            ?>
                            echo json_encode($rows)
                            CODE
                            """).make([
                                'input' : URLEncoder.encode((input as JSON).toString(), 'UTF-8'),
                                'rule' : { r ->
                                    def gte = new GStringTemplateEngine()
                                    return gte.createTemplate(r).make(input).toString() 
                                }.call(rule)
                            ]).toString().execute(null,new File('/my/working/dir'))                        
                        p.waitFor()
                        return JSON.parse(p.text)
                    }.call()
                )
            } catch(Exception e) {
                log.debug "${rule.name} error: ${e.printStackTrace()} on source named ${sourceName}"
                System.out.println("${rule.name} error: ${e.printStackTrace()} on source named ${sourceName}")
                return [
                    error: e.message,
                    rule: rule.name,
                    type: "PHP",
                    source: sourceName
                ]
            }            
        }



        def e = new groovy.text.GStringTemplateEngine()

         def p = e.createTemplate("""php << 'CODE'


       <?php


       $f = file("php://stdin");

       $row = json_decode(urldecode("${input}"));

       $rows = array();


       ${rule}

       ?>

       echo json_encode($rows)

       CODE

       """).make([
            'input' : URLEncoder.encode((input as JSON).toString(), 'UTF-8'),
            'rule' : { r ->
                def gte = new groovy.text.GStringTemplateEngine()
                return gte.createTemplate(r).make(input).toString() 
            }.call(rule)
        ]).toString().execute(null,new File('/my/working/dir'))




       /**



        def p = """php << 'CODE'


       <?php


       $f = file("php://stdin");

       $row = json_decode(urldecode("${input}"));

       $rows = array();


       ${rule}

       ?>

       echo json_encode($rows)

       CODE





       """.execute(null,new File('/my/working/dir'))


       **/

        p.waitFor()


        return JSON.parse(p.text)


       }
    /**
     * Executes a Groovy Script
     * 
     * @param     rule                   The text code of the rule
     * @param     sourceName             The string value of the database source to be executed on
     * @param     executeEnum            The enumerator value indicating how the script will be executed
     * @param     resultEnum             The enumerator value indicating the result will be handled
     * @param     input                  A set of parameters used in execution of the script
     * @return                           An array of objects representing the result of the script
     * @see       ExecuteEnum
     * @see       ResultEnum
     * @see       Rule
     */
    def justGroovy(Rule rule,String sourceName,ExecuteEnum executeEnum,ResultEnum resultEnum,def input) {
        return Link.withTransaction{ status ->
            def sql = getSQLSource(sourceName)
            try {
                return {rows->
                    switch(resultEnum) {
                        case [ ResultEnum.ROW,ResultEnum.APPENDTOROW,ResultEnum.PREPENDTOROW ]:
                            println "Before ${rows as JSON}"
                            println "After ${((rows.size() > 0)?rows[0..0]:rows) as JSON}"
                            return (rows.size() > 0)?rows[0..0]:rows
                            break
                        case [ ResultEnum.RECORDSET ]: 
                            return rows
                            break
                        case [ ResultEnum.NONE,ResultEnum.UPDATE ]:
                            return []
                            break
                    }
                }.call(
                    new GroovyShell(new Binding([
                        longSQLplaceHolderUniqueVariable:sql,
                        longSQLSplaceHolderUniqueVariable:getSQLSources(),
                        longROWplaceHolderVariable: input,
                        rcGlobals: getMergedGlobals().rcGlobals
                    ])).evaluate("""
                        import grails.converters.*
                        import au.com.bytecode.opencsv.*

                        def sql = longSQLplaceHolderUniqueVariable
                        def sqls = longSQLSplaceHolderUniqueVariable
                        def row = longROWplaceHolderVariable
                        rcGlobals

                        ${rule.rule}
                    """)    
                )
            } catch(Exception e) {
                log.debug "${rule.name} error: ${e.printStackTrace()} on source named ${sourceName}"
                System.out.println("${rule.name} error: ${e.printStackTrace()} on source named ${sourceName}")
                return [
                    error: e.message,
                    rule: rule.name,
                    type: "Groovy",
                    source: sourceName
                ]                
            }
        }
    }
    /**
     * Executes a Python Script
     * 
     * @param     rule                   The text code of the rule
     * @param     sourceName             The string value of the database source to be executed on
     * @param     executeEnum            The enumerator value indicating how the script will be executed
     * @param     resultEnum             The enumerator value indicating the result will be handled
     * @param     input                  A set of parameters used in execution of the script
     * @return                           An array of objects representing the result of the script
     * @see       ExecuteEnum
     * @see       ResultEnum
     * @see       Rule
     */
    def justPython(Rule rule,String sourceName,ExecuteEnum executeEnum,ResultEnum resultEnum,def input) {
        return Link.withTransaction{ status ->
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("python")
            engine.put("row", input)
            engine.put("rcGlobals", getMergedGlobals().rcGlobals)
            engine.put("sql",getSQLSource().createConnection())
            engine.put("sqls",getSQLSources().collectEntries { key, value ->
                return [key, value.createConnection()]
            })
            try {
                return {rows->
                    switch(resultEnum) {
                        case [ ResultEnum.ROW,ResultEnum.APPENDTOROW,ResultEnum.PREPENDTOROW ]:
                            println "Before ${rows as JSON}"
                            println "After ${((rows.size() > 0)?rows[0..0]:rows) as JSON}"
                            return (rows.size() > 0)?rows[0..0]:rows
                            break
                        case [ ResultEnum.RECORDSET ]: 
                            return rows
                            break
                        case [ ResultEnum.NONE,ResultEnum.UPDATE ]:
                            return []
                            break
                    }
                }.call(
                    engine.eval("""
                        from java.sql import DriverManager
                        
                        ${rule.rule}
                    """)
                )
            } catch(Exception e) {
                log.debug "${rule.name} error: ${e.printStackTrace()} on source named ${sourceName}"
                System.out.println("${rule.name} error: ${e.printStackTrace()} on source named ${sourceName}")
                return [
                    error: e.message,
                    rule: rule.name,
                    type: "Python",
                    source: sourceName
                ]                
            }
        }
    }
    /**
     * Executes a SQL Script
     * 
     * @param     rule                   The text code of the rule
     * @param     sourceName             The string value of the database source to be executed on
     * @param     executeEnum            The enumerator value indicating how the script will be executed
     * @param     resultEnum             The enumerator value indicating the result will be handled
     * @param     input                  A set of parameters used in execution of the script
     * @return                           An array of objects representing the result of the script
     * @see       ExecuteEnum
     * @see       ResultEnum
     * @see       Rule
     */
    def justSQL(def rule,String sourceName,ExecuteEnum executeEnum,ResultEnum resultEnum,def input) {
        Link.withTransaction {
            def sql = getSQLSource(sourceName)
            try {
                println "Input is "+(input as JSON)
                switch(resultEnum) {
                     case [ ResultEnum.ROW,ResultEnum.APPENDTOROW,ResultEnum.PREPENDTOROW ]:
                         println input as JSON
                         return {rows-> (rows.size() > 0)?rows[0..0]:rows }.call((executeEnum in [ExecuteEnum.EXECUTE_USING_ROW])?sql.rows(rule.rule, input):sql.rows(rule.rule))
                         break
                     case [ ResultEnum.RECORDSET ]: 
                         return ((executeEnum in [ExecuteEnum.EXECUTE_USING_ROW])?sql.rows(rule.rule, input):sql.rows(rule.rule))
                         break
                     case [ ResultEnum.NONE,ResultEnum.UPDATE ]:
                         (executeEnum in [ExecuteEnum.EXECUTE_USING_ROW])?sql.execute(rule.rule, input):sql.execute(rule.rule)
                         return []
                         break
                 }
             } catch(Exception e) {
                 log.debug "${rule.name} error: ${e.printStackTrace()} on source named ${sourceName}"
                 System.out.println("${rule.name} error: ${e.printStackTrace()} on source named ${sourceName}")
                 return [
                     error: e.message,
                     rule: rule.name,
                     type: "SQL",
                     source: sourceName
                 ]
             }
        }
    }
    /**
     * Executes a SQL Stored Procedure Script
     * 
     * @param     rule                   The text code of the rule
     * @param     sourceName             The string value of the database source to be executed on
     * @param     executeEnum            The enumerator value indicating how the script will be executed
     * @param     resultEnum             The enumerator value indicating the result will be handled
     * @param     input                  A set of parameters used in execution of the script
     * @return                           An array of objects representing the result of the script
     * @see       ExecuteEnum
     * @see       ResultEnum
     * @see       Rule
     */
    def justStoredProcedure(def rule,String sourceName,ExecuteEnum executeEnum,ResultEnum resultEnum,def input) {
        Link.withTransaction {
            //println input as JSON
            def sql = getSQLSource(sourceName)
            def binding = new Binding()
            def closure = new GroovyShell(binding).evaluate(rule.closure)
            closure.delegate=this
            // Execute the stored procedure to populate the "rows" bound variable
            try {
                ((executeEnum in [ExecuteEnum.EXECUTE_USING_ROW])?sql.call(rule.rule, input,closure):sql.call(rule.rule, closure))
                switch(resultEnum) {
                    case [ ResultEnum.ROW,ResultEnum.APPENDTOROW,ResultEnum.PREPENDTOROW ]:
                        return {rows-> (rows.size() > 0)?rows[0..0]:rows }.call(binding.rows)
                        break;
                    case [ ResultEnum.RECORDSET ]: 
                        return binding.rows
                        break
                    case [ ResultEnum.NONE,ResultEnum.UPDATE ]:
                        return []
                        break
                }
            } catch(Exception e) {
                log.debug "${rule.name} error: ${e.printStackTrace()} on source named ${sourceName}"
                System.out.println("${rule.name} error: ${e.printStackTrace()} on source named ${sourceName}")
                return [
                    error: e.message,
                    rule: rule.name,
                    type: "Stored Procedure",
                    source: sourceName
                ]
            }
        }
    }
        
}
