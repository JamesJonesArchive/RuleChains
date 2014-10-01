package edu.usf.RuleChains
import groovy.lang.GroovyShell
import groovy.lang.Binding
import grails.util.GrailsNameUtils
import grails.converters.*
import edu.usf.RuleChains.*
import org.hibernate.FlushMode
import groovy.sql.Sql
import oracle.jdbc.driver.OracleTypes
import groovy.text.*
import grails.util.Holders
import grails.util.GrailsUtil

/**
 * Chain domain class is the sequencing object for processing
 * a sequence of rules.
 * <p>
 * Developed originally for the University of South Florida
 * 
 * @author <a href='mailto:james@mail.usf.edu'>James Jones</a> 
 */ 
class Chain {
    String name
    List<Link> links
    List<List> input = [[:]]
    boolean isSynced = true
    JobHistory jobHistory
    static hasMany = [links:Link]
    static fetchMode = [links: 'eager']
    static transients = ['orderedLinks','input','output','jobHistory','isSynced','mergedGlobals']
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
                        Rule.withNewSession { session ->
                            session.flushMode = (GrailsUtil.environment in ['test'])?javax.persistence.FlushModeType.COMMIT:FlushMode.MANUAL
                            try {
                                def r = Rule.findByName(val)
                                valid = ((r instanceof Snippet)?!!!!r:!!!r) && !!!RuleSet.findByName(val) && !!!ChainServiceHandler.findByName(val)
                            } finally {
                                session.setFlushMode((GrailsUtil.environment in ['test'])?javax.persistence.FlushModeType.AUTO:FlushMode.AUTO)
                            }
                        }
                        return valid
                    }.call() 
                }
            )               
    }
    /*
     * Handles syncronization for saves 
     */    
    def afterInsert() {
        if(isSynced) {
            saveGitWithComment("Creating ${name} Chain")
        }
    }
    /*
     * Handles syncronization for update
     */    
    def beforeUpdate() {
        if(isSynced) {
            updateGitWithComment("Updating ${name} Chain")
        }
    }
    /*
     * Handles syncronization for deletes 
     */            
    def afterDelete() {
        if(isSynced) {
            deleteGitWithComment("Deleted ${name} Chain")
        }
    }    
    
    /**
     * Anytime a chain is renamed, snippet reference name needs to be renamed (if exists)
     * and any ChainServiceHandlers their reference name updated as well
     **/
    def afterUpdate() {  
        if(!(GrailsUtil.environment in ['test'])) {
            Snippet.findAllByChain(this).each { s ->
                if(s.name != name) {
                    s.name=name
                    s.isSynced = isSynced
                    s.save()
                }
            }
        }
        if(isSynced) {
            ChainServiceHandler.findAllByChain(this).each { h ->
                h.saveGitWithComment("Updating ChainServicesHandler referencing ${name} Chain")
            }
        }
    }
    /**
     * Before a chain is deleted, snippet reference and any links using it need to be removed
     **/
    def beforeDelete() {
        Snippet.findAllByChain(this).each { s ->
            Link.findAllByRule(s).each { l ->
                (new ChainService()).deleteChainLink(l.chain.name,l.sequenceNumber)
            }
            (new RuleSetService()).deleteRule(s.ruleSet.name,s.name)
        }
    }
    /*
     * Retrieves the links from this chain ordered by sequence number
     * 
     * @return     A list of sorted links
     */
    def getOrderedLinks() {
        links.sort{it.sequenceNumber}
    }
    /*
     * Returns the final output of the chain (after execution it is set)
     * 
     * @return     The output of the last link that was processed in the sequence
     */
    def getOutput() {
        getOrderedLinks().last().output
    }
    /*
     * Executes the chain sequence of links with their referenced rules
     * 
     * @param     input         An array of objects to be used as input parameters
     * @param     orderedLinks  A list of links on this chain 
     * @return                  An array of objects
     */
    def execute(def input = [[:]],List<Link> orderedLinks = getOrderedLinks()) {
        println "I'm running"
        jobHistory.appendToLog("[START_EXECUTE] Chain ${name}")
        if(!!!orderedLinks) {
            orderedLinks = getOrderedLinks()
        }
        
        def linkService = new LinkService()
        ((!!input)?input:[[:]]).each { row ->
            /**
             * Pre-populate input based on incoming data array
            **/
            println "Made it this far with ${row}"
            for(int i = 0; i < orderedLinks.size(); i++) {
                orderedLinks[i].input = row
            }
            /**
             * Distinguish what kind of "rules" and handle them by type
            **/
            for(int i = 0; i < orderedLinks.size(); i++) {
                log.info "Unmodified input for link ${i} is ${orderedLinks[i].input as JSON}"
                // Execute the rule based on it's type
                log.info "Modified rearranged input for link ${i} is ${Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder) as JSON}"

                switch(orderedLinks[i].rule) {
                    case { it instanceof SQLQuery }:
                        jobHistory.appendToLog("[SQLQuery] Detected a SQLQuery for ${orderedLinks[i].rule.name}")
                        log.info "Detected an SQLQuery for ${orderedLinks[i].rule.name}"
                        orderedLinks[i].output = linkService.justSQL(
                            { p ->
                                def gStringTemplateEngine = new GStringTemplateEngine()
                                def rule = [:]
                                rule << p
                                rule << [
                                    rule: gStringTemplateEngine.createTemplate(rule.rule).make(getMergedGlobals(Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder))).toString(),
                                    jobHistory: jobHistory
                                ]
                                log.info rule.rule
                                jobHistory.appendToLog("[SQLQuery] Untemplated Rule is: ${p.rule}")
                                jobHistory.appendToLog("[SQLQuery] Unmodified input for Templating Rule on link ${i} is ${orderedLinks[i].input as JSON}")
                                jobHistory.appendToLog("[SQLQuery] Modified input for Templating Rule on link ${i} is ${Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder) as JSON}")
                                jobHistory.appendToLog("[SQLQuery] Templated Rule is: ${rule.rule}")
                                return rule
                            }.call(orderedLinks[i].rule.properties),
                            orderedLinks[i].sourceName,
                            orderedLinks[i].executeEnum,    
                            orderedLinks[i].resultEnum,
                            { e ->
                                switch(e) {
                                    case ExecuteEnum.EXECUTE_USING_ROW: 
                                        jobHistory.appendToLog("[SQLQuery] Execute Using Row being used")
                                        jobHistory.appendToLog("[SQLQuery] Unmodified input for Executing Row on link ${i} is ${orderedLinks[i].input as JSON}")
                                        jobHistory.appendToLog("[SQLQuery] Modified input for Executing Row link ${i} is ${Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder) as JSON}")
                                        return Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder)
                                        break
                                    default:
                                        return [:]
                                        break
                                }                                        
                            }.call(orderedLinks[i].executeEnum)
                        ).collect {
                            if(orderedLinks[i].resultEnum in [ResultEnum.APPENDTOROW]) {
                                return Chain.rearrange((([:] << orderedLinks[i].input) << it),orderedLinks[i].outputReorder)
                            } else if(orderedLinks[i].resultEnum in [ResultEnum.PREPENDTOROW]) {
                                return Chain.rearrange((([:] << it) << orderedLinks[i].input),orderedLinks[i].outputReorder)
                            }
                            return Chain.rearrange(it,orderedLinks[i].outputReorder)
                        }
                        break
                    case { it instanceof StoredProcedureQuery }:
                        jobHistory.appendToLog("[StoredProcedureQuery] Detected a Stored Procedure for ${orderedLinks[i].rule.name}")                        
                        log.info "Detected a StoredProcedureQuery Script for ${orderedLinks[i].rule.name}"
                        orderedLinks[i].output = linkService.justStoredProcedure(
                            { p ->
                                def gStringTemplateEngine = new GStringTemplateEngine()
                                def rule = [:]
                                rule << p
                                rule << [
                                    rule: gStringTemplateEngine.createTemplate(rule.rule).make(getMergedGlobals(Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder))).toString(),
                                    jobHistory: jobHistory
                                ]
                                log.info rule.rule
                                jobHistory.appendToLog("[StoredProcedureQuery] Untemplated Rule is: ${p.rule}")
                                jobHistory.appendToLog("[StoredProcedureQuery] Unmodified input for Templating Rule on link ${i} is ${orderedLinks[i].input as JSON}")
                                jobHistory.appendToLog("[StoredProcedureQuery] Modified input for Templating Rule on link ${i} is ${Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder) as JSON}")
                                jobHistory.appendToLog("[StoredProcedureQuery] Templated Rule is: ${rule.rule}")
                                println rule.rule
                                return rule
                            }.call(orderedLinks[i].rule.properties),
                            orderedLinks[i].sourceName,
                            orderedLinks[i].executeEnum,    
                            orderedLinks[i].resultEnum,
                            { e ->
                                switch(e) {
                                    case ExecuteEnum.EXECUTE_USING_ROW: 
                                        jobHistory.appendToLog("[StoredProcedureQuery] Execute Using Row being used")
                                        jobHistory.appendToLog("[StoredProcedureQuery] Unmodified input for Executing Row on link ${i} is ${orderedLinks[i].input as JSON}")
                                        jobHistory.appendToLog("[StoredProcedureQuery] Modified input for Executing Row link ${i} is ${Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder) as JSON}")                                        
                                        return Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder)
                                        break
                                    default:
                                        return [:]
                                        break
                                }                                        
                            }.call(orderedLinks[i].executeEnum)
                        ).collect {
                            if(orderedLinks[i].resultEnum in [ResultEnum.APPENDTOROW]) {
                                return Chain.rearrange((([:] << orderedLinks[i].input) << it),orderedLinks[i].outputReorder)
                            } else if(orderedLinks[i].resultEnum in [ResultEnum.PREPENDTOROW]) {
                                return Chain.rearrange((([:] << it) << orderedLinks[i].input),orderedLinks[i].outputReorder)
                            }
                            return Chain.rearrange(it,orderedLinks[i].outputReorder)
                        }
                        break
                    case { it instanceof Groovy }:
                        jobHistory.appendToLog("[Groovy] Detected a Groovy script for ${orderedLinks[i].rule.name}")                        
                        log.info "Detected a Groovy Script for ${orderedLinks[i].rule.name}"
                        orderedLinks[i].rule.jobHistory = jobHistory
                        orderedLinks[i].output = { r ->
                            if([Collection, Object[]].any { it.isAssignableFrom(r.getClass()) }) {
                                switch(r) {
                                    case r.isEmpty():
                                        return r
                                        break
                                    case [Collection, Object[]].any { it.isAssignableFrom(r[0].getClass()) }:
                                        return r
                                        break
                                    default:
                                        return r
                                        break
                                }
                                return r
                            } else {
                                jobHistory.appendToLog("[Groovy] Object needs to be an array of objects so wrapping it as an array like this ${[r] as JSON}") 
                                return [ r ] 
                            }
                        }.call(linkService.justGroovy(
                            orderedLinks[i].rule,
                            orderedLinks[i].sourceName,
                            orderedLinks[i].executeEnum,    
                            orderedLinks[i].resultEnum,
                            { e ->
                                switch(e) {
                                    case ExecuteEnum.EXECUTE_USING_ROW: 
                                        jobHistory.appendToLog("[Groovy] Execute Using Row being used")
                                        jobHistory.appendToLog("[Groovy] Unmodified input for Executing Row on link ${i} is ${orderedLinks[i].input as JSON}")
                                        jobHistory.appendToLog("[Groovy] Modified input for Executing Row link ${i} is ${Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder) as JSON}")                                        
                                        return Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder)
                                        break
                                    default:
                                        return [:]
                                        break
                                }                                        
                            }.call(orderedLinks[i].executeEnum)
                        )).collect {
                            if(orderedLinks[i].resultEnum in [ResultEnum.APPENDTOROW]) {
                                return Chain.rearrange((([:] << orderedLinks[i].input) << it),orderedLinks[i].outputReorder)
                            } else if(orderedLinks[i].resultEnum in [ResultEnum.PREPENDTOROW]) {
                                return Chain.rearrange((([:] << it) << orderedLinks[i].input),orderedLinks[i].outputReorder)
                            }
                            return Chain.rearrange(it,orderedLinks[i].outputReorder)
                        }
                        break
                    case { it instanceof Python }:
                        jobHistory.appendToLog("[Python] Detected a Python script for ${orderedLinks[i].rule.name}")                        
                        log.info "Detected a Python Script for ${orderedLinks[i].rule.name}"
                        orderedLinks[i].rule.jobHistory = jobHistory
                        orderedLinks[i].output = { r ->
                            if([Collection, Object[]].any { it.isAssignableFrom(r.getClass()) }) {
                                switch(r) {
                                    case r.isEmpty():
                                        return r
                                        break
                                    case [Collection, Object[]].any { it.isAssignableFrom(r[0].getClass()) }:
                                        return r
                                        break
                                    default:
                                        return r
                                        break
                                }
                                return r
                            } else {
                                jobHistory.appendToLog("[Python] Object needs to be an array of objects so wrapping it as an array like this ${[r] as JSON}") 
                                return [ r ] 
                            }
                        }.call(linkService.justPython(
                            orderedLinks[i].rule,
                            orderedLinks[i].sourceName,
                            orderedLinks[i].executeEnum,    
                            orderedLinks[i].resultEnum,
                            { e ->
                                switch(e) {
                                    case ExecuteEnum.EXECUTE_USING_ROW: 
                                        jobHistory.appendToLog("[Python] Execute Using Row being used")
                                        jobHistory.appendToLog("[Python] Unmodified input for Executing Row on link ${i} is ${orderedLinks[i].input as JSON}")
                                        jobHistory.appendToLog("[Python] Modified input for Executing Row link ${i} is ${Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder) as JSON}")                                        
                                        return Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder)
                                        break
                                    default:
                                        return [:]
                                        break
                                }                                        
                            }.call(orderedLinks[i].executeEnum)
                        )).collect {
                            if(orderedLinks[i].resultEnum in [ResultEnum.APPENDTOROW]) {
                                return Chain.rearrange((([:] << orderedLinks[i].input) << it),orderedLinks[i].outputReorder)
                            } else if(orderedLinks[i].resultEnum in [ResultEnum.PREPENDTOROW]) {
                                return Chain.rearrange((([:] << it) << orderedLinks[i].input),orderedLinks[i].outputReorder)
                            }
                            return Chain.rearrange(it,orderedLinks[i].outputReorder)
                        }
                        break                        
                    case { it instanceof Ruby }:
                        jobHistory.appendToLog("[Ruby] Detected a Ruby script for ${orderedLinks[i].rule.name}")                        
                        log.info "Detected a Ruby Script for ${orderedLinks[i].rule.name}"
                        orderedLinks[i].rule.jobHistory = jobHistory
                        orderedLinks[i].output = { r ->
                            if([Collection, Object[]].any { it.isAssignableFrom(r.getClass()) }) {
                                switch(r) {
                                    case r.isEmpty():
                                        return r
                                        break
                                    case [Collection, Object[]].any { it.isAssignableFrom(r[0].getClass()) }:
                                        return r
                                        break
                                    default:
                                        return r
                                        break
                                }
                                return r
                            } else {
                                jobHistory.appendToLog("[Ruby] Object needs to be an array of objects so wrapping it as an array like this ${[r] as JSON}") 
                                return [ r ] 
                            }
                        }.call(linkService.justRuby(
                            orderedLinks[i].rule,
                            orderedLinks[i].sourceName,
                            orderedLinks[i].executeEnum,    
                            orderedLinks[i].resultEnum,
                            { e ->
                                switch(e) {
                                    case ExecuteEnum.EXECUTE_USING_ROW: 
                                        jobHistory.appendToLog("[Ruby] Execute Using Row being used")
                                        jobHistory.appendToLog("[Ruby] Unmodified input for Executing Row on link ${i} is ${orderedLinks[i].input as JSON}")
                                        jobHistory.appendToLog("[Ruby] Modified input for Executing Row link ${i} is ${Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder) as JSON}")                                        
                                        return Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder)
                                        break
                                    default:
                                        return [:]
                                        break
                                }                                        
                            }.call(orderedLinks[i].executeEnum)
                        )).collect {
                            if(orderedLinks[i].resultEnum in [ResultEnum.APPENDTOROW]) {
                                return Chain.rearrange((([:] << orderedLinks[i].input) << it),orderedLinks[i].outputReorder)
                            } else if(orderedLinks[i].resultEnum in [ResultEnum.PREPENDTOROW]) {
                                return Chain.rearrange((([:] << it) << orderedLinks[i].input),orderedLinks[i].outputReorder)
                            }
                            return Chain.rearrange(it,orderedLinks[i].outputReorder)
                        }
                        break                        
                    case { it instanceof PHP }:
                        jobHistory.appendToLog("[PHP] Detected a PHP script for ${orderedLinks[i].rule.name}")                        
                        log.info "Detected a PHP Script for ${orderedLinks[i].rule.name}"
                        orderedLinks[i].rule.jobHistory = jobHistory
                        orderedLinks[i].output = { r ->
                            if([Collection, Object[]].any { it.isAssignableFrom(r.getClass()) }) {
                                switch(r) {
                                    case r.isEmpty():
                                        return r
                                        break
                                    case [Collection, Object[]].any { it.isAssignableFrom(r[0].getClass()) }:
                                        return r
                                        break
                                    default:
                                        return r
                                        break
                                }
                                return r
                            } else {
                                jobHistory.appendToLog("[PHP] Object needs to be an array of objects so wrapping it as an array like this ${[r] as JSON}") 
                                return [ r ] 
                            }
                        }.call(linkService.justPHP(
                            orderedLinks[i].rule,
                            orderedLinks[i].sourceName,
                            orderedLinks[i].executeEnum,    
                            orderedLinks[i].resultEnum,
                            { e ->
                                switch(e) {
                                    case ExecuteEnum.EXECUTE_USING_ROW: 
                                        jobHistory.appendToLog("[PHP] Execute Using Row being used")
                                        jobHistory.appendToLog("[PHP] Unmodified input for Executing Row on link ${i} is ${orderedLinks[i].input as JSON}")
                                        jobHistory.appendToLog("[PHP] Modified input for Executing Row link ${i} is ${Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder) as JSON}")
                                        return Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder)
                                        break
                                    default:
                                        return [:]
                                        break
                                }                                        
                            }.call(orderedLinks[i].executeEnum)
                        )).collect {
                            if(orderedLinks[i].resultEnum in [ResultEnum.APPENDTOROW]) {
                                return Chain.rearrange((([:] << orderedLinks[i].input) << it),orderedLinks[i].outputReorder)
                            } else if(orderedLinks[i].resultEnum in [ResultEnum.PREPENDTOROW]) {
                                return Chain.rearrange((([:] << it) << orderedLinks[i].input),orderedLinks[i].outputReorder)
                            }
                            return Chain.rearrange(it,orderedLinks[i].outputReorder)
                        }
                        break
                    case { it instanceof DefinedService }:
                        jobHistory.appendToLog("[DefinedService] Detected a Defined service for ${orderedLinks[i].rule.name}")                        
                        log.info "Detected a Defined Service ${orderedLinks[i].rule.name}" 
                        orderedLinks[i].rule.jobHistory = jobHistory
                        def gStringTemplateEngine = new GStringTemplateEngine()
                        def credentials = [
                            user: gStringTemplateEngine.createTemplate(orderedLinks[i].rule.user).make(getMergedGlobals().rcGlobals).toString(),
                            password: gStringTemplateEngine.createTemplate(orderedLinks[i].rule.password).make(getMergedGlobals().rcGlobals).toString()
                        ]
                        switch(orderedLinks[i].rule.authType) {
                            case AuthTypeEnum.CASSPRING:
                                jobHistory.appendToLog("[DefinedService] Detected a CASSPRING service") 
                                orderedLinks[i].output = linkService.casSpringSecurityRest(
                                    orderedLinks[i].rule.url,
                                    orderedLinks[i].rule.method.name(),
                                    orderedLinks[i].rule.parse,
                                    credentials.user,
                                    credentials.password,
                                    orderedLinks[i].rule.headers,
                                    { e ->
                                        switch(e) {
                                            case ExecuteEnum.EXECUTE_USING_ROW: 
                                                jobHistory.appendToLog("[DefinedService] Execute Using Row being used")
                                                jobHistory.appendToLog("[DefinedService] Unmodified input for Executing Row on link ${i} is ${orderedLinks[i].input as JSON}")
                                                jobHistory.appendToLog("[DefinedService] Modified input for Executing Row link ${i} is ${Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder) as JSON}")                                        
                                                return Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder)
                                                break
                                            default:
                                                return [:]
                                                break
                                        }                                        
                                    }.call(orderedLinks[i].executeEnum),
                                    orderedLinks[i].rule.springSecurityBaseURL
                                ).collect {
                                    if(orderedLinks[i].resultEnum in [ResultEnum.APPENDTOROW]) {
                                        return Chain.rearrange((([:] << orderedLinks[i].input) << it),orderedLinks[i].outputReorder)
                                    } else if(orderedLinks[i].resultEnum in [ResultEnum.PREPENDTOROW]) {
                                        return Chain.rearrange((([:] << it) << orderedLinks[i].input),orderedLinks[i].outputReorder)
                                    }
                                    return Chain.rearrange(it,orderedLinks[i].outputReorder)
                                }
                                break;
                            case AuthTypeEnum.CAS:
                                jobHistory.appendToLog("[DefinedService] Detected a CAS service") 
                                orderedLinks[i].output = linkService.casRest(
                                    orderedLinks[i].rule.url,
                                    orderedLinks[i].rule.method.name(),
                                    orderedLinks[i].rule.parse,
                                    credentials.user,
                                    credentials.password,
                                    orderedLinks[i].rule.headers,
                                    { e ->
                                        switch(e) {
                                            case ExecuteEnum.EXECUTE_USING_ROW: 
                                                jobHistory.appendToLog("[DefinedService] Execute Using Row being used")
                                                jobHistory.appendToLog("[DefinedService] Unmodified input for Executing Row on link ${i} is ${orderedLinks[i].input as JSON}")
                                                jobHistory.appendToLog("[DefinedService] Modified input for Executing Row link ${i} is ${Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder) as JSON}")                                        
                                                return Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder)
                                                break
                                            default:
                                                return [:]
                                                break
                                        }                                        
                                    }.call(orderedLinks[i].executeEnum)
                                ).collect {
                                    if(orderedLinks[i].resultEnum in [ResultEnum.APPENDTOROW]) {
                                        return Chain.rearrange((([:] << orderedLinks[i].input) << it),orderedLinks[i].outputReorder)
                                    } else if(orderedLinks[i].resultEnum in [ResultEnum.PREPENDTOROW]) {
                                        return Chain.rearrange((([:] << it) << orderedLinks[i].input),orderedLinks[i].outputReorder)
                                    }
                                    return Chain.rearrange(it,orderedLinks[i].outputReorder)
                                }                                
                                break;
                            case [AuthTypeEnum.BASIC,AuthTypeEnum.DIGEST,AuthTypeEnum.NONE]:
                                jobHistory.appendToLog("[DefinedService] Detected a REST service")                                 
                                orderedLinks[i].output = linkService.justRest(
                                    orderedLinks[i].rule.url,
                                    orderedLinks[i].rule.method,
                                    orderedLinks[i].rule.authType, 
                                    orderedLinks[i].rule.parse,
                                    credentials.user,
                                    credentials.password,
                                    orderedLinks[i].rule.headers,
                                    { e ->
                                        switch(e) {
                                            case ExecuteEnum.EXECUTE_USING_ROW: 
                                                jobHistory.appendToLog("[DefinedService] Execute Using Row being used")
                                                jobHistory.appendToLog("[DefinedService] Unmodified input for Executing Row on link ${i} is ${orderedLinks[i].input as JSON}")
                                                jobHistory.appendToLog("[DefinedService] Modified input for Executing Row link ${i} is ${Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder) as JSON}")                                        
                                                return Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder)
                                                break
                                            default:
                                                return [:]
                                                break
                                        }                                        
                                    }.call(orderedLinks[i].executeEnum)
                                ).collect {
                                    if(orderedLinks[i].resultEnum in [ResultEnum.APPENDTOROW]) {
                                        return Chain.rearrange((([:] << orderedLinks[i].input) << it),orderedLinks[i].outputReorder)
                                    } else if(orderedLinks[i].resultEnum in [ResultEnum.PREPENDTOROW]) {
                                        return Chain.rearrange((([:] << it) << orderedLinks[i].input),orderedLinks[i].outputReorder)
                                    }
                                    return Chain.rearrange(it,orderedLinks[i].outputReorder)
                                }
                                break;
                        }
                        break
                    case { it instanceof Snippet }:
                        jobHistory.appendToLog("[Snippet] Detected a Snippet for ${orderedLinks[i].rule.name}")                        
                        log.info "Detected a Snippet ${orderedLinks[i].rule.name}"   
                        orderedLinks[i].rule.chain.jobHistory = jobHistory
                        orderedLinks[i].output = orderedLinks[i].rule.chain.execute(
                            { e ->
                                switch(e) {
                                    case ExecuteEnum.EXECUTE_USING_ROW: 
                                        jobHistory.appendToLog("[Snippet] Execute Using Row being used")
                                        jobHistory.appendToLog("[Snippet] Unmodified input for Executing Row on link ${i} is ${orderedLinks[i].input as JSON}")
                                        jobHistory.appendToLog("[Snippet] Modified input for Executing Row link ${i} is ${Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder) as JSON}")                                        
                                        return Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder)
                                        break
                                    default:
                                        return [[:]]
                                        break
                                }                                        
                            }.call(orderedLinks[i].executeEnum)
                        ).collect {
                            if(orderedLinks[i].resultEnum in [ResultEnum.APPENDTOROW]) {
                                return Chain.rearrange((([:] << orderedLinks[i].input) << it),orderedLinks[i].outputReorder)
                            } else if(orderedLinks[i].resultEnum in [ResultEnum.PREPENDTOROW]) {
                                return Chain.rearrange((([:] << it) << orderedLinks[i].input),orderedLinks[i].outputReorder)
                            }
                            return Chain.rearrange(it,orderedLinks[i].outputReorder)
                        }
                        break                
                }
                // Handle result (aka: output)
                if((i+1) < orderedLinks.size() && orderedLinks[i].resultEnum in [ ResultEnum.ROW,ResultEnum.APPENDTOROW,ResultEnum.PREPENDTOROW ]) {
                    log.info "Setting the next output"
                    jobHistory.appendToLog("[NextInput] Setting the next input ${((orderedLinks[i].output)?orderedLinks[i].output.first():[:] as JSON)}")
                    orderedLinks[i+1].input = (orderedLinks[i].output)?orderedLinks[i].output.first():[:] 
                } else {
                    jobHistory.appendToLog("[NextInput] Not setting the next input for i=${i+1} and size ${orderedLinks.size()}")
                }
                // Handle link enum
                if((i+1) <= orderedLinks.size()) {
                    switch(orderedLinks[i].linkEnum) {
                        case [LinkEnum.NEXT]:
                            orderedLinks[i+1].input = Chain.rearrange(orderedLinks[i].input ,orderedLinks[i].outputReorder)
                            jobHistory.appendToLog("[Next] Carrying the current input to the following input ${orderedLinks[i+1].input as JSON}")
                            break
                        case [ LinkEnum.LOOP ]:
                            def endLoopIndex = Chain.findEndLoop(orderedLinks,i)
                            jobHistory.appendToLog("[LOOP] Detected a LOOP with End Loop Index ${endLoopIndex} starting at ${i+1}")
                            if(endLoopIndex != i) {
                                orderedLinks[endLoopIndex].output = execute(orderedLinks[i].output,orderedLinks[(i+1)..endLoopIndex])
                                i = endLoopIndex
                            }
                            jobHistory.appendToLog("[LOOP] is ended at i=${i}")
                            break
                    }
                }
            }
        }
        jobHistory.appendToLog("[END_EXECUTE] Chain ${name}")
        return (orderedLinks.isEmpty())?[[:]]:orderedLinks.last().output
    }
    /*
     * Static method to rearrange an input object
     * 
     * @param     row           A source object to be rearranged
     * @param     rearrange     A string containing groovy code which will handle the reordering
     * @return                  A rearranged result object
     */
    static def rearrange(def row,String rearrange){
        if(!!rearrange) {
            String toBeEvaluated = """
                import groovy.sql.Sql
                import oracle.jdbc.driver.OracleTypes
                
                rcGlobals
                row
                ${rearrange}
            """        
            try {
                return new GroovyShell(new Binding("row":row,rcGlobals: (Holders.config.rcGlobals)?Holders.config.rcGlobals:[:])).evaluate(toBeEvaluated)
            } catch(Exception e) {
                System.out.println("${row.toString()} error: ${e.message} on closure: ${toBeEvaluated}")
            }
        }
        return row
    }    
    /*
     * Static method to iterate through links to find the position of the end loop in the link sequence
     * 
     * @param     links     A list of links to be search for a cooresponding end loop position
     * @param     i         The current position of the start of the loop
     * @return              The position detected as the corresponding end loop
     */
    static def findEndLoop(List<Link> links,int i) {
        def endFound = false
        def endIndex = links.size()-1
        int loopCount = 1
        for( int l = (i+1) ; ( l < links.size() && !endFound) ; l++ ) {
            LinkEnum linkEnum = links[l].linkEnum
            switch(links[l].linkEnum) {
                case LinkEnum.LOOP:
                    loopCount++
                    break
                case LinkEnum.ENDLOOP:
                    loopCount--
                    if(!loopCount) {
                        endIndex = l
                        endFound = true
                    }
                    break
            }
        }                
        return endIndex
    }    
}
