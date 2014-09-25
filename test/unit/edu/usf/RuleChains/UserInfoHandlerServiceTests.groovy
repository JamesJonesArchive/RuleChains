package edu.usf.RuleChains



import grails.test.mixin.*
import org.junit.*

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(UserInfoHandlerService)
class UserInfoHandlerServiceTests {
    def grailsApplication
    void testResolveEmail() {
        def userInfoHandlerService = new UserInfoHandlerService()
        userInfoHandlerService.grailsApplication = [config: [jgit : [
            fallbackEmailDefault: 'sombody@mycompany.com',
            fallbackUsername: 'sombody',
            fallbackMap: [:]
        ]]];
        assert userInfoHandlerService.resolveEmail() == 'sombody@mycompany.com';
        userInfoHandlerService.grailsApplication = [config: [jgit : [
            fallbackEmailDefault: 'sombody@mycompany.com',
            fallbackUsername: 'sombody',
            fallbackMap: [sombody: 'sombody@anothercompany.com']
        ]]];
        assert userInfoHandlerService.resolveEmail() == 'sombody@anothercompany.com'
    }
    void testResolveUsername() {
        def userInfoHandlerService = new UserInfoHandlerService()
        userInfoHandlerService.grailsApplication = [config: [jgit : [
            fallbackUsername: 'sombody'
        ]]]
        assert userInfoHandlerService.resolveUsername() == 'sombody'
    }
    void testGetGitAuthorInfo() {
        def userInfoHandlerService = new UserInfoHandlerService()
        userInfoHandlerService.grailsApplication = [config: [jgit : [
            fallbackEmailDefault: 'sombody@mycompany.com',
            fallbackUsername: 'sombody',
            fallbackMap: [:]
        ]]]
        assert userInfoHandlerService.getGitAuthorInfo() == [ user: 'sombody', email: 'sombody@mycompany.com' ]
    }
}
