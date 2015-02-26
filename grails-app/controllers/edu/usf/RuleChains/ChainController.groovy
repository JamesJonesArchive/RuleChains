package edu.usf.RuleChains

import grails.converters.*

/**
 * ChainController provides for REST services handling the processing and manipulation of Chain objects
 * <p>
 * Developed originally for the University of South Florida
 * 
 * @author <a href='mailto:james@mail.usf.edu'>James Jones</a> 
 */ 
class ChainController {
    def chainService
    def jobService
    /**
     * Returns a list of Chain objects with an option matching filter
     * 
     * @return          An object containing the resulting list of Chain objects
     * @see    Chain
     */        
    def listChains() { 
        withFormat {
            html {
                return chainService.listChains(params.pattern)
            }
            xml {
                render chainService.listChains(params.pattern) as XML
            }
            json {
                JSON.use("deep") { render chainService.listChains(params.pattern) as JSON }
            }
        }                    
    }
    /**
     * Creates a new Chain
     * 
     * @return           Returns an object containing the new Chain
     */
    def addChain() {
        withFormat {
            html {
                return chainService.addChain(params.name)
            }
            xml {
                render chainService.addChain(params.name) as XML
            }
            json {
                render chainService.addChain(params.name) as JSON
            }
        }                    
    }
    /**
     * Renames an existing Chain
     * 
     * @return                                   Returns an object containing the updated Chain
     */
    def modifyChain() {
        withFormat {
            html {
                return chainService.modifyChain(params.name,params.chain.name)
            }
            xml {
                render chainService.modifyChain(params.name,params.chain.name) as XML
            }
            json {
                render chainService.modifyChain(params.name,params.chain.name) as JSON
            }
        }                    
    }
    /**
     * Removes an existing Chain by name
     * 
     * @return           Returns an object containing the sucess or error message
     */    
    def deleteChain() {
        withFormat {
            html {
                return chainService.deleteChain(params.name)
            }
            xml {
                render chainService.deleteChain(params.name) as XML
            }
            json {
                render chainService.deleteChain(params.name) as JSON
            }
        }                    
    }
    /**
     * Finds a Chain by it's name
     * 
     * @return       Returns a Chain if matched or returns an error message
     * @see    Chain
     */
    def getChain() {
        withFormat {
            html {
                return chainService.getChain(params.name)
            }
            xml {
                render chainService.getChain(params.name) as XML
            }
            json {
                JSON.use("deep") { render chainService.getChain(params.name) as JSON }
            }
        }                    
    }    
    /**
     * Finds a Link by it's sequence number and Chain name
     * 
     * @return                 Returns a Link if matched or returns an error message
     * @see    Link
     */    
    def getChainLink() {
        withFormat {
            html {
                return chainService.getChainLink(params.name,params.sequenceNumber)
            }
            xml {
                render chainService.getChainLink(params.name,params.sequenceNumber) as XML
            }
            json {
                JSON.use("deep") { render chainService.getChainLink(params.name,params.sequenceNumber) as JSON }
            }
        }                            
    }
    /**
     * Adds a new link to an existing chain
     * 
     * @return          Returns an object containing the updated Chain
     */    
    def addChainLink() {
        withFormat {
            html {
                return chainService.addChainLink(params.name,params.link)
            }
            xml {
                render chainService.addChainLink(params.name,params.link) as XML
            }
            json {
                JSON.use("deep") { render chainService.addChainLink(params.name,params.link) as JSON }
            }
        }                            
    }
    /**
     * Removes an existing link by sequence number and Chain name. The Chain links are reordered
     * sequentially without gaps.
     * 
     * @return                 Returns an object containing the updated Chain
     */    
    def deleteChainLink() {
        withFormat {
            html {
                return chainService.deleteChainLink(params.name,params.sequenceNumber)
            }
            xml {
                render chainService.deleteChainLink(params.name,params.sequenceNumber) as XML
            }
            json {
                JSON.use("deep") { render chainService.deleteChainLink(params.name,params.sequenceNumber) as JSON }
            }
        }                            
    }
    def moveChainLink() {
        withFormat {
            html {
                return chainService.moveChainLink(params.originalChainName,params.destinationChainName,params.originalSequenceNumber,params.destinationSequenceNumber)
            }
            xml {
                render chainService.moveChainLink(params.originalChainName,params.destinationChainName,params.originalSequenceNumber,params.destinationSequenceNumber) as XML
            }
            json {
                JSON.use("deep") { render chainService.moveChainLink(params.originalChainName,params.destinationChainName,params.originalSequenceNumber,params.destinationSequenceNumber) as JSON }
            }
        }
    }
    /**
     * Updates a target links property in a chain.
     * 
     * @return                 Returns an object containing the updated Link
     * @see    Link
     * @see    Chain
     */    
    def modifyChainLink() {
        withFormat {
            html {
                return chainService.modifyChainLink(params.name,params.sequenceNumber,params.link)
            }
            xml {
                render chainService.modifyChainLink(params.name,params.sequenceNumber,params.link) as XML
            }
            json {
                JSON.use("deep") { render chainService.modifyChainLink(params.name,params.sequenceNumber,params.link) as JSON }
            }
        }                            
    }
    /**
     * Retrieves a list of available sources and other objects strictly for the user interface
     * 
     * @return  An object containing available sources along with actions,jobgroups and currently executing jobs
     */
    def getSources() {
        withFormat {
            html {
                return chainService.getSources()
            }
            xml {
                render chainService.getSources() as XML
            }
            json {
                render chainService.getSources() as JSON
            }
        }                            
    }
    
}
