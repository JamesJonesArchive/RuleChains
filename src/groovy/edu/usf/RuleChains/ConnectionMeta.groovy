/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.usf.RuleChains

import edu.usf.RuleChains.LinkService
import groovy.sql.Sql
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer

/**
 * LinkMeta performs all the metaprogramming for the LinkService
 * mainly to handle Groovy SQL and Hibernate sessions
 * <p>
 * Developed originally for the University of South Florida
 * 
 * @author <a href='mailto:james@mail.usf.edu'>James Jones</a> 
 */ 
class ConnectionMeta {
    /**
     * The builder method that creates all the metaprogramming methods for connections
     * 
     * @param   grailsApplication    The Grails GrailsApplication object
     */
    def buildMeta = {grailsApplication->
        for (serviceClass in grailsApplication.serviceClasses) {
            /**
             * Retrives a hibernate session from the session factory based on a provided name match.
             * 
             * @param     name     The name of the datasource (when using multiple datasources)
             * @return             A hibernate session
             * @see       Session
             */
            serviceClass.metaClass.getSourceSession { String name ->
                String sfRoot = "sessionFactory_"
                def sfb = grailsApplication.mainContext.beanDefinitionNames.findAll{ it.startsWith( 'sessionFactory_' ) }.find{ it.endsWith(name) }
                if(!!!!sfb) {
                    return grailsApplication.mainContext."${sfb}".currentSession
                }
                return grailsApplication.mainContext."sessionFactory".currentSession
            }     
            /**
             * Retrives a Groovy SQL object from the session factory based on a provided name match.
             * 
             * @param     name     The name of the datasource (when using multiple datasources)
             * @return             A Groovy SQL object
             * @see       Sql
             */
            serviceClass.metaClass.getSQLSource { String name ->
                String sfRoot = "sessionFactory_"
                def sfb = grailsApplication.mainContext.beanDefinitionNames.findAll{ it.startsWith( 'sessionFactory_' ) }.find{ it.endsWith(name) }
                if(!!!!sfb) {
                    return new Sql(grailsApplication.mainContext."${sfb}".currentSession.connection())
                }
                return new Sql(grailsApplication.mainContext."sessionFactory".currentSession.connection())                
            }
            /**
             * Retrives hashmap of Groovy SQL objects derrived from the available session factories
             * 
             * @return             A hashmap of Groovy SQL objects
             * @see       Sql
             */
            serviceClass.metaClass.getSQLSources {
                String sfRoot = "sessionFactory_"
                return grailsApplication.mainContext.beanDefinitionNames.findAll{ it.startsWith( 'sessionFactory_' ) }.inject([:]) {m,b ->
                    m[b[sfRoot.size()..-1]] = new Sql(grailsApplication.mainContext."${b}".currentSession.connection())
                    return m
                }
            }
            /**
             * Retrieves the global variables hashmap from the config called "rcGlobals"
             * and combines it with an optional provided Map and some local variables on the 
             * current local environment.
             * 
             * @param  map       An optional parameter to add key/value pairs to the merge of global and local variables
             * @return           Returns an Map containing global,local and provided key/value pairs
             */
            serviceClass.metaClass.getMergedGlobals { map=[:] ->
                return [ rcGlobals: (grailsApplication.config.ruleChains.globals)?grailsApplication.config.ruleChains.globals:[:] ] + map
            }
        }
        /**
         * Retrieves the global variables hashmap from the config called "rcGlobals"
         * and combines it with an optional provided Map and some local variables on the 
         * current local environment.
         * 
         * @param  map       An optional parameter to add key/value pairs to the merge of global and local variables
         * @return           Returns an Map containing global,local and provided key/value pairs
         */
        Chain.metaClass.getMergedGlobals { map=[:] ->      
            return [ rcGlobals: (grailsApplication.config.ruleChains.globals)?grailsApplication.config.ruleChains.globals:[:] ] + map + [ rcLocals: [chain: delegate.name] ]
        }
        /**
         * Returns the config string of imports available in Groovy Rule scripts
         * 
         * @return   A string block of import statements
         */
        LinkService.metaClass.getGroovyRuleImports {->
            def imports = grailsApplication.config?.ruleChains.imports
            return (imports)?imports:""
        }
        /**
         * This is a workaround to make sure the groovy env is set
         * 
         * @return   A GroovyRuleEnvironmentService bean
         */
        LinkService.metaClass.getGroovyRuleEnvironmentService {->
            grailsApplication.mainContext.groovyRuleEnvironmentService
        }
    }	
}

