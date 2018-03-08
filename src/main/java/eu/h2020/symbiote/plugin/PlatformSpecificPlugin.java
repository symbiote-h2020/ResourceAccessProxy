/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.plugin;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import eu.h2020.symbiote.model.cim.WGS84Location;
import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.model.cim.ObservationValue;
import eu.h2020.symbiote.model.cim.Property;
import eu.h2020.symbiote.model.cim.UnitOfMeasurement;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
public class PlatformSpecificPlugin extends PlatformPlugin {
    
    private static final Logger log = LoggerFactory.getLogger(PlatformSpecificPlugin.class);
    
    private static final boolean PLUGIN_PLATFORM_FILTERS_FLAG = true;
    private static final boolean PLUGIN_PLATFORM_NOTIFICATIONS_FLAG = true;

    public static final String PLUGIN_PLATFORM_ID = "platform_01";
    public static final String PLUGIN_RES_ACCESS_QUEUE = "rap-platform-queue_" + PLUGIN_PLATFORM_ID;   
    

    public PlatformSpecificPlugin(RabbitTemplate rabbitTemplate, TopicExchange exchange) {
        super(rabbitTemplate, exchange, PLUGIN_PLATFORM_ID, PLUGIN_PLATFORM_FILTERS_FLAG, PLUGIN_PLATFORM_NOTIFICATIONS_FLAG);
    }

    @Override
    public String readResource(String resourceId) {
        String json;
        try {
            //
            // INSERT HERE: query to the platform with internal resource id
            //
            // example
            Observation obs = observationExampleValue();
            ObjectMapper mapper = new ObjectMapper();
            json = mapper.writeValueAsString(obs);
            
            
        } catch (JsonProcessingException ex) {
            log.error(ex.getMessage(), ex);
            json = ex.getMessage();
        }
        return json;
    }
    
    // This is for actuating resource or invoking service.
    // In the case of actuation body will be JSON Object with capabilities and parameters. Actuation does not return value (it will be ignored). 
    // In the case of invoking service body will be JSON Array with parameters.
    @Override
    public String writeResource(String resourceId, String body) {
        // INSERT HERE: call to the platform with internal resource id
        String newBody = body.trim();
        if(newBody.charAt(0) == '{') {
            // actuation
            System.out.println("Actuation on resource " + resourceId + " called.");
            if("iaid1".equals(resourceId)) {
                try {
                    ObjectMapper mapper = new ObjectMapper();  
                    HashMap<String,ArrayList<HashMap<String, Object>>> jsonObject = 
                            mapper.readValue(body, new TypeReference<HashMap<String,ArrayList<HashMap<String, Object>>>>() { });
                    for(Entry<String, ArrayList<HashMap<String,Object>>> capabilityEntry: jsonObject.entrySet()) {
                        System.out.println("Found capability " + capabilityEntry.getKey());
                        System.out.println(" There are " + capabilityEntry.getValue().size() + " parameters.");
                        for(HashMap<String, Object> parameterMap: capabilityEntry.getValue()) {
                            for(Entry<String, Object> parameter: parameterMap.entrySet()) {
                                System.out.println(" paramName: " + parameter.getKey());
                                System.out.println(" paramValueType: " + parameter.getValue().getClass().getName() + " value: " + parameter.getValue() + "\n");
                            }
                        }
                    }
                    System.out.println("jsonObject:  " + jsonObject);
                    
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if(!"iaid1".equals(resourceId)) {
                throw new RapPluginException(404, "Resource with id " + resourceId + " was not found!");
            }
            return null;
        } else {
            // invoking service
            return "\"some json\"";
        }
    }
    
    @Override
    public String readResourceHistory(String resourceId) {
        String json;
        try {
            List<Observation> value = new ArrayList();
            //
            // INSERT HERE: query to the platform with internal resource id
            //
            // example
            Observation obs1 = observationExampleValue();
            Observation obs2 = observationExampleValue();
            value.add(obs1);
            value.add(obs2);

            ObjectMapper mapper = new ObjectMapper();
            json = mapper.writeValueAsString(value);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            json = ex.getMessage();
        }
        
        return json;
    }
    
    @Override
    public void subscribeResource(String resourceId) {
        // INSERT HERE: call to the platform to subscribe resource
    }
    
    @Override
    public void unsubscribeResource(String resourceId) {
        // INSERT HERE: call to the platform to unsubscribe resource
    }
    
    /* 
    *   Some sample code for observations 
    */   
    public Observation observationExampleValue () {        
        String sensorId = "symbIoTeID1";
        ArrayList<String> ldescr = new ArrayList();
        ldescr.add("City of Zagreb");
        WGS84Location loc = new WGS84Location(15.9, 45.8, 145, "Spansko", ldescr);
        TimeZone zoneUTC = TimeZone.getTimeZone("UTC");
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateFormat.setTimeZone(zoneUTC);
        Date date = new Date();
        String timestamp = dateFormat.format(date);
        long ms = date.getTime() - 1000;
        date.setTime(ms);
        String samplet = dateFormat.format(date);
        ArrayList<String> pdescr = new ArrayList();
        pdescr.add("Air temperature");
        ArrayList<String> umdescr = new ArrayList();
        umdescr.add("Temperature in degree Celsius");
        ObservationValue obsval = new ObservationValue("7", new Property("Temperature", pdescr), new UnitOfMeasurement("C", "degree Celsius", umdescr));
        ArrayList<ObservationValue> obsList = new ArrayList();
        obsList.add(obsval);
        Observation obs = new Observation(sensorId, loc, timestamp, samplet , obsList);
        
        log.debug("Observation: \n" + obs.toString());
        
        return obs;
    }
}
