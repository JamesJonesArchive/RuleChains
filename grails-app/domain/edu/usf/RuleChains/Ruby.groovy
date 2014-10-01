package edu.usf.RuleChains

/**
 * Ruby extends the basic Rule domain class and is the unit
 * for processing a rule containing text in the JRuby language.
 * <p>
 * Developed originally for the University of South Florida
 * 
 * @author <a href='mailto:james@mail.usf.edu'>James Jones</a> 
 */ 
class Ruby extends Rule {
    String rule = """# Output must be saved to this variable
out = []"""
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
            saveGitWithComment("Creating ${name} Ruby")
        }
    }
    /*
     * Handles syncronization for update
     */
    def beforeUpdate() {
        if(isSynced) {
            updateGitWithComment("Renaming ${name} Ruby")
        }
    }
    /*
     * Handles syncronization for post-update saves 
     */    
    def afterUpdate() {
        if(isSynced) {
            saveGitWithComment("Updating ${name} Ruby")
            /**
             * Anytime a rule is renamed, any link referenced rule name in git repo needs to be updated (if exists)
             **/
            Link.findAllByRule(this).each { l ->
                l.saveGitWithComment("Updating Link referencing ${name} Ruby")
            }
        }
    }
    /*
     * Handles syncronization for deletes 
     */    
    def beforeDelete() {
        if(isSynced) {
            deleteGitWithComment("Deleted ${name} Ruby")
        }
    }    
}
