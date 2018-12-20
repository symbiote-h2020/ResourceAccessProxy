/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.plugin;

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
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * @author Matteo Pardi
 */
public class PlatformSpecificPlugin extends PlatformPlugin {

    private static final Logger log = LoggerFactory.getLogger(PlatformSpecificPlugin.class);

    private static final boolean PLUGIN_PLATFORM_FILTERS_FLAG = true;
    private static final boolean PLUGIN_PLATFORM_NOTIFICATIONS_FLAG = true;

    public static final String PLUGIN_PLATFORM_ID = "platform_01";
    public static final String PLUGIN_RES_ACCESS_QUEUE = "rap-platform-queue_" + PLUGIN_PLATFORM_ID;

    private Set<String> subscriptionSet = Collections.synchronizedSet(new HashSet<>());
    private volatile Thread subscriptionThread;

    public PlatformSpecificPlugin(RabbitTemplate rabbitTemplate, TopicExchange exchange) {
        super(rabbitTemplate, exchange, PLUGIN_PLATFORM_ID, PLUGIN_PLATFORM_FILTERS_FLAG, PLUGIN_PLATFORM_NOTIFICATIONS_FLAG);
    }

    /**
     * This is called when received request for reading resource.
     *
     * You need to checked if you can read sensor data with that internal id and in case
     * of problem you can throw RapPluginException
     *
     * @param resourceId internal id of sensor as registered
     *
     * @return string that contains JSON of one Observation
     *
     * @throws RapPluginException can be thrown when something went wrong. It has return code
     * that can be returned to consumer.
     */
    @Override
    public String readResource(String resourceId) {
        String json;
        try {
            //
            // INSERT HERE: query to the platform with internal resource id
            //
            // example
            if("isen1".equals(resourceId)) {
                Observation obs = observationExampleValue();
                ObjectMapper mapper = new ObjectMapper();
                json = mapper.writeValueAsString(obs);
                return json;
            } else {
                throw new RapPluginException(HttpStatus.NOT_FOUND.value(), "Sensor not found.");
            }
        } catch (JsonProcessingException ex) {
            throw new RapPluginException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Can not convert to JSON.", ex);
        }
    }

    /**
     * This method is called when actuating resource or invoking service is requested.
     *
     * In the case of actuation
     * body will be JSON Object with capabilities and parameters.
     * Actuation does not return value (it will be ignored).
     * Example of body:
     * <pre>
     * {
     *   "SomeCapabililty" : [
     *     {
     *       "param1" : true
     *     },
     *     {
     *       "param2" : "some text"
     *     },
     *     ...
     *   ]
     * }
     * </pre>
     *
     * In the case of invoking service body will be JSON Array with parameters.
     * Example of body:
     * <pre>
     * [
     *   {
     *     "inputParam1" : false
     *   },
     *   {
     *     "inputParam2":"some text"
     *   },
     *   ...
     * ]
     * </pre>
     *
     * @param body JSON input depending on what is called (actuation or invoking service)
     *
     * @return returns JSON string that will be returned as response
     *
     * @throws RapPluginException can be thrown when something went wrong. It has return code
     * that can be returned to consumer.
     */
    @Override
    public String writeResource(String resourceId, String body) {
        // INSERT HERE: call to the platform with internal resource id
        String newBody = body.trim();
        if(newBody.charAt(0) == '{') {
            // actuation
            System.out.println("Actuation on resource " + resourceId + " called.");
            if("iaid1".equals(resourceId)) {
                try {
                    // This is example of extracting data from body
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
                    // actuation always returns null if everything is ok
                    return null;
                } catch (IOException e) {
                    throw new RapPluginException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
                }
            } else {
                throw new RapPluginException(HttpStatus.NOT_FOUND.value(), "Sensor not found.");
            }
        } else {
            // invoking service
            System.out.println("Invoking service " + resourceId + ".");
            if("isrid1".equals(resourceId)) {
                try {
                    // extracting service parameters
                    ObjectMapper mapper = new ObjectMapper();
                    ArrayList<HashMap<String, Object>> jsonObject =
                            mapper.readValue(body, new TypeReference<ArrayList<HashMap<String, Object>>>() { });
                    for(HashMap<String,Object> parameters: jsonObject) {
                        System.out.println("Found " + parameters.size() + " parameter(s).");
                        for(Entry<String, Object> parameter: parameters.entrySet()) {
                            System.out.println(" paramName: " + parameter.getKey());
                            System.out.println(" paramValueType: " + parameter.getValue().getClass().getName() + " value: " + parameter.getValue() + "\n");
                        }
                    }
                    System.out.println("jsonObject:  " + jsonObject);
                    // Service can return either null if nothing to return or some JSON
                    // example
                    return "\"some json\"";
                } catch (IOException e) {
                    throw new RapPluginException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
                }
            } else {
                throw new RapPluginException(HttpStatus.NOT_FOUND.value(), "Service not found!");
            }
        }
    }

