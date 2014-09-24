package edu.usf.RuleChains

import edu.usf.RuleChains.MethodEnum
import edu.usf.RuleChains.AuthTypeEnum
import edu.usf.RuleChains.ParseEnum

/**
 * DefinedService extends the basic Rule domain class and is the unit
 * for processing a rule containing parameters in calling an external
 * REST service.
 * <p>
 * Developed originally for the University of South Florida
 * 
 * @author <a href='mailto:james@mail.usf.edu'>James Jones</a> 
 */ 
class DefinedService extends Rule {
    MethodEnum method = MethodEnum.GET
    AuthTypeEnum authType = AuthTypeEnum.NONE
    ParseEnum parse = ParseEnum.TEXT
    String url = ""
    String user = ""
    String password = ""    
    String springSecurityBaseURL = ""   
    Map headers = [:]
    static constraints = {
        method( 
            blank: false,
            validator: { val, obj -> 
                return val.name() in MethodEnum.values().collect { it.name() }                
            }        
        )
        authType( 
            blank: false,
            validator: { val, obj -> 
                return val.name() in AuthTypeEnum.values().collect { it.name() }                
            }        
        )
        parse( 
            blank: false,
            validator: { val, obj -> 
                return val.name() in ParseEnum.values().collect { it.name() }                
            }        
        )
    }
    /*
     * Handles syncronization for saves 
     */    
    def afterInsert() {
        if(isSynced) {
            saveGitWithComment("Creating ${name} DefinedService")
        }
    }
    /*
     * Handles syncronization for update
     */    
    def beforeUpdate() {
        if(isSynced) {
            updateGitWithComment("Renaming ${name} DefinedService")
        }
    }
    /*
     * Handles syncronization for post-update saves 
     */        
    def afterUpdate() {
        if(isSynced) {
            saveGitWithComment("Updating ${name} DefinedService")
            /**
             * Anytime a rule is renamed, any link referenced rule name in git repo needs to be updated (if exists)
             **/
            Link.findAllByRule(this).each { l ->
                l.saveGitWithComment("Updating Link referencing ${name} SQLQuery")
            }
        }
    }
    /*
     * Handles syncronization for deletes 
     */        
    def beforeDelete() {
        if(isSynced) {
            deleteGitWithComment("Deleted ${name} DefinedService")
        }
    }
}
