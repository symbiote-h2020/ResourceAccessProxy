/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.interfaces;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.resources.RapDefinitions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import eu.h2020.symbiote.security.commons.Coupon;
import eu.h2020.symbiote.security.communication.payloads.BarteredAccessRequest;

/**
 * This class handles communication between RAP and Bartering and Trading Manager
 *
 * @author Pavle Skocir
 */
@Component 
public class BarteringTradingCommunicationService {
	
    private static final Logger log = LoggerFactory.getLogger(BarteringTradingCommunicationService.class);
	
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Value("${rabbit.exchange.bartered.access}")
    private String barteringExchangeName;

    @Value("${rabbit.routingKey.bartered.access}")
    private String barteringAccessKey;
    
    private ObjectMapper mapper;
    
    private BarteredAccessRequest  barteredAccessRequest;
            
    public BarteringTradingCommunicationService() {
    	    	
    	mapper = new ObjectMapper();
    	mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    	mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }
    
    public void addRequest(String clientPlatform, String federationId, String federatedResourceId, Coupon.Type type){
    	barteredAccessRequest = new BarteredAccessRequest(clientPlatform, federationId, federatedResourceId, type);
    	
    }
            
    /**
     * 
     * sending RabbitMQ message to B&T manager & receiving response in a string
     * @param message
     */
    private boolean sendMessage(String message){
    	String response = (String) rabbitTemplate.convertSendAndReceive(barteringExchangeName, barteringAccessKey, message);
        log.info("Sent message to check access rights to B&T manager and received: " + response);
        if (response.equals(HttpStatus.OK.toString())) {
        	log.info("Success");
        	return true;
        }
        else {
        	log.error("Failure");
        	return false;
        }
    }

	public boolean sendToCheck() {
		String jsonMessage = null;
		try {
        	synchronized (barteredAccessRequest) {
        		jsonMessage = mapper.writeValueAsString(barteredAccessRequest);
                log.debug("barteredAccessRequest = " + jsonMessage);
			}
            sendMessage(jsonMessage);
        } catch (JsonProcessingException jsonEx) {
            log.error(jsonEx.toString(), jsonEx);
        }
		
		
		return sendMessage(jsonMessage);
	}
    
}
