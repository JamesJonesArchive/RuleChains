package edu.usf.RuleChains

/**
 * StoredProcedureQuery extends the basic Rule domain class and is the unit
 * for processing a rule containing a stored procedure call and the required
 * closure.
 * <p>
 * Developed originally for the University of South Florida
 * 
 * @author <a href='mailto:james@mail.usf.edu'>James Jones</a> 
 */ 
class StoredProcedureQuery extends Rule {
    String rule = ""
    String closure = """ 
{-> 
    rows = [
        [
            
        ]
    ]
}
"""
    static constraints = {
    }
    static mapping = {
        rule type: 'text'
    }    
    /*
     * Handles syncronization for saves 
     */    
    def afterInsert() {
        if(isSynced) {
            saveGitWithComment("Creating ${name} StoredProcedureQuery")
        }
    }
    /*
     * Handles syncronization for update
     */    
    def beforeUpdate() {
        if(isSynced) {
            updateGitWithComment("Renaming ${name} StoredProcedureQuery")
        }
    }
    /*
     * Handles syncronization for post-update saves 
     */        
    def afterUpdate() {
        if(isSynced) {
            saveGitWithComment("Updating ${name} StoredProcedureQuery")
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
            deleteGitWithComment("Deleted ${name} StoredProcedureQuery")
        }
    }
    
}
