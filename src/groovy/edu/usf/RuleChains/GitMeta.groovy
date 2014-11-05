/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.usf.RuleChains

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.InitCommand
import org.eclipse.jgit.api.CloneCommand
import org.eclipse.jgit.api.FetchCommand
import org.eclipse.jgit.api.LsRemoteCommand
import org.eclipse.jgit.api.PullCommand
import org.eclipse.jgit.api.PushCommand
import org.eclipse.jgit.api.SubmoduleSyncCommand
import org.eclipse.jgit.api.RmCommand
import org.eclipse.jgit.api.CheckoutCommand
import org.eclipse.jgit.api.StatusCommand
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.api.errors.GitAPIException
import grails.converters.*
import edu.usf.RuleChains.*
import org.hibernate.FlushMode
import grails.util.Holders
/**
 * GitMeta performs all the metaprogramming for the accessing 
 * an internal Git repository where it's needed for syncronization.
 * <p>
 * Developed originally for the University of South Florida
 * 
 * @author <a href='mailto:james@mail.usf.edu'>James Jones</a> 
 */ 
class GitMeta {
    /**
     * The builder method that creates all the metaprogramming methods
     * 
     * @param   grailsApplication    The Grails GrailsApplication object
     */        
    def buildMeta = { grailsApplication ->
        for (domainClass in grailsApplication.domainClasses.findAll { sc -> sc.name in ['DefinedService','SQLQuery','Snippet','StoredProcedureQuery','PHP','Groovy','Python','Ruby']}) {
            /**
             * Provides a Git delete method on Rule Domain classes
             * 
             * @param    comment       The comment used in the Git commit
             */
            domainClass.metaClass.deleteGitWithComment {comment ->
                def domainDelegate = delegate
                delegate.withJGit { rf ->
                    def relativePath = "ruleSets/${domainDelegate.getPersistentValue('ruleSet').name}/${domainDelegate.name}.json"
                    pull().call()
                    def f = new File(rf,relativePath)
                    if(f.exists()) {
                        rm().addFilepattern("${relativePath}").call()
                        f.delete()
                    }
                    if(!status().call().isClean()) {
                        commit().setMessage(comment).call()
                    }
                    push().call()
                    pull().call()
                }
            }
            /**
             * Provides a Git update method on Rule Domain classes
             * 
             * @param    comment       The comment used in the Git commit
             */
            domainClass.metaClass.updateGitWithComment {comment ->
                def domainDelegate = delegate
                delegate.withJGit { rf ->
                    def relativePath = "ruleSets/${domainDelegate.ruleSet.name}/${domainDelegate.name}.json"
                    def oldRelativePath = "ruleSets/${domainDelegate.ruleSet.name}/${domainDelegate.getPersistentValue("name")}.json"
                    pull().call()
                    if (domainDelegate.isDirty('name')) {
                        def f = new File(rf,oldRelativePath)
                        if(f.exists()) {                    
                            f.renameTo(new File(rf,relativePath))
                            add().addFilepattern("${relativePath}").call()
                            rm().addFilepattern("${oldRelativePath}").call()
                            if(!status().call().isClean()) {
                                commit().setMessage(comment).call()
                            }
                            push().call();
                            pull().call()
                        }
                    }
                }
            }
            /**
             * Provides a Git save method on Rule Domain classes
             * 
             * @param    comment       The comment used in the Git commit
             */
            domainClass.metaClass.saveGitWithComment {comment ->
                def domainDelegate = delegate
                delegate.withJGit { rf ->
                    def relativePath = "ruleSets/${domainDelegate.ruleSet.name}/${domainDelegate.name}.json"
                    def f = new File(rf,relativePath)
                    pull().call()
                    switch(domainDelegate) {
                    case { it instanceof SQLQuery }:
                        println "Writing SQLQuery File ${domainDelegate.name}.json"
                        f.text = {j->
                            j.setPrettyPrint(true)
                            return j
                        }.call([
                                rule: domainDelegate?.rule,
                                "class": domainDelegate['class']
                            ] as JSON)                        
                        break
                    case { it instanceof Groovy }:
                        println "Writing Groovy File ${domainDelegate.name}.json"
                        f.text = {j->
                            j.setPrettyPrint(true)
                            return j
                        }.call([
                                rule: domainDelegate?.rule,
                                "class": domainDelegate['class']
                            ] as JSON)                        
                        break
                    case { it instanceof Python }:
                        println "Writing Python File ${domainDelegate.name}.json"
                        f.text = {j->
                            j.setPrettyPrint(true)
                            return j
                        }.call([
                                rule: domainDelegate?.rule,
                                "class": domainDelegate['class']
                            ] as JSON)                        
                        break
                    case { it instanceof Ruby }:
                        println "Writing Ruby File ${domainDelegate.name}.json"
                        f.text = {j->
                            j.setPrettyPrint(true)
                            return j
                        }.call([
                                rule: domainDelegate?.rule,
                                "class": domainDelegate['class']
                            ] as JSON)                        
                        break
                    case { it instanceof PHP }:
                        println "Writing PHP File ${domainDelegate.name}.json"
                        f.text = {j->
                            j.setPrettyPrint(true)
                            return j
                        }.call([
                                rule: delegate?.rule,
                                "class": delegate['class']
                            ] as JSON)                        
                        break
                    case { it instanceof StoredProcedureQuery }:
                        println "Writing StoredProcedureQuery File ${domainDelegate.name}.json"
                        f.text = {j->
                            j.setPrettyPrint(true)
                            return j
                        }.call([
                                rule: domainDelegate?.rule,
                                "class": domainDelegate['class'],
                                "closure": domainDelegate['closure']
                            ] as JSON)                        
                        break
                    case { it instanceof DefinedService }:  
                        println "Writing DefinedService File ${domainDelegate.name}.json"
                        def ds = domainDelegate as JSON
                        println ds
                        f.text = {j->
                            j.setPrettyPrint(true)
                            return j
                        }.call([
                                method:{ m->
                                    switch(m) {
                                    case MethodEnum.GET:
                                        return "GET"
                                        break
                                    case MethodEnum.POST:
                                        return "POST"
                                        break
                                    case MethodEnum.PUT:
                                        return "PUT"
                                        break
                                    case MethodEnum.DELETE:
                                        return "DELETE"
                                        break
                                    }                                
                                }.call(domainDelegate.method),
                                authType:{ t->
                                    switch(t) {
                                    case AuthTypeEnum.NONE:
                                        return "NONE"
                                        break
                                    case AuthTypeEnum.BASIC:
                                        return "BASIC"
                                        break
                                    case AuthTypeEnum.DIGEST:
                                        return "DIGEST"
                                        break
                                    case AuthTypeEnum.CAS:
                                        return "CAS"
                                        break
                                    case AuthTypeEnum.CASSPRING:
                                        return "CASSPRING"
                                        break
                                    }                                
                                }.call(domainDelegate.authType),
                                parse:{ p->
                                    switch(p) {
                                    case ParseEnum.TEXT:
                                        return "TEXT"
                                        break
                                    case ParseEnum.XML:
                                        return "XML"
                                        break
                                    case ParseEnum.JSON:
                                        return "JSON"
                                        break
                                    }                                
                                }.call(domainDelegate.parse),
                                url: domainDelegate.url,
                                springSecurityBaseURL: domainDelegate.springSecurityBaseURL,
                                user: domainDelegate.user,
                                password: domainDelegate.password,
                                "class": domainDelegate['class']
                            ] as JSON)    
                        println "DONE!"
                        break
                    case { it instanceof Snippet }:
                        println "Writing Snippet File ${domainDelegate.name}.json"
                        f.text = {j->
                            j.setPrettyPrint(true)
                            return j
                        }.call([
                                chain: domainDelegate?.chain?.name,
                                "class": domainDelegate['class']
                            ] as JSON)                        
                        break
                    }
                    add().addFilepattern("${relativePath}").call()
                    if(!status().call().isClean()) {
                        commit().setMessage(comment).call()
                    }
                    push().call()
                    pull().call()
                }                    
            }
        }
        /**
         * Provides a Git delete method on RuleSet Domain class
         * 
         * @param    comment       The comment used in the Git commit
         */
        RuleSet.metaClass.deleteGitWithComment  = {comment->  
            def domainDelegate = delegate
            delegate.withJGit { rf ->
                def relativePath = "ruleSets/${domainDelegate.name}/"
                pull().call()
                new File(rf,relativePath).deleteDir()
                rm().addFilepattern("${relativePath}").call()
                if(!status().call().isClean()) {
                    commit().setMessage(comment).call()
                }
                push().call()
                pull().call()            
            }
        }
        /**
         * Provides a Git update method on RuleSet Domain class
         * 
         * @param    comment       The comment used in the Git commit
         */
        RuleSet.metaClass.updateGitWithComment = {comment ->
            def domainDelegate = delegate
            delegate.withJGit { rf ->
                def relativePath = "ruleSets/${domainDelegate.name}/"
                def f = new File(rf,relativePath)
                pull().call()
                if (domainDelegate.isDirty('name')) {
                    def oldRelativePath = "ruleSets/${domainDelegate.getPersistentValue("name")}/"
                    def of = new File(rf,oldRelativePath)
                    if(of.exists()) {
                        of.renameTo(f)
                        add().addFilepattern("${relativePath}").call()
                        rm().addFilepattern("${oldRelativePath}").call()
                    } else {
                        if(!f.exists()) {
                            f.mkdirs()
                        }
                        add().addFilepattern("${relativePath}").call()
                    }
                    if(!status().call().isClean()) {
                        commit().setMessage(comment).call()
                    }
                    push().call()
                    pull().call()            
                }
            }                
        }
        /**
         * Provides a Git save method on RuleSet Domain class
         * 
         * @param    comment       The comment used in the Git commit
         */
        RuleSet.metaClass.saveGitWithComment = {comment ->
            def domainDelegate = delegate
            delegate.withJGit { rf ->
                def relativePath = "ruleSets/${domainDelegate.name}/"
                def f = new File(rf,relativePath)
                pull().call()
                if(!f.exists()) {
                    f.mkdirs()
                }            
                add().addFilepattern("${relativePath}").call()
                if(!status().call().isClean()) {
                    commit().setMessage(comment).call()
                }
                push().call()
                pull().call()
            }
        }
        /**
         * Provides a Git delete method on Chain Domain class
         * 
         * @param    comment       The comment used in the Git commit
         */
        Chain.metaClass.deleteGitWithComment  = {comment->  
            def domainDelegate = delegate
            delegate.withJGit { rf ->
                def relativePath = "chains/${domainDelegate.name}/"
                new File(rf,relativePath).deleteDir()
                pull().call()
                rm().addFilepattern("${relativePath}").call()
                if(!status().call().isClean()) {
                    commit().setMessage(comment).call()
                }
                push().call()
            }
        }
        /**
         * Provides a Git update method on Chain Domain class
         * 
         * @param    comment       The comment used in the Git commit
         */
        Chain.metaClass.updateGitWithComment = {comment ->
            def domainDelegate = delegate
            delegate.withJGit { rf ->
                def relativePath = "chains/${domainDelegate.name}/"
                def f = new File(rf,relativePath)
                pull.call()
                if (domainDelegate.isDirty('name')) {
                    def oldRelativePath = "chains/${domainDelegate.getPersistentValue("name")}/"
                    def of = new File(rf,oldRelativePath)
                    if(of.exists()) {
                        of.renameTo(f)
                        add().addFilepattern("${relativePath}").call()
                        rm().addFilepattern("${oldRelativePath}").call()                
                    } else {
                        if(!f.exists()) {
                            f.mkdirs()
                        }
                        add().addFilepattern("${relativePath}").call()
                    }
                    if(!status().call().isClean()) {
                        commit().setMessage(comment).call()
                    }
                    push().call()
                    pull().call()
                }
            }                
        }
        /**
         * Provides a Git save method on Chain Domain class
         * 
         * @param    comment       The comment used in the Git commit
         */
        Chain.metaClass.saveGitWithComment = {comment ->
            def domainDelegate = delegate
            delegate.withJGit { rf ->
                def relativePath = "chains/${domainDelegate.name}/"
                def f = new File(rf,relativePath)
                pull().call()
                if(!f.exists()) {
                    f.mkdirs()
                }         
                add().addFilepattern("${relativePath}").call()
                add().addFilepattern(".").call()
                if(!status().call().isClean()) {
                    commit().setMessage(comment).call()
                }
                push().call()
                pull().call()
            }                
        }
        /**
         * Provides a Git delete method on ChainServiceHandler Domain class
         * 
         * @param    comment       The comment used in the Git commit
         */
        ChainServiceHandler.metaClass.deleteGitWithComment {comment ->
            def domainDelegate = delegate
            delegate.withJGit { rf ->
                def relativePath = "chainServiceHandlers/${domainDelegate.name}.json"
                pull.call()
                def f = new File(rf,relativePath)
                if(f.exists()) {
                    f.delete()
                    rm().addFilepattern("${relativePath}").call()
                    if(!status().call().isClean()) {
                        commit().setMessage(comment).call()
                    }
                    push().call()
                }
                pull().call()
            }                
        }
        /**
         * Provides a Git update method on ChainServiceHandler Domain class
         * 
         * @param    comment       The comment used in the Git commit
         */
        ChainServiceHandler.metaClass.updateGitWithComment = {comment ->
            def domainDelegate = delegate
            delegate.withJGit { rf ->
                def relativePath = "chainServiceHandlers/${domainDelegate.name}.json"
                pull.call()
                if (domainDelegate.isDirty('name')) {
                    def oldRelativePath = "chainServiceHandlers/${domainDelegate.getPersistentValue("name")}.json"
                    def f = new File(rf,oldRelativePath)
                    if(f.exists()) {
                        f.renameTo(new File(rf,relativePath))
                        add().addFilepattern("${relativePath}").call()
                        rm().addFilepattern("${oldRelativePath}").call()                
                        if(!status().call().isClean()) {
                            commit().setMessage(comment).call()
                        }
                        push.call()
                        pull.call()
                    }
                }
            }                
        }
        /**
         * Provides a Git save method on ChainServiceHandler Domain class
         * 
         * @param    comment       The comment used in the Git commit
         */
        ChainServiceHandler.metaClass.saveGitWithComment {comment ->
            def domainDelegate = delegate
            delegate.withJGit { rf ->
                def relativePath = "chainServiceHandlers/${domainDelegate.name}.json"
                new File(rf,"chainServiceHandlers/").mkdirs()
                def f = new File(rf,relativePath)
                pull().call()
                f.text = {j->
                    j.setPrettyPrint(true)
                    return j
                }.call([
                        chain: domainDelegate.chain.name,
                        inputReorder: domainDelegate.inputReorder,
                        outputReorder: domainDelegate.outputReorder,
                        method:{ m->
                            switch(m) {
                            case MethodEnum.GET:
                                return "GET"
                                break
                            case MethodEnum.POST:
                                return "POST"
                                break
                            case MethodEnum.PUT:
                                return "PUT"
                                break
                            case MethodEnum.DELETE:
                                return "DELETE"
                                break
                            }                                
                        }.call(domainDelegate.method)
                    ] as JSON)
                add().addFilepattern("${relativePath}").call()
                add().addFilepattern(".").call()
                if(!status().call().isClean()) {
                    commit().setMessage(comment).call()
                }
                push().call()
                pull().call()
            }                
        }
        /**
         * Provides a Git delete method on Link Domain class
         * 
         * @param    comment       The comment used in the Git commit
         */
        Link.metaClass.deleteGitWithComment {comment ->
            def domainDelegate = delegate
            delegate.withJGit { rf ->
                pull().call()
                def relativePath = "chains/${domainDelegate.getPersistentValue('chain').name}/${domainDelegate.sequenceNumber}.json"
                def f = new File(rf,relativePath)
                if(f.exists()) {
                    f.delete()
                    rm().addFilepattern("${relativePath}").call()
                    if(!status().call().isClean()) {
                        commit().setMessage(comment).call()
                    }
                    push().call()
                }
                pull().call()
            }
        }
        /**
         * Provides a Git update method on Link Domain class
         * 
         * @param    comment       The comment used in the Git commit
         */
        Link.metaClass.updateGitWithComment {comment ->
            def domainDelegate = delegate
            delegate.withJGit { rf ->
                def relativePath = "chains/${domainDelegate.chain.name}/${domainDelegate.sequenceNumber}.json"
                pull().call()
                if (domainDelegate.isDirty('sequenceNumber')) {
                    def oldRelativePath = "chains/${domainDelegate.chain.name}/${domainDelegate.getPersistentValue("sequenceNumber")}.json"
                    def f = new File(rf,oldRelativePath)
                    if(f.exists()) {
                        f.renameTo(new File(rf,relativePath))
                        add().addFilepattern("${relativePath}").call()
                        if(!relativePath.equalsIgnoreCase(oldRelativePath)) {
                            rm().addFilepattern("${oldRelativePath}").call()
                        }
                        if(!status().call().isClean()) {
                            commit().setMessage(comment).call()
                        }
                        push().call()
                        pull().call()
                    }                
                }
            }
        }
        /**
         * Provides a Git save method on Link Domain class
         * 
         * @param    comment       The comment used in the Git commit
         */
        Link.metaClass.saveGitWithComment {comment ->
            def domainDelegate = delegate
            delegate.withJGit { rf ->
                def relativePath = "chains/${domainDelegate.chain.name}/${domainDelegate.sequenceNumber}.json"
                def f = new File(rf,relativePath)
                pull().call()
                f.text = {j->
                    j.setPrettyPrint(true)
                    return j
                }.call([
                        sourceName: domainDelegate.sourceName,
                        inputReorder: domainDelegate.inputReorder,
                        outputReorder: domainDelegate.outputReorder,
                        executeEnum:{ e->
                            switch(e) {
                            case ExecuteEnum.EXECUTE_USING_ROW:
                                return "EXECUTE_USING_ROW"
                                break
                            case ExecuteEnum.NORMAL:
                                return "NORMAL"
                                break
                            }                                
                        }.call(domainDelegate.executeEnum),
                        resultEnum:{ r->
                            switch(r) {
                            case ResultEnum.NONE:
                                return "NONE"
                                break
                            case ResultEnum.UPDATE:
                                return "UPDATE"
                                break
                            case ResultEnum.RECORDSET:
                                return "RECORDSET"
                                break
                            case ResultEnum.ROW:
                                return "ROW"
                                break
                            case ResultEnum.APPENDTOROW:
                                return "APPENDTOROW"
                                break
                            case ResultEnum.PREPENDTOROW:
                                return "PREPENDTOROW"
                                break
                            }                                
                        }.call(domainDelegate.resultEnum),
                        linkEnum:{ l->
                            switch(l) {
                            case LinkEnum.NONE:
                                return "NONE"
                                break
                            case LinkEnum.LOOP:
                                return "LOOP"
                                break
                            case LinkEnum.ENDLOOP:
                                return "ENDLOOP"
                                break
                            case LinkEnum.NEXT:
                                return "NEXT"
                                break
                            }                                
                        }.call(domainDelegate.linkEnum),
                        rule: domainDelegate.rule.name,
                    "class": domainDelegate['class']
                    ] as JSON) 
                add().addFilepattern("${relativePath}").call()
                add().addFilepattern(".").call()
                if(!status().call().isClean()) {
                    commit().setMessage(comment).call()
                }
                push().call()
                pull().call()
            }
            
        }
        // END DOMAIN CLASSES
        // Wrapping the Schedule Lister
        /**
         * Provides a Git save method on RuleChainsSchedulerListener Quartz Schedule Listener
         * 
         * @param    jobDetail     The Quartz JobDetail object on the current Quartz schedule
         * @param    triggers      A list of quartz triggers
         * @param    comment       The comment used in the Git commit
         * @see      JobDetail
         * @see      Trigger
         */        
        RuleChainsSchedulerListener.metaClass.saveGitWithComment {jobDetail,triggers,comment ->
            (new Link()).withJGit { rf ->
                def jobFolder = new File(rf,"jobs/")
                if(!jobFolder.exists()) {
                    jobFolder.mkdirs()
                }
                pull().call()
                def jobKey = jobDetail.getKey()
                println jobKey.name
                println jobKey
                def dataMap = jobDetail.getJobDataMap()
                def gitAuthorInfo = dataMap.get("gitAuthorInfo")
                def relativePath = "jobs/${jobKey.name}.json"
                def f = new File(rf,relativePath)
                f.text = {js->
                    js.setPrettyPrint(true)
                    return js                            
                }.call([
                        group: jobKey.group,
                        name: jobKey.name,
                        triggers: triggers,
                        chain: dataMap.getString("chain"),
                        input: dataMap.get("input"),
                        emailLog: dataMap.get("emailLog")
                    ] as JSON)
                add().addFilepattern("${relativePath}").call()
                if(!status().call().isClean()) {
                    commit().setAuthor(gitAuthorInfo.user,gitAuthorInfo.email).setMessage(comment).call()
                }
                push().call()
                pull().call() 
            }
        }
        /**
         * Provides a Git delete method on RuleChainsSchedulerListener Quartz Schedule Listener
         * 
         * @param    context       The Quartz JobExecutionContext object on the current Quartz schedule
         * @param    comment       The comment used in the Git commit
         * @see      JobExecutionContext
         */        
        RuleChainsSchedulerListener.metaClass.deleteGitWithComment {context,comment ->
            (new Link()).withJGit { rf ->
                def jobKey = context.getJobDetail().getKey()
                pull().call()
                def relativePath = "jobs/${jobKey.name}.json"
                def dataMap = context.getMergedJobDataMap()
                def gitAuthorInfo = dataMap.get("gitAuthorInfo")            
                def f = new File(rf,relativePath)
                if(f.exists()) {
                    f.delete()
                    rm().addFilepattern("${relativePath}").call()
                    if(!status().call().isClean()) {
                        commit().setAuthor(gitAuthorInfo.user,gitAuthorInfo.email).setMessage(comment).call()
                    }
                    push().call()
                }
                pull().call()
            }
        }    
        /**
         * Provides a Git delete method on RuleChainsJobListener Quartz Job Listener
         * 
         * @param    context       The Quartz JobExecutionContext object on the current Quartz schedule
         * @param    comment       The comment used in the Git commit
         * @see      JobExecutionContext
         */        
        RuleChainsJobListener.metaClass.deleteGitWithComment {context,comment ->
            (new Link()).withJGit { rf ->
                def jobKey = context.getJobDetail().getKey()
                pull().call()
                def relativePath = "jobs/${jobKey.name}.json"
                def dataMap = context.getMergedJobDataMap()
                def gitAuthorInfo = dataMap.get("gitAuthorInfo")            
                def f = new File(rf,relativePath)
                if(f.exists()) {
                    f.delete()
                    rm().addFilepattern("${relativePath}").call()
                    if(!status().call().isClean()) {
                        commit().setAuthor(gitAuthorInfo.user,gitAuthorInfo.email).setMessage(comment).call()
                    }
                    push().call()
                }
                pull().call()
            }                
        }    
        /**
         * Provides a Git save method on RuleChainsJobListener Quartz Job Listener
         * 
         * @param    context       The Quartz JobExecutionContext object on the current Quartz schedule
         * @param    comment       The comment used in the Git commit
         * @see      JobExecutionContext
         */        
        RuleChainsJobListener.metaClass.saveGitWithComment {context,comment ->
            (new Link()).withJGit { rf ->
                def jobFolder = new File(rf,"jobs/")
                if(!jobFolder.exists()) {
                    jobFolder.mkdirs()
                }
                pull().call()
                def jobKey = context.getJobDetail().getKey()
                println jobKey.name
                println jobKey
                def dataMap = context.getMergedJobDataMap()
                def gitAuthorInfo = dataMap.get("gitAuthorInfo")
                def relativePath = "jobs/${jobKey.name}.json"
                def f = new File(rf,relativePath)
                f.text = {js->
                    js.setPrettyPrint(true)
                    return js                            
                }.call([
                        group: jobKey.group,
                        triggers: context.getScheduler().getTriggersOfJob(jobKey).collect { it.getCronExpression() },
                        chain: dataMap.getString("chain"),
                        input: dataMap.get("input"),
                        emailLog: dataMap.get("emailLog")
                    ] as JSON)
                add().addFilepattern("${relativePath}").call()
                if(!status().call().isClean()) {
                    commit().setAuthor(gitAuthorInfo.user,gitAuthorInfo.email).setMessage(comment).call()
                }
                push().call()
                pull().call() 
            }
        }
        // END Wrapping the Schedule Lister
    }
}

