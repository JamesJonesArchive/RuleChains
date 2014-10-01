package edu.usf.RuleChains

class UserInfoHandlerService {
    def usfCasService
    def grailsApplication
    
    /**
     * Resolves the Git email address from USF CAS attributes and uses a fallback if necessary
     *
     * @param    username     The username used to resolve an email address
     * @return                The email address associated or fallback email address if not found
     */
    def resolveEmail() {
        def username = resolveUsername()
        if(grailsApplication.config.jgit.fallbackMap[username]) {
            return grailsApplication.config.jgit.fallbackMap[username]
        } else {
            try {
                def email = usfCasService.attributes[grailsApplication.config.jgit.cas.emailAttribute]
                if(!!!!email) {
                    return email
                }
                return grailsApplication.config.jgit.fallbackEmailDefault
            } catch(e) {
                // use the default
                return grailsApplication.config.jgit.fallbackEmailDefault
            }
        }
    }

    /**
     * Resolves the Git username from USF CAS Spring Security
     *
     * @return                The username provided by Spring Security
     */
    def resolveUsername() {
        try {
            def username = usfCasService.getUsername()
            if(!!!!username) {
                return username
            }
            return grailsApplication.config.jgit.fallbackUsername
        } catch(e) {
            return grailsApplication.config.jgit.fallbackUsername
        }
    }
    /**
     * Returns a hashmap of the author and email resolution
     * 
     * @return               A map containing the username and email address
     */
    def getGitAuthorInfo() {
        return [ 
            user: resolveUsername(),
            email: resolveEmail()
        ]
    }
}
