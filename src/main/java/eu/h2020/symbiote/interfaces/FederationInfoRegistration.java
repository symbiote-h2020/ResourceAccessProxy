package eu.h2020.symbiote.interfaces;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.model.mim.Federation;
import eu.h2020.symbiote.resources.db.FederationRepository;

/**
 * Class stores federation info in the database.
 * This info is inevitable for creating federation access policy.
 * @author Pavle Skocir
 *
 */
public class FederationInfoRegistration {
	
	@Autowired
	FederationRepository fedRepo;
	
    private static Logger log = LoggerFactory.getLogger(FederationInfoRegistration.class);

    public void receiveFederationCreatedMessage(byte[] message) {
    	log.info("Federation info message received: \n" + new String(message) + "");
        
        ObjectMapper mapper = new ObjectMapper();
        
        try {
			Federation fed = mapper.readValue(message, new TypeReference<Federation>(){});
			fedRepo.save(fed);
			log.info("Federation with id: " + fed.getId() + " added to repository");
		
		} catch (JsonParseException e) {
			log.error("Error in parsing federation info", e);
		} catch (JsonMappingException e) {
			log.error("Error in mapping federation info", e);
		} catch (IOException e) {
			log.error("Error in reading federation info", e);
			e.printStackTrace();
		}
    	
    }
    
    public void receiveFederationChangedMessage(byte[] message) {
    	log.info("Federation info message received: \n" + new String(message) + "");
        
        ObjectMapper mapper = new ObjectMapper();
        
        try {
			Federation fed = mapper.readValue(message, new TypeReference<Federation>(){});
			fedRepo.save(fed);
			log.info("Federation with id: " + fed.getId() + " updated");
		
		} catch (JsonParseException e) {
			log.error("Error in parsing federation info", e);
		} catch (JsonMappingException e) {
			log.error("Error in mapping federation info", e);
		} catch (IOException e) {
			log.error("Error in reading federation info", e);
			e.printStackTrace();
		}
    	
    }

    public void receiveFederationDeletedMessage(byte[] message) {
    	try {
	        String id = new String(message);
	        log.info("Federation Unregistration message received: \n" + id + "");
	        log.debug("Unregistering federation with Id " + id);
	        fedRepo.deleteById(id);
	        log.info("Federation with id: " + id + " removed from repository.");   	
        } catch (Exception e) {
			log.error("Error deleting federation info", e);
        }
    	
    	
    }

}
