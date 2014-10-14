package edu.usf.RuleChains
import edu.usf.RuleChains.*
import org.hibernate.FlushMode
import grails.util.GrailsUtil

/**
 * The abstract Rule domain class and is the unit
 * for processing a rules if different extended types.
 * <p>
 * Developed originally for the University of South Florida
 * 
 * @author <a href='mailto:james@mail.usf.edu'>James Jones</a> 
 */ 
abstract class Rule {
    String name
    def jobInfo
    boolean isSynced = true
    
    static belongsTo = [ruleSet: RuleSet]
    static transients = ['jobInfo','isSynced']
    static constraints = {
        name(   
                blank: false,
                nullable: false,
                size: 3..255,
                unique: true,
                //Custom constraint - only allow upper, lower, digits, dash and underscore
                validator: { val, obj -> val ==~ /[A-Za-z0-9_.-]+/ && {
                        boolean valid = true;
                        Chain.withNewSession { session ->
                            session.flushMode = (GrailsUtil.environment in ['test'])?javax.persistence.FlushModeType.COMMIT:FlushMode.MANUAL
                            try {
                                valid = (obj instanceof Snippet)?(!!!!Chain.findByName(val) && !!!RuleSet.findByName(val) && !!!ChainServiceHandler.findByName(val)):(!!!Chain.findByName(val) && !!!RuleSet.findByName(val) && !!!ChainServiceHandler.findByName(val))                                
                            } finally {
                                session.setFlushMode((GrailsUtil.environment in ['test'])?javax.persistence.FlushModeType.AUTO:FlushMode.AUTO)
                            }
                        }
                        return valid
                    }.call()
                }
            )               

    }
}
