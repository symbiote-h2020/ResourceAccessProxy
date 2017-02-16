/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.plugin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.interfaces.ResourceAccessRestController;
import eu.h2020.symbiote.messages.RegisterPluginMessage;
import eu.h2020.symbiote.messages.ResourceAccessMessage;
import eu.h2020.symbiote.messages.ResourceAccessSetMessage;
import eu.h2020.symbiote.model.data.Observation;
import eu.h2020.symbiote.model.data.ObservationValue;
import eu.h2020.symbiote.resources.RapDefinitions;
import eu.h2020.symbiote.resources.ResourceInfo;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
public class PlatformSpecificPlugin {
    private static final Logger log = LoggerFactory.getLogger(ResourceAccessRestController.class);

    private static final String PLUGIN_PLATFORM_ID = "platform_01";

    private static final String PLUGIN_PLATFORM_NAME = "platform_name_01";
    
    //public static final String PLUGIN_RES_ACCESS_QUEUE = PLUGIN_PLATFORM_ID + "-rap-platform-queue";
    public static final String PLUGIN_RES_ACCESS_QUEUE = "rap-platform-queue";
    //public static final String[] PLUGIN_RES_ACCESS_KEYS = {PLUGIN_PLATFORM_ID + ".get", 
    //                                                       PLUGIN_PLATFORM_ID + ".set", 
    //                                                       PLUGIN_PLATFORM_ID + ".history" ,
    //                                                       PLUGIN_PLATFORM_ID + ".subscribe",
    //                                                       PLUGIN_PLATFORM_ID + ".unsubscribe"};
    
    public static final String[] PLUGIN_RES_ACCESS_KEYS = {"get", 
                                                           "set", 
                                                           "history" ,
                                                           "subscribe",
                                                           "unsubscribe"};
    
    private final RabbitTemplate rabbitTemplate;
    private final TopicExchange exchange;
    
    
    public PlatformSpecificPlugin(RabbitTemplate rabbitTemplate, TopicExchange exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange; 
        registerPlugin();
    }
    
    public static String getPluginPlatformId() {
        return PLUGIN_PLATFORM_ID;
    }
    
    
    public Observation readResource(String resourceId) {
        Observation value;
        //
        // INSERT HERE: query to the platform with internal resource id
        //
        value = Observation.observationExampleValue();
        
        return value;
    }
    
    public void writeResource(String resourceId, ObservationValue value) {
        // TODO    
    }
    
    public List<Observation> readResourceHistory(String resourceId) {
        List<Observation> value = null;
        // TODO
        return value;
    }
    
    public String receiveMessage(String message) {
        String json = "";
        try {            
            ObjectMapper mapper = new ObjectMapper();
            ResourceAccessMessage msg = mapper.readValue(message, ResourceAccessMessage.class);
            ResourceInfo info = msg.getResourceInfo();
            ResourceAccessMessage.AccessType access = msg.getAccessType();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            switch(access) {
                case GET:                    
                    Observation observation = readResource(info.getPlatformResourceId());
                    json = mapper.writeValueAsString(observation);
                    break;
                case HISTORY:
                    List<Observation> observationLst = readResourceHistory(info.getPlatformResourceId());
                    json = mapper.writeValueAsString(observationLst);
                    throw new Exception("Access type " + access.toString() + " not yet supported");
                case SET:
                    ResourceAccessSetMessage mess = (ResourceAccessSetMessage)msg;
                    writeResource(info.getPlatformResourceId(), mess.getValue());
                    throw new Exception("Access type " + access.toString() + " not yet supported");
            }
        } catch (Exception e) {
            log.error("Error while processing message:\n" + message + "\n" + e);
        }
        return json;
    }
    
    private void registerPlugin() {
        try {
            RegisterPluginMessage msg = new RegisterPluginMessage(PLUGIN_PLATFORM_ID, PLUGIN_PLATFORM_NAME);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            String json = mapper.writeValueAsString(msg);

            rabbitTemplate.convertAndSend(exchange.getName(), RapDefinitions.PLUGIN_REGISTRATION_KEY, json);
        } catch (Exception e ) {
            log.error("Error while registering plugin for platform " + PLUGIN_PLATFORM_ID + "\n" + e);
        }
    }
}
