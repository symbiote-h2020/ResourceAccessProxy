/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.interfaces;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.resources.db.ResourceInfo;
import eu.h2020.symbiote.resources.db.ResourcesRepository;
import eu.h2020.symbiote.service.notificationResource.WebSocketController;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;

import static eu.h2020.symbiote.resources.RapDefinitions.JSON_OBJECT_TYPE_FIELD_NAME;

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

    private ResourceInfo getResourceInfo(String internalId) {
        ResourceInfo resInfo = null;
        List<ResourceInfo> resInfoList = resourcesRepo.findByInternalId(internalId);

        for (ResourceInfo rInfo : resInfoList) {
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
        ResourceInfo resInfo = getResourceInfo(internalObservation.getResourceId());

        return new Observation(resInfo.getSymbioteId(), internalObservation.getLocation(),
                internalObservation.getResultTime(), internalObservation.getSamplingTime(),
                internalObservation.getObsValues());
    }

    /**
     * This method sent a successful push message to CRAM
     * @param symbioteId the id of the resource
     */
    public void sendSuccessfulPushMessage(String symbioteId){
        List<Date> dateList = new ArrayList<>();
        dateList.add(new Date());
        
        notificationService.addSuccessfulPushes(symbioteId, dateList);
        notificationService.sendAccessData();
    }
}
