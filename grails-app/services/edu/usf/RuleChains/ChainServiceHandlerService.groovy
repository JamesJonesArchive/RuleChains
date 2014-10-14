package edu.usf.RuleChains

import grails.converters.*
import groovy.lang.GroovyShell
import groovy.lang.Binding
import java.util.regex.Matcher
import java.util.regex.Pattern
import groovy.sql.Sql
import oracle.jdbc.driver.OracleTypes
import grails.util.Holders

/**
 * ChainServiceHanderService provide for the processing and manipulation of ChainServiceHandler objects
 * <p>
 * Developed originally for the University of South Florida
 * 
 * @author <a href='mailto:james@mail.usf.edu'>James Jones</a> 
 */ 
class ChainServiceHandlerService {
    def chainService
    def jobService
    
    /**
     * Takes REST parameters, matches it on a method and processess
     * the matched Chain Service Handler {@link ChainServiceHandler}
     * against it's embedded Chain {@link Chain} with input parameters.
     * <p>
     * This method has it's own reordering handling to conform the input and output 
     * to the calling REST client's needs.
     * 
     * @param  name
     * @param  method
     * @param  input
     * @return The result of the chain execution along with optional grooming on the output reorder
     * @see    Chain
     * @see    ChainServiceHandler
     */ 
    def handleChainService(String name,String method,def input) {
        println "--------"
        println input as JSON
        println "--------"
        def chainServiceHandler = ChainServiceHandler.findByName(name)
        if(!!chainServiceHandler) {
            if(chainServiceHandler.method in [ MethodEnum.byName(method) ]) {
                def suffix = System.currentTimeMillis()
                // Attaches a JobInfo to the Chain as a transient
                chainServiceHandler.chain.jobInfo = [
                    chain: chainServiceHandler.chain.name,
                    suffix:suffix,
                    description: "ChainServiceHandler ${name} for ${chainServiceHandler.chain.name}",
                    groupName: "none",
                    cron: "rest triggered",
                    fireTime: new Date(suffix),
                    scheduledFireTime: new Date(suffix)                    
                ]
                def rows = [ Chain.rearrange(input,chainServiceHandler.inputReorder) ]
                return rearrange(chainServiceHandler.chain.execute(rows,chainServiceHandler.chain.getOrderedLinks()),chainServiceHandler.outputReorder)
            } else {
                return [ error: "Method '${chainServiceHandler.method.name()}' for handler '${name}' not defined" ]
            }
        } else {
            return [ error: "Chain Service Handler not found" ]
        }
        
    }
    /**
     * A special rearrange for returning REST responses.
     * This takes the whole value and allows you to manipulate it rather
     * than row by row
     * 
     * @param  rows       an array of objects
     * @param  rearrange  a groovy script with 'rows' bound in it to allow custom rearranging
     * @return            the rearranged array of objects processed through the rearrange groovy script
     */
    private def rearrange(def rows,String rearrange){
        println "unmodified ${rows}"
        println rows as JSON
        println "-------"
        if(!!rearrange) {
            println "Made it Rearrange ${rearrange}"
            String toBeEvaluated = """
                import groovy.sql.Sql
                import oracle.jdbc.driver.OracleTypes
                
                rcGlobals
                rows
                ${rearrange}
            """        
            try {
                return new GroovyShell(new Binding("rows":rows,rcGlobals: (Holders.config.ruleChains.globals)?Holders.config.ruleChains.globals:[:])).evaluate(toBeEvaluated)
            } catch(Exception e) {
                System.out.println("${rows.toString()} error: ${e.message} on closure: ${toBeEvaluated}")
            }
        }
        println "Skipped rearrange as it was ${rearrange}"
        return rows
    }    
    /**
     * Finds a ChainServiceHandler by it's name
     * 
     * @param  name  The unique name of the ChainServiceHandler
     * @return       Returns a ChainServiceHander if matched or returns an error message
     * @see    ChainServiceHandler
     */
    def getChainServiceHandler(String name) {
        if(!!name) {
            def chainServiceHandler = ChainServiceHandler.findByName(name.trim())
            if(!!chainServiceHandler) {
                return [ chainServiceHandler: chainServiceHandler ]
            }
            return [ error : "ChainServiceHandler named ${name} not found!"]
        }
        return [ error : "You must supply a name for the target ChainServiceHandler"]        
    }
    /**
     * Returns a list of ChainServiceHandler objects with an option matching filter
     * 
     * @param  pattern  An optional parameter. When provided the full list (default) will be filtered down with the regex pattern string when provided
     * @return          An object containing the resulting list of ChainServiceHandler objects
     * @see    ChainServiceHandler
     */
    def listChainServiceHandlers(String pattern = null) { 
        if(!!pattern) {
            return [chainServiceHandlers: ChainServiceHandler.list().findAll {
                    Pattern.compile(pattern.trim()).matcher(it.name).matches()
                }]
        } else {
            return [ chainServiceHandlers: ChainServiceHandler.list() ]
        }
    }
    /**
     * Creates a new ChainServiceHandler
     * 
     * @param  name      The unique name of the new ChainServiceHandler
     * @param  ch        A HashMap of the ChainServiceHandler properties
     * @param  isSynced  An optional parameter for syncing to Git. The default value is 'true' keeping sync turned on
     * @return           Returns an object containing the new ChainServiceHandler
     */
    def addChainServiceHandler(String name,def ch,boolean isSynced = true) {
        def chain = Chain.findByName(('name' in ch)?ch.name:ch)
        if(!!name && !!!!chain) {
            chain.isSynced = isSynced
            def chainServiceHandler = [ name: name.trim(), chain: chain ] as ChainServiceHandler
            chainServiceHandler.isSynced = isSynced
            if(!chainServiceHandler.save(failOnError:false, flush: true, insert: true, validate: true)) {
                return [ error : "Name value '${chainServiceHandler.errors.fieldError.rejectedValue}' rejected" ]
            } else {
                return [ chainServiceHandler: chainServiceHandler ]
            }
        } else if(!!!chain) {
            return [ error: "The chain specified does not exist" ]
        }
        return [ error: "You must supply a name" ]
    }
    /**
     * Modifies an existing ChainServiceHandler with updated options
     * 
     * @param  name                              The name of the ChainServiceHandler to be updated
     * @param  updatedChainServiceHandler        A HashMap of the ChainServiceHandler properties
     * @param  isSynced                          An optional parameter for syncing to Git. The default value is 'true' keeping sync turned on
     * @return                                   Returns an object containing the updated ChainServiceHandler
     */
    def modifyChainServiceHandler(String name,def updatedChainServiceHandler,boolean isSynced = true) {
        def chainServiceHandler = ChainServiceHandler.findByName(name.trim())
        if(!!chainServiceHandler) {
            chainServiceHandler.properties = updatedChainServiceHandler.inject([:]) {m,k,v ->
                switch(k) {
                    case "method":
                        m[k] = MethodEnum.byName((("name" in v)?v.name:v))
                        break
                    case "chain":
                        m[k] = Chain.findByName(("name" in v)?v.name:v)
                        break
                    default:
                        m[k] = v
                        break
                }
                return m
            }
            chainServiceHandler.isSynced = isSynced
            if(!chainServiceHandler.save(failOnError:false, flush: true, validate: true)) {
                chainServiceHandler.errors.allErrors.each {
                    println it
                }           
                return [ error : "'${chainServiceHandler.errors.fieldError.field}' value '${chainServiceHandler.errors.fieldError.rejectedValue}' rejected" ]                
            }
            return [ chainServiceHandler : chainServiceHandler]
        }
        return [ error : "Chain Service Handler named ${name} not found!"]
    }
    /**
     * Removes an existing ChainServiceHander by name
     * 
     * @param  name      The name of the ChainServiceHandler to be removed
     * @param  isSynced  An optional parameter for syncing to Git. The default value is 'true' keeping sync turned on
     * @return           Returns an object containing the sucess or error message
     */
    def deleteChainServiceHandler(String name,boolean isSynced = true) {
        if(!!name) {
            def chainServiceHandler = ChainServiceHandler.findByName(name.trim())
            if(!!chainServiceHandler) {
                chainServiceHandler.isSynced = isSynced
                chainServiceHandler.delete()
                return [ success : "Chain Service Handler deleted" ]
            }
            return [ error : "Chain Service Handler named ${name} not found!"]
        }
        return [ error : "You must supply a name for the target Chain Service Handler"]        
    }
}
