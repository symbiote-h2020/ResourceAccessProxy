/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.interfaces;

import static eu.h2020.symbiote.resources.RapDefinitions.JSON_OBJECT_TYPE_FIELD_NAME;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.resources.db.DbResourceInfo;
import eu.h2020.symbiote.resources.db.ResourcesRepository;
import eu.h2020.symbiote.service.notificationResource.WebSocketController;

/**
 *
* @author Luca Tomaselli
*/
public class PluginNotification {
    private static final Logger log = LoggerFactory.getLogger(PluginNotification.class);

    @Autowired
    WebSocketController webSocketController;
    
    @Value("${symbiote.rap.cram.url}") 
    private String notificationUrl;
    
    @Autowired
    private AuthorizationManager authManager;

    @Autowired
    private ResourcesRepository resourcesRepo;


    /**
     *
     * This method is receiving push notifications from plugin
     *
     * @param messageObject the message itself
     */
    public void receiveNotification(Object messageObject) {
        try {
            String message = checkPushNotificationMessageFormat(messageObject);
            log.debug("Plugin Notification message received.\n" + message);

            // THIS WOULD CUT OUT SUPPORT FOR PIMs in notification mechanism
            Observation observation = changeInternalIdToSymbIoTeIdInObservation(messageObject);
            sendSuccessfulPushMessage(observation.getResourceId());

            webSocketController.SendMessage(observation);
            
        } catch (Exception e) {
            log.info("Error while processing notification received from plugin \n" + e.getMessage());
        }
    }

    private DbResourceInfo getResourceInfo(String internalId) {
        DbResourceInfo resInfo = null;
        List<DbResourceInfo> resInfoList = resourcesRepo.findByInternalId(internalId);

        for (DbResourceInfo rInfo : resInfoList) {
            List<String> sessionList = rInfo.getSessionId();
            if(!sessionList.isEmpty()) {
                resInfo=rInfo;
                break;
            }
        }

        return resInfo;
    }

    private String checkPushNotificationMessageFormat(Object obj) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        if (obj == null) {
            log.error("Empty notification from plugin");
            throw new Exception("Empty notification from plugin");
        }

        String rawObj;
        if (obj instanceof byte[]) {
            rawObj = new String((byte[]) obj, "UTF-8");
        } else if (obj instanceof String){
            rawObj = (String) obj;
        } else {
            throw new Exception("Can not parse response from RAP plugin. Expected byte[] or String but got " +
                    obj.getClass().getName());
        }

        try {
            JsonNode jsonObj = mapper.readTree(rawObj);
            if (!jsonObj.has(JSON_OBJECT_TYPE_FIELD_NAME)) {
                log.error("Field " + JSON_OBJECT_TYPE_FIELD_NAME + " is mandatory");
            }
            return rawObj;
        } catch (Exception e) {
            throw new ODataApplicationException("Can not parse response from RAP to JSON.\n Cause: " + e.getMessage(),
                    HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(),
                    Locale.ROOT,
                    e);
        }
    }

    private Observation changeInternalIdToSymbIoTeIdInObservation(Object obj) throws Exception {
        Observation internalObservation = null;
        if(obj instanceof Observation) {
            internalObservation = (Observation) obj;
        } else if(obj instanceof Map) {
            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.writeValueAsString(obj);
            internalObservation = mapper.readValue(jsonBody, Observation.class);

        }
        DbResourceInfo resInfo = getResourceInfo(internalObservation.getResourceId());

        return new Observation(resInfo.getSymbioteId(), internalObservation.getLocation(),
                internalObservation.getResultTime(), internalObservation.getSamplingTime(),
                internalObservation.getObsValues());
    }

    /**
     * This method sent a successful push message to CRAM
     * @param symbioteId the id of the resource
     */
    public void sendSuccessfulPushMessage(String symbioteId){
        String jsonNotificationMessage = null;
        ObjectMapper map = new ObjectMapper();
        map.configure(SerializationFeature.INDENT_OUTPUT, true);
        map.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        
        List<Date> dateList = new ArrayList<Date>();
        dateList.add(new Date());
        ResourceAccessNotification notificationMessage = new ResourceAccessNotification(authManager,notificationUrl);
        
        try{
            notificationMessage.SetSuccessfulPushes(symbioteId, dateList);
            jsonNotificationMessage = map.writeValueAsString(notificationMessage);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
        notificationMessage.SendSuccessfulPushMessage(jsonNotificationMessage);
    }
}
