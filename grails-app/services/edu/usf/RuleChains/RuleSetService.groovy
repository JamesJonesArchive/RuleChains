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

class RuleSetService {
    static transactional = true

    def listRuleSets(String pattern = null) { 
        if(!!pattern) {
            return [ruleSets: RuleSet.list().findAll() {
                Pattern.compile(pattern.trim()).matcher(it.name).matches()
            }]
        } else {
            return [ ruleSets: RuleSet.list() ]
        }
    }
    def addRuleSet(String name) {
        if(!!name) {
            def ruleSet = [ name: name.trim() ] as RuleSet
            if(!ruleSet.save(failOnError:false, flush: true, insert: true, validate: true)) {
                return [ error : "Name value '${ruleSet.errors.fieldError.rejectedValue}' rejected" ]
            } else {
                return [ ruleSet: ruleSet ]
            }
        }
        return [ error: "You must supply a name" ]
    }
    def getRuleSet(String name) {
        if(!!name) {
            def ruleSet = RuleSet.findByName(name.trim())
            if(!!ruleSet) {
                def resultSet = [:]
                resultSet << ruleSet.properties
                if(!!!!resultSet.rules) {
                    resultSet.rules = Rule.createCriteria().list(sort: 'name',order: 'asc') {
                        eq('ruleSet',ruleSet)
                        resultTransformer(CriteriaSpecification.ALIAS_TO_ENTITY_MAP)
                        projections {
                            property('name', 'name')
                            property('rule', 'rule')
                            property('id','id')
                            property('class', 'class')
                        }                        
                    }
                }
                return [ ruleSet: resultSet ]                    
            }
            return [ error : "RuleSet named ${name} not found!"]
        }
        return [ error : "You must supply a name for the target ruleSet"]
    }
    def deleteRuleSet(String name) {
        if(!!name) {
            def ruleSet = RuleSet.findByName(name.trim())
            if(!!ruleSet) {
                ruleSet.delete()
                return [ success : "RuleSet deleted" ]
            }
            return [ error : "RuleSet named ${name} not found!"]
        }
        return [ error : "You must supply a name for the target ruleSet"]
    }
    def modifyRuleSet(String name,String newName) {
        if(!!name && !!newName) {
            def ruleSet = RuleSet.findByName(name.trim())
            if(!!ruleSet) {
                System.out.println(newName)
                ruleSet.name = newName.trim()
                if(!ruleSet.save(failOnError:false, flush: true, validate: true)) {
                    return [ error : "Name value '${ruleSet.errors.fieldError.rejectedValue}' rejected" ]
                } else {
                    return [ ruleSet: ruleSet ]
                }
            }
            return [ error : "RuleSet named ${name} not found!"]
        }
        return [ error : "You must supply a name and new name for the target ruleSet"]
    }
    def addRule(String ruleSetName,String name,String serviceType) {        
        if(!!name && !!ruleSetName && !!serviceType) {
            def ruleSet = RuleSet.findByName(ruleSetName)
            if(!!ruleSet) {
                System.out.println(serviceType)
                System.out.println(name)
                def serviceTypeEnum = ServiceTypeEnum.byName(serviceType.trim())
                def rule
                switch(serviceTypeEnum) {
                    case ServiceTypeEnum.SQLQUERY:
                        rule = [ name: name, rule: "" ] as SQLQuery
                        break
                    case ServiceTypeEnum.GROOVY:
                        rule = [ name: name, rule: "" ] as Groovy
                        break
                    case ServiceTypeEnum.STOREDPROCEDUREQUERY:
                        rule = [ name: name ] as StoredProcedureQuery
                        break
                    case ServiceTypeEnum.DEFINEDSERVICE:
                        rule = [ name: name ] as DefinedService
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
                        break
                }
                System.out.println(rule.name)
                try {
                    if(!ruleSet.addToRules(rule).save(failOnError:false, flush: true, validate: true)) {
                        ruleSet.errors.allErrors.each {
                            println it
                        }           
                        return [ error : "'${ruleSet.errors.fieldError.field}' value '${ruleSet.errors.fieldError.rejectedValue}' rejected" ]
                    } else {
                        return [ rule: rule ]
                    }                    
                } catch(Exception ex) {
                    rule.errors.allErrors.each {
                        println it
                    }           
                    return [ error: "'${rule.errors.fieldError.field}' value '${rule.errors.fieldError.rejectedValue}' rejected" ]
                }
            } else {
                return [ error: "Rule Set specified does not exist!" ]
            }
        }
        return [ error: "You must supply a rule set name, rule name and a service type"]
    }
    def updateRule(String ruleSetName,String name,def ruleUpdate) {
        if(!!name && !!ruleSetName && !!ruleUpdate) {
            def ruleSet = RuleSet.findByName(ruleSetName)
            if(!!ruleSet) {
                def rule = ruleSet.rules.collect { r ->
                    def er
                    switch(r) {
                        case { it instanceof SQLQuery }:
                            er = r as SQLQuery
                            break
                        case { it instanceof Groovy }:
                            er = r as Groovy
                            break
                        case { it instanceof PHP }:
                            er = r as PHP
                            break
                        case { it instanceof StoredProcedureQuery }:
                            er = r as StoredProcedureQuery
                            break
                        case { it instanceof DefinedService }:                            
                            er = r as DefinedService
                            break
                        case { it instanceof Snippet }:
                            er = r as Snippet
                            break
                    }
                    er.refresh()
                    return er
                }.find {
                    it.name == name
                }
                if(!!rule) {
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
                    return [ rule: rule ]
                }
                return [ error: "Rule specified does not exist!" ]
            }
            return [ error: "Rule Set specified does not exist!" ]
        }
        return [ error: "You must supply a rule set name, rule name and the updated rule"]
    }
    def updateRuleName(String ruleSetName,String name,String nameUpdate) {
        if(!!name && !!ruleSetName && !!nameUpdate) {
            def ruleSet = RuleSet.findByName(ruleSetName)
            if(!!ruleSet) {
                def rule = ruleSet.rules.collect { r ->
                    def er
                    switch(r) {
                        case { it instanceof SQLQuery }:
                            er = r as SQLQuery
                            break
                        case { it instanceof Groovy }:
                            er = r as Groovy
                            break
                        case { it instanceof StoredProcedureQuery }:
                            er = r as StoredProcedureQuery
                            break
                        case { it instanceof DefinedService }:                            
                            er = r as DefinedService
                            break
                        case { it instanceof Snippet }:
                            er = r as Snippet
                            break
                    }
                    er.refresh()
                    return er
                }.find {
                    it.name == name
                }
                if(!!rule) {
                    rule.name = nameUpdate
                    if(!rule.save(failOnError:false, flush: true, validate: true)) {
                        rule.errors.allErrors.each {
                            println it
                        }           
                        return [ error: "'${rule.errors.fieldError.field}' value '${rule.errors.fieldError.rejectedValue}' rejected" ]                        
                    }
                    return [ rule: rule ]                    
                }
                return [ error: "Rule specified does not exist!" ]
            }
            return [ error: "Rule Set specified does not exist!" ]
        }
        return [ error: "You must supply a rule set name, rule name and the updated rule name"]
    }
    def getRule(String ruleSetName,String name) {
        if(!!name && !!ruleSetName) {
            def ruleSet = RuleSet.findByName(ruleSetName)
            if(!!ruleSet) {
                def rule = ruleSet.rules.find {
                    it.name == name
                }
                if(!!rule) {
                    return [ rule: rule ]
                }
                return [ error: "Rule specified does not exist!" ]
            }
            return [ error: "Rule Set specified does not exist!" ]
        }
        return [ error: "You must supply a rule set name and rule name"]
    }
    def deleteRule(String ruleSetName,String name) {
        if(!!name && !!ruleSetName) {
            def ruleSet = RuleSet.findByName(ruleSetName)
            if(!!ruleSet) {
                def rule = ruleSet.rules.find {
                    it.name == name
                }
                if(!!rule) {
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
    def moveRule(String ruleSetName,String name,String nameUpdate) {
        if(!!name && !!ruleSetName && !!nameUpdate) {
            def ruleSet = RuleSet.findByName(ruleSetName)
            if(!!ruleSet) {
                def rule = ruleSet.rules.collect { r ->
                    def er
                    switch(r) {
                        case { it instanceof SQLQuery }:
                            er = r as SQLQuery
                            break
                        case { it instanceof Groovy }:
                            er = r as Groovy
                            break
                        case { it instanceof StoredProcedureQuery }:
                            er = r as StoredProcedureQuery
                            break
                        case { it instanceof DefinedService }:                            
                            er = r as DefinedService
                            break
                        case { it instanceof Snippet }:
                            er = r as Snippet
                            break
                    }
                    er.refresh()
                    return er
                }.find {
                    it.name == name
                }
                if(!!rule) {
                    def targetRuleSet = RuleSet.findByName(nameUpdate)
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
                                        return [ rule: rule ]
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
