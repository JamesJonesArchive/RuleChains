package edu.usf.RuleChains

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import edu.usf.RuleChains.ServiceTypeEnum
import edu.usf.RuleChains.MethodEnum
import edu.usf.RuleChains.ParseEnum
import edu.usf.RuleChains.AuthTypeEnum
import edu.usf.RuleChains.Rule
import grails.converters.*
import org.hibernate.criterion.CriteriaSpecification
import grails.util.GrailsUtil
/**
 * RuleSetService provide for the creation and manipulation of RuleSet and Rule objects
 * <p>
 * Developed originally for the University of South Florida
 * 
 * @author <a href='mailto:james@mail.usf.edu'>James Jones</a> 
 */ 
class RuleSetService {
    static transactional = true
    /**
     * Returns a list of RuleSet objects objects with an option matching filter
     * 
     * @param  pattern  An optional parameter. When provided the full list (default) will be filtered down with the regex pattern string when provided
     * @return          An object containing the resulting list of RuleSet objects
     * @see    RuleSet
     */    
    def listRuleSets(String pattern = null) { 
        if(!!pattern) {
            return [ruleSets: RuleSet.list().findAll() {
                    Pattern.compile(pattern.trim()).matcher(it.name).matches()
                }.collect { rs ->
                    return getRuleSet(rs.name).ruleSet
                }]
        } else {
            return [ ruleSets: RuleSet.list().collect { rs -> return getRuleSet(rs.name).ruleSet } ]
        }
    }
    /**
     * Creates a new RuleSet
     * 
     * @param  name      The unique name of the new RuleSet
     * @param  isSynced  An optional parameter for syncing to Git. The default value is 'true' keeping sync turned on
     * @return           Returns an object containing the new RuleSet
     */    
    def addRuleSet(String name,boolean isSynced = true) {
        if(!!name) {
            def ruleSet = [ name: name.trim() ] as RuleSet
            ruleSet.isSynced = isSynced
            if(!ruleSet.save(failOnError:false, flush: true, insert: true, validate: true)) {
                return [ error : "'${ruleSet.errors.fieldError.field}' value '${ruleSet.errors.fieldError.rejectedValue}' rejected" ]
            } else {
                return [ ruleSet: getRuleSet(ruleSet.name).ruleSet ]
            }
        }
        return [ error: "You must supply a name" ]
    }
    /**
     * Finds a RuleSet by it's name
     * 
     * @param  name  The unique name of the RuleSet
     * @return       Returns a RuleSet if matched or returns an error message
     * @see    RuleSet
     */
    def getRuleSet(String name) {
        if(!!name) {
            def ruleSet = RuleSet.findByName(name.trim())
            if(!!ruleSet) {
                return [ ruleSet: ruleSet.properties.inject([id:ruleSet.id]) { rs,k,v -> 
                        switch(k) {
                            case ['id','name']:
                                rs[k] = v
                                return rs
                                break
                            case 'rules':
                                rs[k] = (!v)?[]:Rule.createCriteria().list(sort: 'name',order: 'asc') {
                                    eq('ruleSet',ruleSet)
                                    resultTransformer(CriteriaSpecification.ALIAS_TO_ENTITY_MAP)
                                    projections {
                                        property('name', 'name')
                                        property('rule', 'rule')
                                        property('id','id')
                                        property('class', 'class')
                                    }                        
                                }.collect { r ->
                                    return getRule(name.trim(),r.name).rule
                                }   
                                return rs
                                break
                            default:
                                return rs
                                break
                        }
                    } ]
            }
            return [ error : "RuleSet named ${name} not found!"]
        }
        return [ error : "You must supply a name for the target ruleSet"]
    }
    /**
     * Removes an existing RuleSet by name
     * 
     * @param  name      The name of the RuleSet to be removed
     * @param  isSynced  An optional parameter for syncing to Git. The default value is 'true' keeping sync turned on
     * @return           Returns an object containing the sucess or error message
     */    
    def deleteRuleSet(String name,boolean isSynced = true) {
        if(!!name) {
            def ruleSet = RuleSet.findByName(name.trim())
            if(!!ruleSet) {
                ruleSet.isSynced = isSynced
                ruleSet.delete()
                return [ success : "RuleSet deleted" ]
            }
            return [ error : "RuleSet named ${name} not found!"]
        }
        return [ error : "You must supply a name for the target ruleSet"]
    }
    /**
     * Renames an existing RuleSet
     * 
     * @param  name                              The name of the RuleSet to be updated
     * @param  newName                           The new name of the RuleSet to be updated
     * @return                                   Returns an object containing the updated RuleSet
     */
    def modifyRuleSet(String name,String newName,boolean isSynced = true) {
        if(!!name && !!newName) {
            def ruleSet = RuleSet.findByName(name.trim())
            if(!!ruleSet) {
                System.out.println(newName)
                ruleSet.name = newName.trim()
                ruleSet.isSynced = isSynced
                if(!ruleSet.save(failOnError:false, flush: true, validate: true)) {
                    return [ error : "Name value '${ruleSet.errors.fieldError.rejectedValue}' rejected" ]
                } else {
                    return [ ruleSet: getRuleSet(ruleSet.name).ruleSet ]
                }
            }
            return [ error : "RuleSet named ${name} not found!"]
        }
        return [ error : "You must supply a name and new name for the target ruleSet"]
    }
    /**
     * Creates a new Rule in an existing RuleSet
     * 
     * @param  ruleSetName     The unique name of the RuleSet
     * @param  name            The unique name of the Rule
     * @param  serviceType     The string value of the enumerator type of rule
     * @param  isSynced        An optional parameter for syncing to Git. The default value is 'true' keeping sync turned on
     * @return                 Returns an object containing the newly created Rule
     * @see Rule
     * @see RuleSet
     */
    def addRule(String ruleSetName,String name,String serviceType,boolean isSynced = true) {        
        if(!!name && !!ruleSetName && !!serviceType) {
            def ruleSet = RuleSet.findByName(ruleSetName)
            if(!!ruleSet) {
                ruleSet.isSynced = isSynced
                def serviceTypeEnum = ServiceTypeEnum.byName(serviceType.trim())
                def rule
                switch(serviceTypeEnum) {
                case ServiceTypeEnum.SQLQUERY:
                    rule = [ name: name, rule: "" ] as SQLQuery
                    rule.isSynced = isSynced
                    break
                case ServiceTypeEnum.GROOVY:
                    rule = [ name: name, rule: "" ] as Groovy
                    rule.isSynced = isSynced
                    break
                case ServiceTypeEnum.PYTHON:
                    rule = [ name: name ] as Python
                    rule.isSynced = isSynced
                    break
                case ServiceTypeEnum.RUBY:
                    rule = [ name: name ] as Ruby
                    rule.isSynced = isSynced
                    break
                case ServiceTypeEnum.STOREDPROCEDUREQUERY:
                    rule = [ name: name ] as StoredProcedureQuery
                    rule.isSynced = isSynced
                    break
                case ServiceTypeEnum.DEFINEDSERVICE:
                    rule = [ name: name ] as DefinedService
                    rule.isSynced = isSynced                    
                    break
                case ServiceTypeEnum.SNIPPET:
                    def chain = Chain.findByName(name)
                    /** TODO
                     * Removed temporarily until the Chain interface is in place
                     **/
                    if(!chain) {
                        return [ error: "Chain '${name}' does not exist! You must specify a name for an existing chain to reference it as a snippet."]
                    }
                    rule = [ name: name, chain: chain ] as Snippet
                    rule.isSynced = isSynced                    
                    break
                }
                try {
                    if(!ruleSet.addToRules(rule).save(failOnError:false, flush: false, validate: true)) {
                        ruleSet.errors.allErrors.each {
                            println it
                        }           
                        return [ error : "RuleSet '${ruleSet.errors.fieldError.field}' value '${ruleSet.errors.fieldError.rejectedValue}' rejected" ]
                    } else {
                        return [ rule: (GrailsUtil.environment in ['test'])?rule:getRule(ruleSetName,rule.name).rule ]
                    }                    
                } catch(Exception ex) {
                    rule.errors.allErrors.each {
                        println it
                    }           
                    return [ error: "Rule '${rule.errors.fieldError?.field}' value '${rule.errors.fieldError?.rejectedValue}' rejected" ]
                }
            } else {
                return [ error: "Rule Set specified does not exist!" ]
            }
        }
        return [ error: "You must supply a rule set name, rule name and a service type"]
    }
    /**
     * Updates an existing rule in a RuleSet
     * 
     * @param     ruleSetName     The unique name of the RuleSet
     * @param     name            The unique name of the Rule
     * @param     ruleUpdate      An object containing updated parameters for the rule
     * @param     isSynced        An optional parameter for syncing to Git. The default value is 'true' keeping sync turned on
     * @return                    Returns an object containing the updete Rule
     */
    def updateRule(String ruleSetName,String name,def ruleUpdate,boolean isSynced = true) {
        if(!!name && !!ruleSetName && !!ruleUpdate) {
            def ruleSet = RuleSet.findByName(ruleSetName)
            if(!!ruleSet) {
                def rule = Rule.findByRuleSetAndName(ruleSet,name)
                if(!!rule) {
                    rule.isSynced = isSynced
                    if("chain" in ruleUpdate) {
                        ruleUpdate.chain = ("name" in ruleUpdate.chain)?Chain.findByName(ruleUpdate.chain.name):Chain.get(ruleUpdate.chain.id)
                        ruleUpdate.name = ruleUpdate.chain.name
                    } else if("method" in ruleUpdate) {
                        ruleUpdate.method = MethodEnum.byName(("name" in ruleUpdate.method)?ruleUpdate.method.name:ruleUpdate.method)
                        ruleUpdate.parse = ParseEnum.byName(("name" in ruleUpdate.parse)?ruleUpdate.parse.name:ruleUpdate.parse)
                        ruleUpdate.authType = AuthTypeEnum.byName(("name" in ruleUpdate.authType)?ruleUpdate.authType.name:ruleUpdate.authType)
                    }
                    rule.properties = ruleUpdate
                    if(!rule.save(failOnError:false, flush: true, validate: true)) {
                        rule.errors.allErrors.each {
                            println it
                        }
                        return [ error: "'${rule.errors.fieldError.field}' value '${rule.errors.fieldError.rejectedValue}' rejected" ]                        
                    }
                    return [ rule: (GrailsUtil.environment in ['test'])?rule:getRule(ruleSetName,rule.name).rule ]
                }
                return [ error: "Rule specified does not exist!" ]
            }
            return [ error: "Rule Set specified does not exist!" ]
        }
        return [ error: "You must supply a rule set name, rule name and the updated rule"]
    }
    /**
     * Renames an existing Rule
     * 
     * @param  ruleSetName                       The name of the RuleSet
     * @param  name                              The name of the Rule to be updated
     * @param  nameUpdate                        The new name of the Rule being updated
     * @return                                   Returns an object containing the updated Rule
     */
    def updateRuleName(String ruleSetName,String name,String nameUpdate,boolean isSynced = true) {
        if(!!name && !!ruleSetName && !!nameUpdate) {
            def ruleSet = RuleSet.findByName(ruleSetName)
            if(!!ruleSet) {
                def rule = Rule.findByRuleSetAndName(ruleSet,name)
                if(!!rule) {
                    rule.name = nameUpdate
                    rule.isSynced = isSynced
                    if(!rule.save(failOnError:false, flush: true, validate: true)) {
                        rule.errors.allErrors.each {
                            println it
                        }           
                        return [ error: "'${rule.errors.fieldError.field}' value '${rule.errors.fieldError.rejectedValue}' rejected" ]                        
                    }
                    return [ rule: (GrailsUtil.environment in ['test'])?rule:getRule(ruleSetName,rule.name).rule ]                    
                }
                return [ error: "Rule specified does not exist!" ]
            }
            return [ error: "Rule Set specified does not exist!" ]
        }
        return [ error: "You must supply a rule set name, rule name and the updated rule name"]
    }
    /**
     * Retrieves a Rule by it's RuleSet name and Rule name
     * 
     * @param  ruleSetName     The unique name of the RuleSet
     * @param  name            The unique name of the Rule
     * @return                 Returns a Rule if matched or returns an error message
     * @see    Rule
     */    
    def getRule(String ruleSetName,String name) {
        if(!!name && !!ruleSetName) {
            def ruleSet = RuleSet.findByName(ruleSetName)
            if(!!ruleSet) {
                def rule = ruleSet.rules.find {
                    it.name == name
                }
                if(!!rule) {
                    return [ rule: { r ->
                            switch(r) {
                            case { it instanceof SQLQuery }:
                                return (SQLQuery.createCriteria().get {
                                        eq('name',r.name)
                                        resultTransformer(CriteriaSpecification.ALIAS_TO_ENTITY_MAP)
                                        projections {
                                            property('name', 'name')
                                            property('rule', 'rule')
                                            property('id','id')
                                            property('class', 'class')
                                        }                        
                                    } as Map).inject([ruleSet:ruleSetName]) { ds,k,v ->
                                    ds[k] = v
                                    return ds                                    
                                }
                                break
                            case { it instanceof Groovy }:
                                return (Groovy.createCriteria().get {
                                        eq('name',r.name)
                                        resultTransformer(CriteriaSpecification.ALIAS_TO_ENTITY_MAP)
                                        projections {
                                            property('name', 'name')
                                            property('rule', 'rule')
                                            property('id','id')
                                            property('class', 'class')
                                        }                        
                                    } as Map).inject([ruleSet:ruleSetName]) { ds,k,v ->
                                    ds[k] = v
                                    return ds                                    
                                }
                                break
                            case { it instanceof Python }:
                                return (Python.createCriteria().get {
                                        eq('name',r.name)
                                        resultTransformer(CriteriaSpecification.ALIAS_TO_ENTITY_MAP)
                                        projections {
                                            property('name', 'name')
                                            property('rule', 'rule')
                                            property('id','id')
                                            property('class', 'class')
                                        }                        
                                    } as Map).inject([ruleSet:ruleSetName]) { ds,k,v ->
                                    ds[k] = v
                                    return ds                                    
                                }
                                break
                            case { it instanceof Ruby }:
                                return (Ruby.createCriteria().get {
                                        eq('name',r.name)
                                        resultTransformer(CriteriaSpecification.ALIAS_TO_ENTITY_MAP)
                                        projections {
                                            property('name', 'name')
                                            property('rule', 'rule')
                                            property('id','id')
                                            property('class', 'class')
                                        }                        
                                    } as Map).inject([ruleSet:ruleSetName]) { ds,k,v ->
                                    ds[k] = v
                                    return ds                                    
                                }
                                break
                            case { it instanceof StoredProcedureQuery }:
                                return (StoredProcedureQuery.createCriteria().get {
                                        eq('name',r.name)
                                        resultTransformer(CriteriaSpecification.ALIAS_TO_ENTITY_MAP)
                                        projections {
                                            property('name', 'name')
                                            property('rule', 'rule')
                                            property('id','id')
                                            property('class', 'class')
                                            property('closure', 'closure')
                                        }                        
                                    } as Map).inject([ruleSet:ruleSetName]) { ds,k,v ->
                                    ds[k] = v
                                    return ds                                    
                                }
                                break
                            case { it instanceof DefinedService }:                            
                                return (DefinedService.createCriteria().get {
                                        eq('name',r.name)
                                        resultTransformer(CriteriaSpecification.ALIAS_TO_ENTITY_MAP)
                                        projections {
                                            property('name', 'name')
                                            property('id','id')
                                            property('class', 'class')
                                            property('method','method')
                                            property('authType', 'authType')
                                            property('parse', 'parse')
                                            property('url', 'url')
                                            property('springSecurityBaseURL', 'springSecurityBaseURL')
                                            property('user', 'user')
                                            property('password', 'password')
                                        }                        
                                    } as Map).inject([ruleSet:ruleSetName,headers: (r as DefinedService).headers]) { ds,k,v ->
                                    ds[k] = v
                                    return ds
                                }
                                break
                            case { it instanceof Snippet }:
                                return (Snippet.createCriteria().get {
                                        eq('name',r.name)
                                        resultTransformer(CriteriaSpecification.ALIAS_TO_ENTITY_MAP)
                                        projections {
                                            property('name', 'name')
                                            property('id','id')
                                            property('class', 'class')
                                            property('chain', 'chain.name')
                                        }                        
                                    } as Map).inject([ruleSet: ruleSetName]) { ds,k,v ->
                                    ds[k] = v
                                    return ds                                    
                                }
                                break
                            }
                        }.call(rule) ]
                }
                return [ error: "Rule specified does not exist!" ]
            }
            return [ error: "Rule Set specified does not exist!" ]
        }
        return [ error: "You must supply a rule set name and rule name"]
    }
    /**
     * Removes an existing Rule by RuleSet name and Rule name. 
     * 
     * @param  ruleSetName     The unique name of the RuleSet
     * @param  name            The name of the Rule to be removed
     * @param  isSynced        An optional parameter for syncing to Git. The default value is 'true' keeping sync turned on
     * @return                 Returns an object containing the sucess or error message
     */    
    def deleteRule(String ruleSetName,String name,boolean isSynced = true) {
        if(!!name && !!ruleSetName) {
            def ruleSet = RuleSet.findByName(ruleSetName)
            if(!!ruleSet) {
                ruleSet.isSynced = isSynced
                def rule = ruleSet.rules.find {
                    it.name == name
                }
                if(!!rule) {
                    rule.isSynced = isSynced
                    // See if this rule is in any chains
                    def chainNames = Link.findAllByRule(rule).collect { l ->
                        l.chain.name
                    }.unique().join(',')
                    if(!chainNames) {
                        try {
                            if(!ruleSet.removeFromRules(rule).save(failOnError:false, flush: false, validate: true)) {
                                ruleSet.errors.allErrors.each {
                                    println it
                                }           
                                ruleSet.refresh()
                                return [ error : "'${ruleSet.errors.fieldError.field}' value '${ruleSet.errors.fieldError.rejectedValue}' rejected" ]
                            } else {
                                rule.delete()
                                return [ status: "Rule Removed From Set" ] 
                            }                    
                        } catch(Exception ex) {
                            rule.errors.allErrors.each {
                                println it
                            }           
                            return [ error: "'${rule.errors.fieldError.field}' value '${rule.errors.fieldError.rejectedValue}' rejected" ]
                        }
                    }
                    return [ error: "Rule cannot be removed until it's been unlinked in the following chains: ${chainNames}"]
                }
                return [ error: "Rule specified does not exist!" ]
            }
            return [ error: "Rule Set specified does not exist!" ]
        }
        return [ error: "You must supply a rule set name and rule name"]
    }
    /**
     * Relocates an existing Rule in a different RuleSet
     *
     * @param  ruleSetName     The unique name of the source RuleSet
     * @param  name            The name of the Rule to be removed
     * @param  nameUpdate      The unique name of the target RuleSet
     * @return                 Returns moved Rule if successful or returns an error message
     */
    def moveRule(String ruleSetName,String name,String nameUpdate,boolean isSynced = true) {
        if(!!name && !!ruleSetName && !!nameUpdate) {
            def ruleSet = RuleSet.findByName(ruleSetName)
            ruleSet.isSynced = isSynced
            if(!!ruleSet) {
                def rule = Rule.findByRuleSetAndName(ruleSet,name)
                if(!!rule) {
                    rule.isSynced = isSynced
                    def targetRuleSet = RuleSet.findByName(nameUpdate)
                    targetRuleSet.isSynced = isSynced
                    if(!!targetRuleSet) {
                        try {
                            if(!ruleSet.removeFromRules(rule).save(failOnError:false, flush: false, validate: true)) {
                                ruleSet.errors.allErrors.each {
                                    println it
                                }           
                                return [ error : "'${ruleSet.errors.fieldError.field}' value '${ruleSet.errors.fieldError.rejectedValue}' rejected" ]
                            } else {
                                try {
                                    if(!targetRuleSet.addToRules(rule).save(failOnError:false, flush: false, validate: true)) {
                                        targetRuleSet.errors.allErrors.each {
                                            println it
                                        }           
                                        return [ error : "'${targetRuleSet.errors.fieldError.field}' value '${targetRuleSet.errors.fieldError.rejectedValue}' rejected" ]
                                    } else {
                                        return [ rule: (GrailsUtil.environment in ['test'])?rule:getRule(nameUpdate,rule.name).rule ]
                                    }                    
                                } catch(Exception ex) {
                                    rule.errors.allErrors.each {
                                        println it
                                    }           
                                    return [ error: "'${rule.errors.fieldError.field}' value '${rule.errors.fieldError.rejectedValue}' rejected" ]
                                }
                            }                    
                        } catch(Exception ex) {
                            rule.errors.allErrors.each {
                                println it
                            }           
                            return [ error: "'${rule.errors.fieldError.field}' value '${rule.errors.fieldError.rejectedValue}' rejected" ]
                        }
                    }
                    return [ error: "Target Rule Set specified does not exist!" ]
                }
                return [ error: "Rule specified does not exist!" ]
            }
            return [ error: "Rule Set specified does not exist!" ]
        }
        return [ error: "You must supply a rule set name, rule name and the target rule set name"]
    }
}
