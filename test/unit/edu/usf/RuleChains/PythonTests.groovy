package edu.usf.RuleChains



import grails.test.mixin.*
import org.junit.*

/**
 * Testing Python domain class
 * <p>
 * Developed originally for the University of South Florida
 * 
 * @author <a href='mailto:james@mail.usf.edu'>James Jones</a> 
 * 
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@TestFor(Python)
class PythonTests {
    /**
     * Testing creating a new Python
     */
    void testNewPython() {
        mockDomain(Python)
        def rs = new RuleSet(name: "newRuleSet")
        rs.isSynced = false
        rs.save()
        def r = new Python(name: 'testRule',ruleSet: rs)
        assert r.validate()
    }
    /**
     * Testing a missing ruleset
     */
    void testPythonRuleMissingRuleSet() {
        mockDomain(Python)
        def r = new Python(name: 'testRule')
        assert r.validate() == false
        assert r.errors.hasFieldErrors("ruleSet")
        assert r.errors.getFieldError("ruleSet").rejectedValue == null
    }
}
