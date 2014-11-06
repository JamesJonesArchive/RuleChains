package edu.usf.RuleChains

import grails.converters.*
/**
 * ChainController provides for REST services handling the backup and restoration of rules, chains, chainServiceHandlers
 * <p>
 * Developed originally for the University of South Florida
 * 
 * @author <a href='mailto:james@mail.usf.edu'>James Jones</a> 
 */ 
class ConfigController {
    def configService
    
    /**
     * Returns an zip file containing rules,chains and chainServiceHandlers
     * 
     */    
    def downloadChainData() {
        response.setHeader "Content-disposition", "attachment; filename=RCBackup.zip"
        response.contentType = 'application/zip'
        response.outputStream << configService.downloadChainData()
        response.outputStream.flush()        
    }
    /**
     * Takes the JSON object from the upload and merges it into the syncronized
     * Git repository and live database
     * 
     */    
    def uploadChainData() {
        withFormat {
            html {
                return configService.uploadChainData(JSON.parse(params.upload))
            }
            xml {
                render configService.uploadChainData(JSON.parse(params.upload)) as XML
            }
            json {
                JSON.use("deep") { render configService.uploadChainData(JSON.parse(params.upload)) as JSON }
            }
        }         
    }
}
