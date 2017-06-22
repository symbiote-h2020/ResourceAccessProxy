/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.bim;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.cloud.model.data.observation.Observation;
import eu.h2020.symbiote.exceptions.EntityNotFoundException;
import eu.h2020.symbiote.exceptions.GenericException;
import eu.h2020.symbiote.interfaces.ResourceAccessRestController;
import eu.h2020.symbiote.interfaces.conditions.NBInterfaceRESTCondition;
import eu.h2020.symbiote.messages.access.ResourceAccessGetMessage;
import eu.h2020.symbiote.messages.access.ResourceAccessMessage;
import eu.h2020.symbiote.resources.db.ResourceInfo;
import eu.h2020.symbiote.security.exceptions.aam.TokenValidationException;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */

@Conditional(NBInterfaceRESTCondition.class)
@RestController
public class BimRestController {
    private static final Logger log = LoggerFactory.getLogger(BimRestController.class);
    
    private static final String BIM_FILE = "/bim-0.3.owl";
    
    @RequestMapping(value="/bim", method=RequestMethod.GET)
    public HashMap<String,HashMap<String,String>> readResource() {  
        HashMap<String,HashMap<String,String>> map = new HashMap<String,HashMap<String,String>>();
        try {
            String c = "CIAO";
            TripleStore t = new TripleStore();
            map = t.map;
            String b = "CIAO";
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new GenericException(e.getMessage());
        }
        return map;
    }
    
    @RequestMapping(value="/bim2", method=RequestMethod.GET)
    public HashMap<String,HashMap<String,String>> readResource2() {  
        HashMap<String,HashMap<String,String>> map = new HashMap<String,HashMap<String,String>>();
        try {
            String c = "CIAO";
            TripleStore t = new TripleStore("null");
            map = t.map;
            String b = "CIAO";
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new GenericException(e.getMessage());
        }
        return map;
    }
}
