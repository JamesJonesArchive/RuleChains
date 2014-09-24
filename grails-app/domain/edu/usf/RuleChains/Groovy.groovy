package edu.usf.RuleChains

/**
 * Groovy extends the basic Rule domain class and is the unit
 * for processing a rule containing text in the Groovy language.
 * <p>
 * Developed originally for the University of South Florida
 * 
 * @author <a href='mailto:james@mail.usf.edu'>James Jones</a> 
 */ 
class Groovy extends Rule {
    String rule = ""
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
            saveGitWithComment("Creating ${name} Groovy")
        }
    }
    /*
     * Handles syncronization for update
     */
    def beforeUpdate() {
        if(isSynced) {
            updateGitWithComment("Renaming ${name} Groovy")
        }
    }
    /*
     * Handles syncronization for post-update saves 
     */    
    def afterUpdate() {
        if(isSynced) {
            saveGitWithComment("Updating ${name} Groovy")
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
            // deleteGitWithComment("Deleted ${name} Groovy")
            def comment = "Deleted ${name} Groovy"
            withJGit { rf ->
                def relativePath = "ruleSets/${delegate.getPersistentValue('ruleSet').name}/${delegate.name}.json"
                pull().call()
                def f = new File(rf,relativePath)
                if(f.exists()) {
                    rm().addFilepattern("${relativePath}").call()
                    f.delete()
                }
                { clean ->
                    if(clean) {
                        commit().setMessage(comment).call()
                    } 
                }.call(status().call().isClean())
                push().call()
                pull().call()                
            }
        }
    }
    
}
