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
import eu.h2020.symbiote.core.cci.accessNotificationMessages.NotificationMessage;
import eu.h2020.symbiote.resources.RapDefinitions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 *
 * @author Luca Tomaselli, Pavle Skocir
 */
@Component 
public class ResourceAccessNotificationService {
	
    private static final Logger log = LoggerFactory.getLogger(ResourceAccessNotificationService.class);
	
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    @Qualifier(RapDefinitions.RAP_ACCESS_EXCHANGE)
    DirectExchange exchange;
    
    private ObjectMapper mapper;
    
    private NotificationMessage notificationMessage;
            
    public ResourceAccessNotificationService() {
    	notificationMessage = new NotificationMessage();
    	
    	mapper = new ObjectMapper();
    	mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    	mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }
    
    public void addSuccessfulAttempts(String symbioTeId, List<Date> timestamp, String accessType){
    	synchronized (notificationMessage) {
    		notificationMessage.addSuccessfulAttempts(symbioTeId, timestamp, accessType);
		}
    }
    
    public void addSuccessfulAttemptsList(List<String> symbioTeIdList, List<Date> timestamp, String accessType){
    	synchronized (notificationMessage) {
    		notificationMessage.addSuccessfulAttemptsList(symbioTeIdList, timestamp, accessType);
		}
    }
    
    public void addSuccessfulPushes(String symbioTeId, List<Date> timestamp){
    	synchronized (notificationMessage) {
    		notificationMessage.addSuccessfulPushes(symbioTeId, timestamp);
		}
    }
    
    public void addFailedAttempts (String symbioTeId, List<Date> timestamp, 
            String code, String message, String appId, String issuer, 
            String validationStatus, String requestParams) {
		synchronized (notificationMessage) {
			notificationMessage.addFailedAttempts(symbioTeId, timestamp, code, message, appId, issuer, validationStatus, requestParams);
		}
    }


    
    /**
     * old: sending HTTP request to CRAM
     * new: replaced with sending RabbitMQ message to monitoring
     * @param message
     */
    private void sendMessage(String message){
        rabbitTemplate.convertAndSend(exchange.getName(), RapDefinitions.RAP_ACCESS_ROUTING_KEY, message);
        log.info("Sent access notification message to Monitoring");
    }

	public void sendAccessData() {
        try {
        	String jsonMessage = null;
        	synchronized (notificationMessage) {
        		jsonMessage = mapper.writeValueAsString(notificationMessage);
                log.debug("notificationMessage = " + jsonMessage);
                notificationMessage.clear();
			}
            sendMessage(jsonMessage);
        } catch (JsonProcessingException jsonEx) {
            log.error(jsonEx.toString(), jsonEx);
        }
	}
    
}
