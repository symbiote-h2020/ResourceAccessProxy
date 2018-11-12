/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.plugin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.cloud.model.rap.access.ResourceAccessGetMessage;
import eu.h2020.symbiote.cloud.model.rap.access.ResourceAccessHistoryMessage;
import eu.h2020.symbiote.cloud.model.rap.access.ResourceAccessMessage;
import eu.h2020.symbiote.cloud.model.rap.access.ResourceAccessSetMessage;
import eu.h2020.symbiote.cloud.model.rap.access.ResourceAccessSubscribeMessage;
import eu.h2020.symbiote.cloud.model.rap.access.ResourceAccessUnSubscribeMessage;
import eu.h2020.symbiote.cloud.model.rap.ResourceInfo;
import eu.h2020.symbiote.messages.plugin.RapPluginErrorResponse;
import eu.h2020.symbiote.messages.plugin.RapPluginOkResponse;
import eu.h2020.symbiote.messages.plugin.RapPluginResponse;
import eu.h2020.symbiote.messages.registration.RegisterPluginMessage;
import eu.h2020.symbiote.resources.RapDefinitions;

import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 *
 * @author Matteo Pardi
 */
public abstract class PlatformPlugin {
    private static final Logger log = LoggerFactory.getLogger(PlatformPlugin.class);

    private final RabbitTemplate rabbitTemplate;
    private final TopicExchange exchange;

    public RabbitTemplate getRabbitTemplate() {
        return this.rabbitTemplate;
    }
    
