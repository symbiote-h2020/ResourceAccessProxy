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
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.service.notificationResource.WebSocketController;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 *
* @author Luca Tomaselli
*/
public class PluginNotification {
    private static final Logger log = LoggerFactory.getLogger(PluginNotification.class);

    @Autowired
    ResourceAccessNotificationService notificationService;
    
    @Autowired
    WebSocketController webSocketController;
    
    @Value("${symbiote.rap.cram.url}") 
    private String notificationUrl;
    
    @Autowired
    private AuthorizationManager authManager;
    
    
    public void receiveNotification(byte[] messageByte) {
        try {
            String message = new String(messageByte, "UTF-8");
            log.debug("Plugin Notification message received.\n" + message);

            ObjectMapper mapper = new ObjectMapper();
            Observation obs = mapper.readValue(message, Observation.class);            
            sendSuccessfulPushMessage(obs.getResourceId());
            webSocketController.SendMessage(obs);
            
        } catch (Exception e) {
            log.info("Error during plugin registration process\n" + e.getMessage());
        }
    }
    
    public void sendSuccessfulPushMessage(String symbioteId){
        List<Date> dateList = new ArrayList<Date>();
        dateList.add(new Date());
        
        notificationService.addSuccessfulPushes(symbioteId, dateList);
        notificationService.sendAccessData();
    }
}
