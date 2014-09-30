/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.usf.RuleChains

/**
 * ServiceTypeEnum is an Enumerator for the various Rule Types.
 * <p>
 * Developed originally for the University of South Florida
 * 
 * @author <a href='mailto:james@mail.usf.edu'>James Jones</a> 
 */ 
enum ServiceTypeEnum {
    SQLQUERY,
    GROOVY,
    PYTHON,
    RUBY,
    STOREDPROCEDUREQUERY, 
    DEFINEDSERVICE,
    SNIPPET;
    /**
     * Converts a base string into a corresponding enumerated type
     * 
     * @param   str    The string name of the enumerated type
     * @return         A corresponding enumerated type matched on the string parameter
     */                    
    public static ServiceTypeEnum byName(String str) {
        for (serviceTypeEnum in ServiceTypeEnum.values()) {
            if(str.trim().toUpperCase().equalsIgnoreCase(serviceTypeEnum.name())) {
                return serviceTypeEnum;
            }
        }
        return ServiceTypeEnum.SQLQUERY;            
    }  	
}