    public PlatformPlugin(RabbitTemplate rabbitTemplate, TopicExchange exchange,
                          String platformId, boolean hasFilters, boolean hasNotifications) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange; 
        registerPlugin(platformId, hasFilters, hasNotifications);
    }  
    
    
    public String receiveMessage(String message) {
        ObjectMapper mapper = new ObjectMapper();
        String json = null;
        ResourceAccessMessage.AccessType access = null;
        ResourceAccessMessage msg;
        try {
            msg = mapper.readValue(message, ResourceAccessMessage.class);
            access = msg.getAccessType();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        } catch (IOException e) {
            log.error("Error parsing request from generic RAP", e);
            return serializeResponse(mapper, 
                    new RapPluginErrorResponse(500, "Bad request arrived in RAP plugin.\nCause: " + e.getMessage()));
        }
            
        try {            
            switch(access) {
                case GET: {
                    ResourceAccessGetMessage msgGet = (ResourceAccessGetMessage) msg;
                    List<ResourceInfo> resInfoList = msgGet.getResourceInfo();
                    String internalId = null;
                    for(ResourceInfo resInfo: resInfoList){
                        String internalIdTemp = resInfo.getInternalId();
                        if(internalIdTemp != null && !internalIdTemp.isEmpty())
                            internalId = internalIdTemp;
                    }
                    return serializeResponse(mapper, createOkResponse(mapper, readResource(internalId)));
                }
                case HISTORY: {
                    ResourceAccessHistoryMessage msgHistory = (ResourceAccessHistoryMessage) msg;
                    List<ResourceInfo> resInfoList = msgHistory.getResourceInfo();
                    String internalId = null;
                    for(ResourceInfo resInfo: resInfoList){
                        String internalIdTemp = resInfo.getInternalId();
                        if(internalIdTemp != null && !internalIdTemp.isEmpty())
                            internalId = internalIdTemp;
                    }
                    return serializeResponse(mapper, createOkResponse(mapper, readResourceHistory(internalId)));
                }
                case SET: {
                    ResourceAccessSetMessage msgSet = (ResourceAccessSetMessage)msg;
                    List<ResourceInfo> resInfoList = msgSet.getResourceInfo();
                    String internalId = null;
                    for(ResourceInfo resInfo: resInfoList){
                        String internalIdTemp = resInfo.getInternalId();
                        if(internalIdTemp != null && !internalIdTemp.isEmpty())
                            internalId = internalIdTemp;
                    }
                    json = writeResource(internalId, msgSet.getBody());
                    if(json == null)
                        return serializeResponse(mapper, new RapPluginOkResponse());
                    else {
                        try {
                            Object returnObject = mapper.readValue(json, Object.class);
                            return serializeResponse(mapper,RapPluginOkResponse.createFromObject(returnObject));
                        } catch (IOException e) {
                            throw new RapPluginException(500, "PlatfromSpecificPlugin dit not return valid JSON string.");
                        }
                    }
                }
                case SUBSCRIBE: {
                    ResourceAccessSubscribeMessage mess = (ResourceAccessSubscribeMessage)msg;
                    List<ResourceInfo> infoList = mess.getResourceInfoList();
                    for(ResourceInfo info : infoList) {
                        subscribeResource(info.getInternalId());
                    }
                    return serializeResponse(mapper, new RapPluginOkResponse());
                }
                case UNSUBSCRIBE: {
                    ResourceAccessUnSubscribeMessage mess = (ResourceAccessUnSubscribeMessage)msg;
                    List<ResourceInfo> infoList = mess.getResourceInfoList();
                    for(ResourceInfo info : infoList) {
                        unsubscribeResource(info.getInternalId());
                    }
                    return serializeResponse(mapper, new RapPluginOkResponse());
                }
                default:
                    return serializeResponse(mapper, new RapPluginErrorResponse(501,
                            "Access type " + access.toString() + " not supported"));
            }
        } catch (RapPluginException e) {
            return serializeResponse(mapper, e.getResponse());
        }
    }

    private RapPluginResponse createOkResponse(ObjectMapper mapper, String jsonBody) {
        if(jsonBody == null)
            throw new RapPluginException(500, "RAP plugin when reading must not return null value.");
        
        Object o = null;
        
        try {
            o = mapper.readValue(jsonBody, new TypeReference<List<Observation>>() {});
        } catch (IOException e) {
            try {
                o = mapper.readValue(jsonBody, new TypeReference<Observation>() {});
            } catch (IOException e1) {
                throw new RapPluginException(500, "RAP plugin implementation did not raturn List<Observation> or Observation.");
            }
        }
        return RapPluginOkResponse.createFromObject(o);
    }

    private String serializeResponse(ObjectMapper mapper, RapPluginResponse response) {
        String json;
        try {
            json = mapper.writeValueAsString(response);
        } catch (JsonProcessingException e1) {
            log.error("Serializing RapPluginResponse should not have error", e1);
            json = "{\"responseCode\": 500, \"message\": \"Serializing RapPluginErrorResponse should not have error\"}";
        }
        return json;
    }
    
    /*
    *
    */
    private void registerPlugin(String platformId, boolean hasFilters, boolean hasNotifications) {
        try {
            RegisterPluginMessage msg = new RegisterPluginMessage(platformId, hasFilters, hasNotifications);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            byte[] json = mapper.writeValueAsBytes(msg);

            rabbitTemplate.convertAndSend(exchange.getName(), RapDefinitions.PLUGIN_REGISTRATION_KEY, json);
        } catch (Exception e ) {
            log.error("Error while registering plugin for platform " + platformId + "\n" + e);
        }
    }
    
    /*  
    *   OVERRIDE this, inserting the query to the platform with internal resource id
    */
    public abstract String readResource(String resourceId);
    
    /*  
    *   OVERRIDE this, inserting here a call to the platform with internal resource id
    *   setting the actuator value
    */
    public abstract String writeResource(String resourceId, String body);
        
    /*  
    *   OVERRIDE this, inserting the query to the platform with internal resource id
    */
    public abstract String readResourceHistory(String resourceId);
    
    /*  
    *   OVERRIDE this, inserting the subscription of the resource
    */
    public abstract void subscribeResource(String resourceId);
    
    /*  
    *   OVERRIDE this, inserting the unsubscription of the resource
    */
    public abstract void unsubscribeResource(String resourceId);
}
