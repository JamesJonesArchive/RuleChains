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
    def jobInfo = [:]
    static hasMany = [links:Link]
    static fetchMode = [links: 'eager']
    static transients = ['orderedLinks','input','output','jobInfo','isSynced','mergedGlobals']
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
        println "I'm running ${jobInfo}"
        log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][START_EXECUTE] Chain ${jobInfo.chain}:${jobInfo.suffix}"
        log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][SUMMARY] JobInfo ${jobInfo as JSON}"
        if(!!!orderedLinks) {
            orderedLinks = getOrderedLinks()
        }
        
        def linkService = new LinkService()
        linkService.metaClass.getJobInfo {->
            return jobInfo + [ name: name ]
        }
        ((!!input)?input:[[:]]).each { row ->
            /**
             * Pre-populate input based on incoming data array
             */
            log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][INFO] Made it this far with ${row}"
            // println "Made it this far with ${row}"
            for(int i = 0; i < orderedLinks.size(); i++) {
                orderedLinks[i].input = row
            }
            /**
             * Distinguish what kind of "rules" and handle them by type
             **/
            for(int i = 0; i < orderedLinks.size(); i++) {
                log.info("[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][INFO] Unmodified input for link ${i} is ${orderedLinks[i].input as JSON}")
                // Execute the rule based on it's type
                log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][INFO] Modified rearranged input for link ${i} is ${Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder) as JSON}"
                switch(orderedLinks[i].rule) {
                case { it instanceof SQLQuery }:
                    log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][SQLQuery] Detected a SQLQuery for ${orderedLinks[i].rule.name}"
                    orderedLinks[i].output = linkService.justSQL(
                        { p ->
                            def gStringTemplateEngine = new GStringTemplateEngine()
                            def rule = [:]
                            rule << p
                            rule << [
                                rule: gStringTemplateEngine.createTemplate(rule.rule).make(getMergedGlobals(Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder))).toString(),
                                jobInfo: jobInfo
                            ]
                            log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][SQLQuery] Untemplated Rule is: ${p.rule}"
                            log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][SQLQuery] Unmodified input for Templating Rule on link ${i} is ${orderedLinks[i].input as JSON}"
                            log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][SQLQuery] Modified input for Templating Rule on link ${i} is ${Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder) as JSON}"
                            log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][SQLQuery] Templated Rule is: ${rule.rule}"
                            return rule
                        }.call(orderedLinks[i].rule.properties),
                        orderedLinks[i].sourceName,
                        orderedLinks[i].executeEnum,    
                        orderedLinks[i].resultEnum,
                        { e ->
                            switch(e) {
                            case ExecuteEnum.EXECUTE_USING_ROW: 
                                log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][SQLQuery] Execute Using Row being used"
                                log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][SQLQuery] Unmodified input for Executing Row on link ${i} is ${orderedLinks[i].input as JSON}"
                                log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][SQLQuery] Modified input for Executing Row link ${i} is ${Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder) as JSON}"
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
                    log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][StoredProcedureQuery] Detected a StoredProcedureQuery Script for ${orderedLinks[i].rule.name}"
                    orderedLinks[i].output = linkService.justStoredProcedure(
                        { p ->
                            def gStringTemplateEngine = new GStringTemplateEngine()
                            def rule = [:]
                            rule << p
                            rule << [
                                rule: gStringTemplateEngine.createTemplate(rule.rule).make(getMergedGlobals(Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder))).toString(),
                                jobInfo: jobInfo
                            ]
                            log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][StoredProcedureQuery] Untemplated Rule is: ${p.rule}"
                            log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][StoredProcedureQuery] Unmodified input for Templating Rule on link ${i} is ${orderedLinks[i].input as JSON}"
                            log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][StoredProcedureQuery] Modified input for Templating Rule on link ${i} is ${Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder) as JSON}"
                            log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][StoredProcedureQuery] Templated Rule is: ${rule.rule}"
                            println rule.rule
                            return rule
                        }.call(orderedLinks[i].rule.properties),
                        orderedLinks[i].sourceName,
                        orderedLinks[i].executeEnum,    
                        orderedLinks[i].resultEnum,
                        { e ->
                            switch(e) {
                            case ExecuteEnum.EXECUTE_USING_ROW: 
                                log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][StoredProcedureQuery] Execute Using Row being used"
                                log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][StoredProcedureQuery] Unmodified input for Executing Row on link ${i} is ${orderedLinks[i].input as JSON}"
                                log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][StoredProcedureQuery] Modified input for Executing Row link ${i} is ${Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder) as JSON}"
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
                    log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][Groovy] Detected a Groovy Script for ${orderedLinks[i].rule.name}"
                    orderedLinks[i].rule.jobInfo = jobInfo
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
                            log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][Groovy] Object needs to be an array of objects so wrapping it as an array like this ${[r] as JSON}"
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
                                log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][Groovy] Execute Using Row being used"
                                log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][Groovy] Unmodified input for Executing Row on link ${i} is ${orderedLinks[i].input as JSON}"
                                log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][Groovy] Modified input for Executing Row link ${i} is ${Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder) as JSON}"
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
                    log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][Python] Detected a Python Script for ${orderedLinks[i].rule.name}"
                    orderedLinks[i].rule.jobInfo = jobInfo
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
                            log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][Python] Object needs to be an array of objects so wrapping it as an array like this ${[r] as JSON}"
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
                                log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][Python] Execute Using Row being used"
                                log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][Python] Unmodified input for Executing Row on link ${i} is ${orderedLinks[i].input as JSON}"
                                log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][Python] Modified input for Executing Row link ${i} is ${Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder) as JSON}"
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
                    log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][Ruby] Detected a Ruby Script for ${orderedLinks[i].rule.name}"
                    orderedLinks[i].rule.jobInfo = jobInfo
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
                            log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][Ruby] Object needs to be an array of objects so wrapping it as an array like this ${[r] as JSON}"
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
                                    log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][Ruby] Execute Using Row being used"
                                    log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][Ruby] Unmodified input for Executing Row on link ${i} is ${orderedLinks[i].input as JSON}"
                                    log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][Ruby] Modified input for Executing Row link ${i} is ${Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder) as JSON}"
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
                    log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][PHP] Detected a PHP Script for ${orderedLinks[i].rule.name}"
                    orderedLinks[i].rule.jobInfo = jobInfo
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
                            log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][PHP] Object needs to be an array of objects so wrapping it as an array like this ${[r] as JSON}"
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
                                    log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][PHP] Execute Using Row being used"
                                    log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][PHP] Unmodified input for Executing Row on link ${i} is ${orderedLinks[i].input as JSON}"
                                    log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][PHP] Modified input for Executing Row link ${i} is ${Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder) as JSON}"
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
                    log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][DefinedService] Detected a Defined Service ${orderedLinks[i].rule.name}" 
                    orderedLinks[i].rule.jobInfo = jobInfo
                    def gStringTemplateEngine = new GStringTemplateEngine()
                    def credentials = [
                        user: gStringTemplateEngine.createTemplate(orderedLinks[i].rule.user).make(getMergedGlobals().rcGlobals).toString(),
                        password: gStringTemplateEngine.createTemplate(orderedLinks[i].rule.password).make(getMergedGlobals().rcGlobals).toString()
                    ]
                    switch(orderedLinks[i].rule.authType) {
                    case AuthTypeEnum.CASSPRING:
                        log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][DefinedService][CASSPRING] Detected a CASSPRING service"
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
                                    log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][DefinedService][CASSPRING] Execute Using Row being used"
                                    log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][DefinedService][CASSPRING] Unmodified input for Executing Row on link ${i} is ${orderedLinks[i].input as JSON}"
                                    log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][DefinedService][CASSPRING] Modified input for Executing Row link ${i} is ${Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder) as JSON}"
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
                        log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][DefinedService][CAS] Detected a CAS service"
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
                                    log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][DefinedService][CAS] Execute Using Row being used"
                                    log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][DefinedService][CAS] Unmodified input for Executing Row on link ${i} is ${orderedLinks[i].input as JSON}"
                                    log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][DefinedService][CAS] Modified input for Executing Row link ${i} is ${Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder) as JSON}"
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
                        log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][DefinedService][REST] Detected a REST service"
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
                                    log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][DefinedService][REST] Execute Using Row being used"
                                    log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][DefinedService][REST] Unmodified input for Executing Row on link ${i} is ${orderedLinks[i].input as JSON}"
                                    log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][DefinedService][REST] Modified input for Executing Row link ${i} is ${Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder) as JSON}"
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
                    log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][Snippet] Detected a Snippet for ${orderedLinks[i].rule.name}"
                    orderedLinks[i].rule.chain.jobInfo = jobInfo
                    orderedLinks[i].output = orderedLinks[i].rule.chain.execute(
                        { e ->
                            switch(e) {
                            case ExecuteEnum.EXECUTE_USING_ROW: 
                                log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][Snippet] Execute Using Row being used"
                                log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][Snippet] Unmodified input for Executing Row on link ${i} is ${orderedLinks[i].input as JSON}"
                                log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][Snippet] Modified input for Executing Row link ${i} is ${Chain.rearrange(orderedLinks[i].input,orderedLinks[i].inputReorder) as JSON}"
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
                    log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][NextInput] Setting the next input ${((orderedLinks[i].output)?orderedLinks[i].output.first():[:] as JSON)}"
                    orderedLinks[i+1].input = (orderedLinks[i].output)?orderedLinks[i].output.first():[:] 
                } else {
                    log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][NextInput] Not setting the next input for i=${i+1} and size ${orderedLinks.size()}"
                }
                // Handle link enum
                if((i+1) <= orderedLinks.size()) {
                    switch(orderedLinks[i].linkEnum) {
                    case [LinkEnum.NEXT]:
                        orderedLinks[i+1].input = Chain.rearrange(orderedLinks[i].input ,orderedLinks[i].outputReorder)
                        log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][Next] Carrying the current input to the following input ${orderedLinks[i+1].input as JSON}"
                        break
                    case [ LinkEnum.LOOP ]:
                        def endLoopIndex = Chain.findEndLoop(orderedLinks,i)
                        log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][LOOP] Detected a LOOP with End Loop Index ${endLoopIndex} starting at ${i+1}"
                        if(endLoopIndex != i) {
                            orderedLinks[endLoopIndex].output = execute(orderedLinks[i].output,orderedLinks[(i+1)..endLoopIndex])
                            i = endLoopIndex
                        }
                        log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][LOOP] Loop is ended at i=${i}"
                        break
                    }
                }
            }
        }
        log.info "[Chain:${jobInfo.chain}:${jobInfo.suffix}][${name}][END_EXECUTE] Chain ${name}"
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
                return new GroovyShell(new Binding("row":row,rcGlobals: (Holders.config.ruleChains.globals)?Holders.config.ruleChains.globals:[:])).evaluate(toBeEvaluated)
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
