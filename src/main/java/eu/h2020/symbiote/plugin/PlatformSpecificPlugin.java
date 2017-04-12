/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.plugin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.cloud.model.data.observation.ObservationValue;
import eu.h2020.symbiote.cloud.model.data.parameter.InputParameter;
import eu.h2020.symbiote.interfaces.ResourceAccessRestController;
import eu.h2020.symbiote.messages.RegisterPluginMessage;
import eu.h2020.symbiote.messages.ResourceAccessGetMessage;
import eu.h2020.symbiote.messages.ResourceAccessHistoryMessage;
import eu.h2020.symbiote.messages.ResourceAccessMessage;
import eu.h2020.symbiote.messages.ResourceAccessSetMessage;
import eu.h2020.symbiote.cloud.model.data.observation.Observation;
import eu.h2020.symbiote.resources.RapDefinitions;
import eu.h2020.symbiote.resources.ResourceInfo;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import eu.h2020.symbiote.cloud.model.data.observation.Location;
import eu.h2020.symbiote.cloud.model.data.observation.Property;
import eu.h2020.symbiote.cloud.model.data.observation.UnitOfMeasurement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
public class PlatformSpecificPlugin {
    private static final Logger log = LoggerFactory.getLogger(ResourceAccessRestController.class);

    private static final String PLUGIN_PLATFORM_ID = "platform_01";
    private static final boolean PLUGIN_PLATFORM_FILTERS_FLAG = true;
    private static final boolean PLUGIN_PLATFORM_NOTIFICATIONS_FLAG = true;
    public static final String PLUGIN_RES_ACCESS_QUEUE = "rap-platform-queue";    
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
        // example
        value = observationExampleValue();
        
        return value;
    }
    
    public void writeResource(String resourceId, InputParameter value) {
        // INSERT HERE: call to the platform with internal resource id
        // setting the actuator value
    }
    
    public List<Observation> readResourceHistory(String resourceId) {
        List<Observation> value = null;
        //
        // INSERT HERE: query to the platform with internal resource id
        //
        // example
        Observation obs1 = observationExampleValue();
        Observation obs2 = observationExampleValue();
        value.add(obs1);
        value.add(obs2);
        
        return value;
    }
    
    public String receiveMessage(String message) {
        String json = "";
        try {            
            ResourceInfo info;
            ObjectMapper mapper = new ObjectMapper();
            ResourceAccessMessage msg = mapper.readValue(message, ResourceAccessMessage.class);
            ResourceAccessMessage.AccessType access = msg.getAccessType();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            switch(access) {
                case GET:          
                    ResourceAccessGetMessage msgGet = (ResourceAccessGetMessage) msg;
                    info = msgGet.getResourceInfo();
                    Observation observation = readResource(info.getInternalId());
                    json = mapper.writeValueAsString(observation);
                    break;
                case HISTORY:
                    ResourceAccessHistoryMessage msgHistory = (ResourceAccessHistoryMessage) msg;
                    info = msgHistory.getResourceInfo();
                    List<Observation> observationLst = readResourceHistory(info.getInternalId());
                    json = mapper.writeValueAsString(observationLst);                    
                case SET:
                    ResourceAccessSetMessage mess = (ResourceAccessSetMessage)msg;
                    info = mess.getResourceInfo();
                    writeResource(info.getInternalId(), mess.getValue());                    
                case SUBSCRIBE:
                    // insert here subscription to resource
                    break;
                case UNSUBSCRIBE:
                    // insert here unsubscription to resource
                    break;
                default:
                    throw new Exception("Access type " + access.toString() + " not supported");
            }
        } catch (Exception e) {
            log.error("Error while processing message:\n" + message + "\n" + e);
        }
        return json;
    }
    
    private void registerPlugin() {
        try {
            RegisterPluginMessage msg = new RegisterPluginMessage(PLUGIN_PLATFORM_ID, PLUGIN_PLATFORM_FILTERS_FLAG, PLUGIN_PLATFORM_NOTIFICATIONS_FLAG);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            String json = mapper.writeValueAsString(msg);

            rabbitTemplate.convertAndSend(exchange.getName(), RapDefinitions.PLUGIN_REGISTRATION_KEY, json);
        } catch (Exception e ) {
            log.error("Error while registering plugin for platform " + PLUGIN_PLATFORM_ID + "\n" + e);
        }
    }
        
    public static Observation observationExampleValue () {        
        String sensorId = "symbIoTeID1";
        Location loc = new Location(15.9, 45.8, 145, "Spansko", "City of Zagreb");
        TimeZone zoneUTC = TimeZone.getTimeZone("UTC");
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateFormat.setTimeZone(zoneUTC);
        Date date = new Date();
        String timestamp = dateFormat.format(date);
        long ms = date.getTime() - 1000;
        date.setTime(ms);
        String samplet = dateFormat.format(date);
        ObservationValue obsval = new ObservationValue("7", new Property("Temperature", "Air temperature"), new UnitOfMeasurement("C", "degree Celsius", ""));
        Observation obs = new Observation(sensorId, loc, timestamp, samplet , obsval);
        
        log.debug("Observation: \n" + obs.toString());
        
        return obs;
    }
}
