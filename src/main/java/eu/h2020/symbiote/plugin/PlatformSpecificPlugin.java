/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.plugin;

import eu.h2020.symbiote.cloud.model.data.observation.Location;
import eu.h2020.symbiote.cloud.model.data.observation.Observation;
import eu.h2020.symbiote.cloud.model.data.observation.Property;
import eu.h2020.symbiote.cloud.model.data.observation.UnitOfMeasurement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

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
    public List<Observation> readResource(String resourceId) {
        List<Observation> value = new ArrayList();
        //
        // INSERT HERE: query to the platform with internal resource id
        //
        // example
        Observation obs = observationExampleValue();
        value.add(obs);
        
        return value;
    }
    
    @Override
    public void writeResource(String resourceId, String body) {
        // INSERT HERE: call to the platform with internal resource id
        // setting the actuator value
    }
    
    @Override
    public List<Observation> readResourceHistory(String resourceId) {
        List<Observation> value = new ArrayList();
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
        Location loc = new Location(15.9, 45.8, 145, "Spansko", "City of Zagreb");
        TimeZone zoneUTC = TimeZone.getTimeZone("UTC");
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateFormat.setTimeZone(zoneUTC);
        Date date = new Date();
        String timestamp = dateFormat.format(date);
        long ms = date.getTime() - 1000;
        date.setTime(ms);
        String samplet = dateFormat.format(date);
        eu.h2020.symbiote.cloud.model.data.observation.ObservationValue obsval = new eu.h2020.symbiote.cloud.model.data.observation.ObservationValue("7", new Property("Temperature", "Air temperature"), new UnitOfMeasurement("C", "degree Celsius", ""));
        ArrayList<eu.h2020.symbiote.cloud.model.data.observation.ObservationValue> obsList = new ArrayList();
        Observation obs = new Observation(sensorId, loc, timestamp, samplet , obsList);
        
        log.debug("Observation: \n" + obs.toString());
        
        return obs;
    }
}
