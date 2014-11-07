package edu.usf.RuleChains

import grails.converters.*
import java.util.zip.ZipInputStream
import java.io.DataInputStream
import java.io.BufferedInputStream
import java.io.FileInputStream

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
        def uploadStatus
        def file = request.getFile("contents")
        // Zip test from http://www.java2s.com/Code/Java/File-Input-Output/DeterminewhetherafileisaZIPFile.htm
        DataInputStream din = new DataInputStream(new BufferedInputStream(file.getInputStream()))
        int zipTest = din.readInt()
        din.close();
        if(zipTest == 0x504b0304) {
            def result = configService.uploadChainData(new ZipInputStream(file.getInputStream()),params.merge)
            uploadStatus = [
                "status": "success",
                "message": "The upload was successfully uploaded"
            ]
        } else {
            uploadStatus = [
                "status": "error",
                "message": "The uploaded file was not recognized as a Zip file!"
            ]            
        }
        redirect(uri:"/?"+uploadStatus.inject("") {s,k,v -> s += "$k=${v.encodeAsURL()}&"; s}[0..-2]+"#backup") // strips trailing &
    }
}
