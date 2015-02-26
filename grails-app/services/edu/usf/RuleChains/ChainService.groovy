package edu.usf.RuleChains

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import grails.converters.*
import org.hibernate.criterion.CriteriaSpecification
import grails.util.GrailsUtil

/**
 * ChainService provide for the creation and manipulation of Chain and Link objects
 * <p>
 * Developed originally for the University of South Florida
 * 
 * @author <a href='mailto:james@mail.usf.edu'>James Jones</a> 
 */ 
class ChainService {
    static transactional = true
    def grailsApplication
    def jobService
    
    /**
     * Returns a list of Chain objects with an option matching filter
     * 
     * @param  pattern  An optional parameter. When provided the full list (default) will be filtered down with the regex pattern string when provided
     * @return          An object containing the resulting list of Chain objects
     * @see    Chain
     */    
    def listChains(String pattern = null) { 
        if(!!pattern) {
            return [chains: Chain.list().findAll {
                Pattern.compile(pattern.trim()).matcher(it.name).matches()
            }]
        } else {
            return [ chains: Chain.list() ]
        }
    }
    /**
     * Creates a new Chain
     * 
     * @param  name      The unique name of the new Chain
     * @param  isSynced  An optional parameter for syncing to Git. The default value is 'true' keeping sync turned on
     * @return           Returns an object containing the new Chain
     */
    def addChain(String name,boolean isSynced = true) {
        if(!!name) {
            def chain = [ name: name.trim() ] as Chain
            chain.isSynced = isSynced
            if(!chain.save(failOnError:false, flush: true, insert: true, validate: true)) {
                return [ error : "'${chain.errors.fieldError.field}' value '${chain.errors.fieldError.rejectedValue}' rejected" ]
            } else {
                return getChain(name.trim())
            }
        }
        return [ error: "You must supply a name" ]
    }
    /**
     * Renames an existing Chain
     * 
     * @param  name                              The name of the Chain to be updated
     * @param  newName                           The new name of the Chain to be updated
     * @param  isSynced                          An optional parameter for syncing to Git. The default value is 'true' keeping sync turned on
     * @return                                   Returns an object containing the updated Chain
     */
    def modifyChain(String name,String newName,boolean isSynced = true) {
        if(!!name && !!newName) {
            def chain = Chain.findByName(name.trim())
            if(!!chain) {         
                chain.isSynced = isSynced
                chain.name = newName.trim()
                if(!chain.save(failOnError:false, flush: true, validate: true)) {
                    return [ error : "'${chain.errors.fieldError.field}' value '${chain.errors.fieldError.rejectedValue}' rejected" ]
                } else {
                    return getChain(newName.trim())
                }
            }
            return [ error : "Chain named ${name} not found!"]
        }
        return [ error : "You must supply a name and new name for the target chain"]
    }
    /**
     * Removes an existing Chain by name
     * 
     * @param  name      The name of the Chain to be removed
     * @param  isSynced  An optional parameter for syncing to Git. The default value is 'true' keeping sync turned on
     * @return           Returns an object containing the sucess or error message
     */    
    def deleteChain(String name,boolean isSynced = true) {
        if(!!name) {
            def chain = Chain.findByName(name.trim())
            if(!!chain) {
                chain.isSynced = isSynced
                chain.delete()
                return [ success : "Chain deleted" ]
            }
            return [ error : "Chain named ${name} not found!"]
        }
        return [ error : "You must supply a name for the target Chain"]
    }
    /**
     * Finds a Chain by it's name
     * 
     * @param  name  The unique name of the Chain
     * @return       Returns a Chain if matched or returns an error message
     * @see    Chain
     */
    def getChain(String name) {
        if(!!name) {
            def chain = Chain.findByName(name.trim())
            if(!!chain) {
                def resultSet = [:]
                resultSet << chain.properties.subMap(['name','links','id'])
                if(!!!!resultSet.links) {
                    resultSet.links = Link.createCriteria().list(sort: 'sequenceNumber',order: 'asc') {
                        eq('chain',chain)
                        if(!(GrailsUtil.environment in ['test'])) {
                            resultTransformer(CriteriaSpecification.ALIAS_TO_ENTITY_MAP)
                            projections {
                                property('sequenceNumber', 'sequenceNumber')
                                property('rule', 'rule')
                                property('sourceName','sourceName')
                                property('executeEnum', 'executeEnum')
                                property('resultEnum', 'resultEnum')
                                property('linkEnum', 'linkEnum')
                                property('inputReorder', 'inputReorder')
                                property('outputReorder', 'outputReorder')
                            }                        
                        }
                    }
                }
                return [ chain: resultSet ]                    
            }
            return [ error : "Chain named ${name} not found!"]
        }
        return [ error : "You must supply a name for the target Chain"]
    }
    /**
     * Adds a new link to an existing chain
     * 
     * @param  name     The unique name of the Chain
     * @param  newLink  An object containing the link object to be added
     * @param  isSynced An optional parameter for syncing to Git. The default value is 'true' keeping sync turned on
     * @return          Returns an object containing the updated Chain
     */
    def addChainLink(String name,def newLink,boolean isSynced = true) {
        println "Adding a link"
        def chain = Chain.findByName(name.trim())
        if(!!chain) {
            chain.isSynced = isSynced
            // Prepare the new Link for the target chain
            def link = new Link(newLink.inject([:]) {l,k,v ->
                switch(k) {
                    case "executeEnum":
                        println v
                        l[k] = ExecuteEnum.byName((("name" in v)?v.name:v))
                        break
                    case "resultEnum":
                        println v
                        l[k] = ResultEnum.byName((("name" in v)?v.name:v))
                        break
                    case "linkEnum":
                        println v
                        l[k] = LinkEnum.byName((("name" in v)?v.name:v))
                        break
                    case "sequenceNumber":
                        l[k] = "$v".toLong()
                        if(chain.links?.size()) {
                            l[k] = ("$v".toInteger() > chain.links.size())?"${chain.links.size()}".toLong():l[k]
                        }
                        break
                    case "rule":
                        println ("name" in v)?v.name:v
                        l[k] = Rule.findByName(("name" in v)?v.name:v)
                        if(!!l[k]) {
                            l[k].isSynced = isSynced
                        }
                        break
                    default:
                        l[k] = v
                        break                    
                }
                return l
            })
            link.isSynced = isSynced
            Chain.withTransaction { status -> 
                Link.createCriteria().list(sort: 'sequenceNumber',order: 'desc') {
                    eq('chain',chain)
                    ge('sequenceNumber',link.sequenceNumber)
                }.each { l ->
                    l.isSynced = isSynced
                    // increment each one
                    l.sequenceNumber++
                    if(!l.save(failOnError:false, flush: false, validate: true)) {
                        return [ error : "'${l.errors.fieldError.field}' value '${l.errors.fieldError.rejectedValue}' rejected" ]
                    }
                }
                if(!!link.rule) {
                    link.isSynced = isSynced
                    try {
                        if(!chain.addToLinks(link).save(failOnError:false, flush: false, validate: true)) {
                            chain.errors.allErrors.each {
                                println "Error:"+it
                            }           
                            return [ error : "'${chain.errors.fieldError.field}' value '${chain.errors.fieldError.rejectedValue}' rejected" ]                                
                        } else {
                            status.flush()
                        }
                    } catch(Exception ex) {
                        link.errors.allErrors.each {
                            println it                        
                        }    
                        log.info ex.printStackTrace()
                        return [ error: "'${link.errors.fieldError.field}' value '${link.errors.fieldError.rejectedValue}' rejected" ]                
                    }    
                    return getChain(chain.name)
                } else {
                    return [ error : "The target rule for the link ${("name" in newLink.rule)?newLink.rule.name:newLink.rule} was not found!"]
                }
            }
        } else {
            return [ error: "Chain could not found" ]
        }
    }
    /**
     * Finds a Link by it's sequence number and Chain name
     * 
     * @param  name            The unique name of the Chain
     * @param  sequenceNumber  The sequence number of the link in the chain
     * @return                 Returns a Link if matched or returns an error message
     * @see    Link
     */    
    def getChainLink(String name,def sequenceNumber) {
        def chain = Chain.findByName(name.trim())
        if(!!chain) {
            def link = chain.links.find { it.sequenceNumber == sequenceNumber }
            if(!!link) {
                return [ link: link ]
            }
            return [ error : "Link sequence Number ${sequenceNumber} not found!"]
        }        
        return [ error : "Chain named ${name} not found!"]
    }
    /**
     * Moves a chain link to another position in the existing or another chain
     * 
     * @param  originalChainName          The unique name of the orginal Chain
     * @param  destinationChainName       The unique name of the destination Chain
     * @param  originalSequenceNumber     The sequence number of the link in the orginal chain
     * @param  destinationSequenceNumber  The sequence number of the link in the destination chain
     * @param  isSynced                   An optional parameter for syncing to Git. The default value is 'true' keeping sync turned on
     * @return                            Returns an object containing the updated Chain
     */ 
    def moveChainLink(String originalChainName,String destinationChainName,def originalSequenceNumber,def destinationSequenceNumber,boolean isSynced = true) {
        def chain = Chain.findByName(originalChainName.trim())
        if(!!chain) {
            def sequenceNumbers = [
                original: "${originalSequenceNumber}".toLong(),
                destination: "${destinationSequenceNumber}".toLong()
            ]
            def link = Link.createCriteria().get {
                eq('chain',chain)
                eq('sequenceNumber',sequenceNumbers.original)
            }
            if(!!link) {
                link.isSynced = isSynced
                if(originalChainName == destinationChainName && originalSequenceNumber.toLong() < destinationSequenceNumber.toLong()) {
                    sequenceNumbers.destination--
                }
                def status = deleteChainLink(originalChainName,originalSequenceNumber,isSynced)
                if("error" in status) {
                    return [ error: status.error ]
                } else {
                    def newLink = link.properties.subMap(['sourceName','rule','inputReorder','outputReorder','executeEnum','resultEnum','linkEnum'])
                    newLink.sequenceNumber = "${sequenceNumbers.destination}"
                    newLink.rule = newLink.rule.name
                    newLink.executeEnum = "${newLink.executeEnum}" 
                    newLink.resultEnum = "${newLink.resultEnum}" 
                    newLink.linkEnum = "${newLink.linkEnum}" 
                    return addChainLink(destinationChainName,newLink,isSynced)
                }
            } else {
                return [ error: "Link with ${sequenceNumbers.original} could not found" ]         
            }
        } else {
            return [ error : "Chain named ${originalChainName} not found!"]
        }
    }
    /**
     * Removes an existing link by sequence number and Chain name. The Chain links are reordered
     * sequentially without gaps.
     * 
     * @param  name            The name of the Chain to be removed
     * @param  sequenceNumber  The sequence number of the link in the chain
     * @param  isSynced        An optional parameter for syncing to Git. The default value is 'true' keeping sync turned on
     * @return                 Returns an object containing the updated Chain
     */    
    def deleteChainLink(String name,def sequenceNumber,boolean isSynced = true) {
        def chain = Chain.findByName(name.trim())
        if(!!chain) {
            chain.isSynced = isSynced
            def link = Link.createCriteria().get {
                eq('chain',chain)
                eq('sequenceNumber',"${sequenceNumber}".toLong())
            }
            if(!!link) {
                link.isSynced = isSynced
                Chain.withTransaction { status -> 
                    if(!chain.removeFromLinks(link).save(failOnError:false, flush: false, validate: true)) {
                        chain.errors.allErrors.each {
                            println "Error:"+it
                        }           
                        return [ error : "'${chain.errors.fieldError.field}' value '${chain.errors.fieldError.rejectedValue}' rejected" ]                    
                    } else {
                        link.delete()
                        Link.createCriteria().list(sort: 'sequenceNumber',order: 'asc') {
                            eq('chain',chain)
                            gt('sequenceNumber',"${sequenceNumber}".toLong())
                        }.each{ l -> 
                            l.sequenceNumber--
                            if(!l.save(failOnError:false, flush: false, validate: true)) {
                                l.errors.allErrors.each {
                                    println it
                                }    
                                return [ error : "'${l.errors.fieldError.field}' value '${l.errors.fieldError.rejectedValue}' rejected" ]                
                            }
                        }                        
                    }
                    status.flush()
                    return getChain(name.trim())
                }
            } else {
                return [ error: "Link with ${sequenceNumber} could not found" ]                
            }
        } else {
            return [ error: "Chain could not found" ]
        }
    }
    /**
     * Updates a target links property in a chain.
     * 
     * @param  name            The name of the ChainServiceHandler to be removed
     * @param  sequenceNumber  The sequence number of the target link in the chain
     * @param  updatedLink     An updated link object with updated properties to be applied to the target link
     * @param  isSynced        An optional parameter for syncing to Git. The default value is 'true' keeping sync turned on
     * @return                 Returns an object containing the updated Link
     * @see    Link
     * @see    Chain
     */    
    def modifyChainLink(String name,def sequenceNumber,def updatedLink,boolean isSynced = true) {
        def chain = Chain.findByName(name.trim())
        if(!!chain) {
            chain.isSynced = isSynced
            def link = chain.links.find { it.sequenceNumber.toString() == sequenceNumber.toString() }
            if(!!link) {
                link.isSynced = isSynced
                link.properties['sourceName','inputReorder','outputReorder','sequenceNumber','executeEnum','linkEnum','resultEnum','rule'] = updatedLink.inject([:]) { m,k,v ->
                    switch(k) {
                        case "executeEnum":
                            m[k] = ExecuteEnum.byName(("name" in v)?v.name:v)
                            break
                        case "linkEnum":
                            m[k] = LinkEnum.byName(("name" in v)?v.name:v)
                            break
                        case "resultEnum":
                            m[k] = ResultEnum.byName(("name" in v)?v.name:v)
                            break
                        case "rule":
                            m[k] = Rule.findByName(("name" in v)?v.name:v)
                            break
                        default:
                            m[k] = v
                            break
                    }
                    return m
                }
                if(!link.save(failOnError:false, flush: true, validate: true)) {
                    link.errors.allErrors.each {
                        println it
                    }           
                    return [ error : "'${link.errors.fieldError.field}' value '${link.errors.fieldError.rejectedValue}' rejected" ]                
                }
                return [ link : link]
            }
            return [ error : "Link with sequence ${sequenceNumber} not found!"]
        }
        return [ error : "Chain named ${name} not found!"]
    }
    /**
     * Retrieves a list of available sources and other objects strictly for the user interface
     * 
     * @return  An object containing available sources along with actions,jobgroups and currently executing jobs
     */
    def getSources() {
        String sfRoot = "sessionFactory_"
        return [ 
            sources: grailsApplication.mainContext.beanDefinitionNames.findAll{ it.startsWith( sfRoot ) }.collect { sf ->
                sf[sfRoot.size()..-1]
            },
            actions: [
                execute: ExecuteEnum.values().collect { it.name() },
                result: ResultEnum.values().collect { it.name() },
                link: LinkEnum.values().collect { it.name() }
            ],
            jobGroups: jobService.listChainJobs().jobGroups,
            executingJobs: jobService.listCurrentlyExecutingJobs()?.executingJobs            
        ]
        
    }    
}
