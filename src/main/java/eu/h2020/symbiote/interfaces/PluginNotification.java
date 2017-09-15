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
import eu.h2020.symbiote.cloud.model.data.observation.Observation;
import eu.h2020.symbiote.messages.accessNotificationMessages.NotificationMessage;
import eu.h2020.symbiote.service.notificationResource.WebSocketController;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
* @author Luca Tomaselli <l.tomaselli@nextworks.it>
*/
public class PluginNotification {
    private static final Logger log = LoggerFactory.getLogger(PluginNotification.class);

    @Autowired
    WebSocketController webSocketController;
    
    
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
    
    public static void sendSuccessfulPushMessage(String symbioteId){
        String jsonNotificationMessage = null;
        ObjectMapper map = new ObjectMapper();
        map.configure(SerializationFeature.INDENT_OUTPUT, true);
        map.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        
        List<Date> dateList = new ArrayList<Date>();
        dateList.add(new Date());
        NotificationMessage notificationMessage = new NotificationMessage();
        
        try{
            notificationMessage.SetSuccessfulPushes(symbioteId, dateList);
            jsonNotificationMessage = map.writeValueAsString(notificationMessage);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
        NotificationMessage.SendSuccessfulPushMessage(jsonNotificationMessage);
    }
}
