package edu.usf.RuleChains

import edu.usf.RuleChains.ExecuteEnum
import edu.usf.RuleChains.ResultEnum
import edu.usf.RuleChains.LinkEnum
import grails.util.GrailsUtil

/**
 * Link domain class is a wrapper class for target rules with
 * processing directives.
 * <p>
 * Developed originally for the University of South Florida
 * 
 * @author <a href='mailto:james@mail.usf.edu'>James Jones</a> 
 */ 
class Link {
    Long sequenceNumber
    String sourceName
    Rule rule
    String inputReorder = ''
    String outputReorder = ''
    ExecuteEnum executeEnum = ExecuteEnum.NORMAL
    ResultEnum resultEnum = ResultEnum.NONE
    LinkEnum linkEnum = LinkEnum.NONE
    def input = [:]
    def output  = [[:]] 
    boolean isSynced = true    
    static transients = ['input','output','isSynced']
    static belongsTo = [chain: Chain]
    static mapping = {
        inputReorder type: 'text'
        outputReorder type: 'text'
    }    
    static constraints = {
        sourceName(
            blank: false,
            nullable: false,
            size: 3..255,
            validator: { val, obj -> 
                Link.sourceNameVerified(val)
            }
        )
        inputReorder(blank:true)
        outputReorder(blank:true)
        executeEnum( 
            validator: { val, obj -> 
                return val.name() in ExecuteEnum.values().collect { it.name() }
            }        
        )
        resultEnum( 
            validator: { val, obj -> 
                return val.name() in ResultEnum.values().collect { it.name() }
            }        
        )
        linkEnum( 
            validator: { val, obj -> 
                return val.name() in LinkEnum.values().collect { it.name() }
            }        
        )
        
    }
    /*
     * Handles syncronization for saves 
     */
    def afterInsert() {
        if(isSynced) {
            saveGitWithComment("Creating ${sequenceNumber} Link")
        }
    }
    /*
     * Handles syncronization for update
     */
    def beforeUpdate() {
        if(isSynced) {
            updateGitWithComment("Renaming ${sequenceNumber} Link")
        }
    }
    /*
     * Handles syncronization for post-update saves 
     */    
    def afterUpdate() {
        if(isSynced) {
            saveGitWithComment("Updating ${sequenceNumber} Link")
        }
    }
    /*
     * Handles syncronization for deletes 
     */    
    def beforeDelete() {
        if(isSynced) {
            deleteGitWithComment("Deleted ${sequenceNumber} Link")
        }
    }    
    /*
     * Verifies a source name to determine if it exists (is valid)
     * 
     * @param     sourceName   The name of the source
     * @return                 A boolean of whether the source actually exists
     */
    static sourceNameVerified(String sourceName) {
        if(GrailsUtil.environment in ['test']) {
            return true
        }
        def grailsApplication = new Link().domainClass.grailsApplication
        def ctx = grailsApplication.mainContext
        def config = grailsApplication.config
        // def fooService = ctx.fooService
        return !!grailsApplication.mainContext.beanDefinitionNames.findAll{ it.startsWith( 'sessionFactory_' ) }.find{ it.endsWith(sourceName) }
    }    
}