    /**
     * This is called when received request for reading resource history.
     *
     * You need to checked if you can read sensor data with that internal id and in case
     * of problem you can throw RapPluginException.
     *
     * Default is to return maximum of 100 observations.
     *
     * @param resourceId internal id of sensor as registered
     *
     * @return string that contains JSON with array of Observations (maximum 100)
     *
     * @throws RapPluginException can be thrown when something went wrong. It has return code
     * that can be returned to consumer.
     */
    @Override
    public String readResourceHistory(String resourceId) {
        String json;
        try {
            List<Observation> value = new ArrayList<>();
            //
            // INSERT HERE: query to the platform with internal resource id and
            // return list of observations in JSON
            //
            // Here is example
            if("isen1".equals(resourceId)) {
                Observation obs1 = observationExampleValue();
                Observation obs2 = observationExampleValue();
                Observation obs3 = observationExampleValue();
                value.add(obs1);
                value.add(obs2);
                value.add(obs3);

                ObjectMapper mapper = new ObjectMapper();
                json = mapper.writeValueAsString(value);
                return json;
            } else {
                throw new RapPluginException(HttpStatus.NOT_FOUND.value(), "Sensor not found.");
            }
        } catch (RapPluginException e) {
            throw e;
        } catch (Exception ex) {
            throw new RapPluginException(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex);
        }
    }

    /**
     * Called when someone wants to subscribe
     */
    @Override
    public void subscribeResource(String resourceId) {
        synchronized (subscriptionSet) {
            subscriptionSet.add(resourceId);
            if(subscriptionThread == null) {
                subscriptionThread = new Thread(() -> {
                    while(subscriptionThread != null) {
                        sendPush();
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
                subscriptionThread.start();
            }
        }
    }

    private void sendPush() {
        ObjectMapper mapper = new ObjectMapper();
        synchronized (subscriptionSet) {
            for(String id: subscriptionSet) {
                Observation observation = observationExampleValue(id);
                String json;
                try {
                    json = mapper.writeValueAsString(observation);
                    sendSubscriptionData(json);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Called when someone wants to unsubscribe
     */
    @Override
    public void unsubscribeResource(String resourceId) {
        synchronized (subscriptionSet) {
            subscriptionSet.remove(resourceId);
            if(subscriptionSet.isEmpty())
                subscriptionThread = null;
        }
    }

    /*
     *   Some sample code for creating one observation
     */
    public Observation observationExampleValue () {
        return observationExampleValue("symbIoTeID1");
    }
    
    /*
     *   Some sample code for creating one observation
     */
    public Observation observationExampleValue (String sensorId) {
        ArrayList<String> ldescr = new ArrayList<>();
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
        ArrayList<String> pdescr = new ArrayList<>();
        pdescr.add("Air temperature");
        ArrayList<String> umdescr = new ArrayList<>();
        umdescr.add("Temperature in degree Celsius");
        ObservationValue obsval = new ObservationValue("7", new Property("Temperature", "TempIRI", pdescr),
                new UnitOfMeasurement("C", "degree Celsius", "CelsiusIRI", umdescr));
        ArrayList<ObservationValue> obsList = new ArrayList<>();
        obsList.add(obsval);
        Observation obs = new Observation(sensorId, loc, timestamp, samplet , obsList);

        log.debug("Observation: \n" + obs.toString());

        return obs;
    }
}
