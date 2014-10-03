package edu.usf.RuleChains
import edu.usf.RuleChains.*
import org.hibernate.FlushMode
import grails.util.GrailsUtil

/**
 * ChainServiceHandler treats a rule chain as a callable REST service. This domain class
 * acts as the container to be processed.
 * <p>
 * Developed originally for the University of South Florida
 * 
 * @author <a href='mailto:james@mail.usf.edu'>James Jones</a> 
 */ 
class ChainServiceHandler {
    String name
    Chain chain
    String inputReorder = ''
    String outputReorder = ''
    MethodEnum method = MethodEnum.GET
    boolean isSynced = true
    static transients = ['isSynced']
    
    static mapping = {
        inputReorder type: 'text'
        outputReorder type: 'text'
    }    
    
    static constraints = {
        name(   
            blank: false,
            nullable: false,
            size: 3..255,
            unique: true,
            //Custom constraint - only allow upper, lower, digits, dash and underscore
            validator: { val, obj -> 
                val ==~ /[A-Za-z0-9_.-]+/ && {  
                    boolean valid = true;
                    ChainServiceHandler.withNewSession { session ->
                        session.flushMode = (GrailsUtil.environment in ['test'])?javax.persistence.FlushModeType.COMMIT:FlushMode.MANUAL
                        try {
                            valid = !!!Rule.findByName(val) && !!!RuleSet.findByName(val) && !!!Chain.findByName(val)
                        } finally {
                            session.setFlushMode((GrailsUtil.environment in ['test'])?javax.persistence.FlushModeType.AUTO:FlushMode.AUTO)
                        }
                    }
                    return valid
                }.call() 
            }
        )               
        inputReorder(blank:true)
        outputReorder(blank:true) 
        chain(nullable:false)
        method( 
            validator: { val, obj -> 
                return val.name() in MethodEnum.values().collect { it.name() }                
            }        
        )
    }
    /*
     * Handles syncronization for saves 
     */
    def afterInsert() {
        if(isSynced) {
            saveGitWithComment("Creating ${name} ChainServiceHandler")
        }
    }
    /*
     * Handles syncronization for update
     */
    def beforeUpdate() {
        if(isSynced) {
            updateGitWithComment("Renaming ${name} ChainServiceHandler")
        }
    }
    /*
     * Handles syncronization for post-update saves 
     */    
    def afterUpdate() {
        if(isSynced) {
            saveGitWithComment("Updating ${name} ChainServiceHandler")
        }
    }
    /*
     * Handles syncronization for deletes 
     */    
    def beforeDelete() {
        if(isSynced) {
            deleteGitWithComment("Deleted ${name} ChainServiceHandler")
        }
    }        
}
