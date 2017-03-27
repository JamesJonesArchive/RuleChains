package edu.usf.RuleChains

import grails.converters.*

/**
 * ChainServiceHanderController provides for REST services handling the processing and manipulation of ChainServiceHandler objects
 * <p>
 * Developed originally for the University of South Florida
 * 
 * @author <a href='mailto:james@mail.usf.edu'>James Jones</a> 
 */ 
class ChainServiceHandlerController {
    def chainService
    def chainServiceHandlerService
    
    /**
     * Takes REST parameters, matches it on a method and processess
     * the matched Chain Service Handler {@link ChainServiceHandler}
     * against it's embedded Chain {@link Chain} with input parameters.
     * 
     * @param  params   Normal params in a controller
     * @param  request  Provided the method used on the request
     * @return The result of the chain execution along with optional grooming on the output reorder
     * @see    Chain
     * @see    ChainServiceHandler
     */     
    def handleChainService() {
        System.out.println("Input service parameters are:")
        System.out.println(params as JSON)
        def handlerResult = { fparms ->
            return chainServiceHandlerService.handleChainService(params.handler,request.method,fparms)
        }.call(params.inject([:]) {m,k,v ->
            if(!(k in ['handler','action','controller'])) {
                m[k] = v
            }
            return m
        })
        withFormat {
            json {
                JSON.use("deep") { render handlerResult as JSON }
            }
            html {
                JSON.use("deep") { render handlerResult as JSON }
            }
            xml {
                render handlerResult as XML
           }
        }        
    }
    /**
     * Returns a list of ChainServiceHandler objects with an option matching filter
     * 
     * @param  pattern  An optional parameter contained in params. When provided the full list (default) will be filtered down with the regex pattern string when provided
     * @return          An object containing the resulting list of ChainServiceHandler objects
     * @see    ChainServiceHandler
     */    
    def listChainServiceHandlers() {
        withFormat {
            html {
                return chainServiceHandlerService.listChainServiceHandlers(params.pattern)
            }
            xml {
                render chainServiceHandlerService.listChainServiceHandlers(params.pattern) as XML
            }
            json {
                JSON.use("deep") { render chainServiceHandlerService.listChainServiceHandlers(params.pattern) as JSON }
            }
        }
    }
    /**
     * Creates a new ChainServiceHandler
     * 
     * @param  name      The unique name of the new ChainServiceHandler. This is the "name" key in the params object
     * @param  chain     The value of the "chain" key in the params object. This is the name of the target chain
     * @return           Returns an object containing the new ChainServiceHandler
     */    
    def addChainServiceHandler() {
        withFormat {
            html {
                return chainServiceHandlerService.addChainServiceHandler(params.name,params.chain)
            }
            xml {
                render chainServiceHandlerService.addChainServiceHandler(params.name,params.chain) as XML
            }
            json {
                render chainServiceHandlerService.addChainServiceHandler(params.name,params.chain) as JSON
            }
        }                            
    }
    /**
     * Modifies an existing ChainServiceHandler with updated options
     * 
     * @param  name                              The name of the ChainServiceHandler to be updated. This is the "name" key in the params object
     * @param  chainServiceHandler               A HashMap of the ChainServiceHandler properties. This is the "chainServiceHandler" key in the params object
     * @return                                   Returns an object containing the updated ChainServiceHandler
     */    
    def modifyChainServiceHandler() {
        withFormat {
            html {
                return chainServiceHandlerService.modifyChainServiceHandler(params.name,params.chainServiceHandler)
            }
            xml {
                render chainServiceHandlerService.modifyChainServiceHandler(params.name,params.chainServiceHandler) as XML
            }
            json {
                JSON.use("deep") { render chainServiceHandlerService.modifyChainServiceHandler(params.name,params.chainServiceHandler) as JSON }
            }
        }                                    
    }
    /**
     * Removes an existing ChainServiceHander by name
     * 
     * @param  name      The name of the ChainServiceHandler to be removed. This is the "name" key in the params object
     * @return           Returns an object containing the sucess or error message
     */    
    def deleteChainServiceHandler() {
        withFormat {
            html {
                return chainServiceHandlerService.deleteChainServiceHandler(params.name)
            }
            xml {
                render chainServiceHandlerService.deleteChainServiceHandler(params.name) as XML
            }
            json {
                JSON.use("deep") { render chainServiceHandlerService.deleteChainServiceHandler(params.name) as JSON }
            }
        }                                    
    }
}
