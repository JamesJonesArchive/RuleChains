package edu.usf.RuleChains

/**
 * Snippet extends the basic Rule domain class and is the unit
 * for processing a rule which executes an embedded rule chain.
 * <p>
 * Developed originally for the University of South Florida
 * 
 * @author <a href='mailto:james@mail.usf.edu'>James Jones</a> 
 */ 
class Snippet extends Rule {
    Chain chain
    static constraints = {
        chain(nullable:true)
    }
    /*
     * Handles syncronization for saves 
     */    
    def afterInsert() {
        if(isSynced) {
            saveGitWithComment("Creating ${name} Snippet")
        }
    }
    /*
     * Handles syncronization for update
     */    
    def beforeUpdate() {
        if(isSynced) {
            updateGitWithComment("Renaming ${name} Snippet")
        }
    }
    /*
     * Handles syncronization for post-update saves 
     */        
    def afterUpdate() {
        if(isSynced) {
            saveGitWithComment("Updating ${name} Snippet")
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
            deleteGitWithComment("Deleted ${name} Snippet")
        }
    }
}
