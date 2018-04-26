/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.h2020.symbiote.plugin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import eu.h2020.symbiote.model.cim.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.awt.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;


/**
 * @author Matteo Pardi
 */
public class PlatformSpecificPlugin extends PlatformPlugin {

    private static final Logger log = LoggerFactory.getLogger(PlatformSpecificPlugin.class);

    private static final boolean PLUGIN_PLATFORM_FILTERS_FLAG = true;
    private static final boolean PLUGIN_PLATFORM_NOTIFICATIONS_FLAG = true;

    public static final String PLUGIN_PLATFORM_ID = "openhab";
    public static final String PLUGIN_RES_ACCESS_QUEUE = "rap-platform-queue_" + PLUGIN_PLATFORM_ID;
    public static final String IP_ADDRESS = "http://146.124.108.10:8080";
    private final String USER_AGENT = "Mozilla/5.0";


    public PlatformSpecificPlugin(RabbitTemplate rabbitTemplate, TopicExchange exchange) {
        super(rabbitTemplate, exchange, PLUGIN_PLATFORM_ID, PLUGIN_PLATFORM_FILTERS_FLAG, PLUGIN_PLATFORM_NOTIFICATIONS_FLAG);
    }

    /**
     * This is called when received request for reading resource.
     *
     * You need to checked if you can read sensor data with that internal id and in case
     * of problem you can throw RapPluginException
     *
     * @param resourceId internal id of sensor as registered (resourceId =OpenHAB name)
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
            String url = IP_ADDRESS + "/rest/items/" + resourceId;
            ArrayList<String> response = sendGet(url);

            if(response.get(0).equals("404")) {
                log.warn("Got 404");
                throw new RapPluginException(HttpStatus.NOT_FOUND.value(), "Sensor not found.");
            } else {
                log.debug("response.get(1) in readResource = " + response.get(1));
                Observation obs = observationExampleValue(resourceId, response.get(1));    //TODO add item type as argument
                ObjectMapper mapper = new ObjectMapper();
                json = mapper.writeValueAsString(obs);
                log.info("Resource " + resourceId + " was found and observation is created");
                log.info("Observation " + json);
                return json;
            }
        } catch (Exception ex) {
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

            try {
                // This is example of extracting data from body
                // ObjectMapper mapper = new ObjectMapper();
                // HashMap<String,ArrayList<HashMap<String, Object>>> jsonObject =
                //         mapper.readValue(body, new TypeReference<HashMap<String,ArrayList<HashMap<String, Object>>>>() { });
                // for(Entry<String, ArrayList<HashMap<String,Object>>> capabilityEntry: jsonObject.entrySet()) {
                //     System.out.println("Found capability " + capabilityEntry.getKey());
                //     System.out.println(" There are " + capabilityEntry.getValue().size() + " parameters.");
                //     for(HashMap<String, Object> parameterMap: capabilityEntry.getValue()) {
                //         for(Entry<String, Object> parameter: parameterMap.entrySet()) {
                //             System.out.println(" paramName: " + parameter.getKey());
                //             System.out.println(" paramValueType: " + parameter.getValue().getClass().getName() + " value: " + parameter.getValue() + "\n");
                //         }
                //     }
                // }
                // System.out.println("jsonObject:  " + jsonObject);


                int responseCode = sendPost(IP_ADDRESS + "/rest/items/" + resourceId, body);

                if (responseCode == 404) {
                    throw new RapPluginException(HttpStatus.NOT_FOUND.value(), "Sensor not found.");
                } else if (responseCode == 400) {
                    throw new RapPluginException(HttpStatus.BAD_REQUEST.value(), "Command is null.");
                }
                // actuation always returns null if everything is ok
                return null;
            } catch (Exception e) {
                throw new RapPluginException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
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
//        String json;
//        try {
//            List<Observation> value = new ArrayList<>();
//            //
//            // INSERT HERE: query to the platform with internal resource id and
//            // return list of observations in JSON
//            //
//            // Here is example
//            if("isen1".equals(resourceId)) {
//                Observation obs1 = observationExampleValue();
//                Observation obs2 = observationExampleValue();
//                Observation obs3 = observationExampleValue();
//                value.add(obs1);
//                value.add(obs2);
//                value.add(obs3);
//
//                ObjectMapper mapper = new ObjectMapper();
//                json = mapper.writeValueAsString(value);
//                return json;
//            } else {
//                throw new RapPluginException(HttpStatus.NOT_FOUND.value(), "Sensor not found.");
//            }
//        } catch (RapPluginException e) {
//            throw e;
//        } catch (Exception ex) {
//            throw new RapPluginException(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex);
//        }
        return null;
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
     *   Some sample code for creating one observation
     */
    public Observation observationExampleValue (String resourceId, String itemJson) {
        //TODO get get sensor and observation information
        log.debug("1");
        String sensorType = getSensorProperty(itemJson, "type");
        log.debug("2");
        String sensorLabel = getSensorProperty(itemJson, "label");
        log.debug("3");
        String value = getSensorProperty(itemJson, "state");
        log.debug("4");

        TimeZone zoneUTC = TimeZone.getTimeZone("UTC");
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateFormat.setTimeZone(zoneUTC);
        Date date = new Date();
        String timestamp = dateFormat.format(date);
        long ms = date.getTime() - 1000;
        date.setTime(ms);
        String samplet = dateFormat.format(date);
        ArrayList<ObservationValue> obslist = new ArrayList<>();
        ObservationValue obsval;
        Observation obs = null;

        switch (sensorType) {
            case "Number":
                log.debug("Number");
                String measurmentSymbol = getSensorProperty(itemJson, "stateDescription.pattern");
                if (measurmentSymbol != null && !measurmentSymbol.isEmpty()) {
                    String mSymbol = measurmentSymbol.replaceAll("^%([,.\\d]*\\w\\s|\\w{1,2}\\s?)%?", "").trim();
                    obsval = new ObservationValue(value, new Property(sensorLabel, sensorLabel.replaceAll("\\s+", ""), null),
                            new UnitOfMeasurement(mSymbol, null, null, null));
                } else {
                    obsval = new ObservationValue(value, new Property(sensorLabel, sensorLabel.replaceAll("\\s+", ""), null), null);
                }

                obslist.add(obsval);
                obs = new Observation(resourceId, null, timestamp, samplet, obslist);
                break;
            case "Color":
                log.debug("Color");
                String[] hsbvals = value.split(",");
                int rgb = Color.HSBtoRGB(Float.parseFloat(hsbvals[0]), Float.parseFloat(hsbvals[1]), Float.parseFloat(hsbvals[3]));
                int red = (rgb>>16)&0xFF;
                int green = (rgb>>8)&0xFF;
                int blue = rgb&0xFF;
                ObservationValue obsvalR = new ObservationValue(Integer.toString(red), new Property("Red", "RedIRI", Collections.singletonList("Red intensity in RGB value")), null);
                ObservationValue obsvalG = new ObservationValue(Integer.toString(green), new Property("Green", "GreenIRI", Collections.singletonList("Green intensity in RGB value")), null);
                ObservationValue obsvalB = new ObservationValue(Integer.toString(blue), new Property("Blue", "BlueIRI", Collections.singletonList("Blue intensity in RGB value")), null);

                obslist.add(obsvalR);
                obslist.add(obsvalG);
                obslist.add(obsvalB);
                obs = new Observation(resourceId, null, timestamp, samplet, obslist);
                break;
            case "Contact":
                log.debug("Contact");

                if (value.equals("OPEN")) {
                    obsval = new ObservationValue("true", new Property("openCloseState", "http://www.symbiote-h2020.eu/ontology/bim/smartresidence#openCloseState", null), null);
                } else {
                    obsval = new ObservationValue("false", new Property("openCloseState", "http://www.symbiote-h2020.eu/ontology/bim/smartresidence#openCloseState", null), null);
                }

                obslist.add(obsval);
                obs = new Observation(resourceId, null, timestamp, samplet, obslist);
                break;
            case "DateTime":
                log.debug("DateTime");
                break;
            case "Dimmer":
                log.debug("Dimmer");

                int percent = (int) Float.parseFloat(value);
                obsval = new ObservationValue(Integer.toString(percent), new Property("Dimmer", "DimmerIRI", null),
                        new UnitOfMeasurement("%", "percent", "PercentIRI", Collections.singletonList("Brightness percent")));
                obslist.add(obsval);
                obs = new Observation(resourceId, null, timestamp, samplet, obslist);
                break;
            case "Location":
                log.debug("Location");
                String[] coord = value.split(",");
                obsval = new ObservationValue(value, new Property("Location", "LocIRI", null), null);
                obslist.add(obsval);
                obs = new Observation(resourceId, new WGS84Location(Double.parseDouble(coord[1]), Double.parseDouble(coord[0]), Double.parseDouble(coord[2]), null, null), timestamp, samplet, obslist);
                break;
            case "String":
                log.debug("String");
                obsval = new ObservationValue(value, new Property("String", "StringIRI", null), null);
                obslist.add(obsval);
                obs = new Observation(resourceId, null, timestamp, samplet, obslist);
                break;
            case "Switch":
                log.debug("Switch");
                if (value.equals("ON")) {
                    obsval = new ObservationValue("true", new Property("onOffState", "http://www.symbiote-h2020.eu/ontology/bim/smartresidence#onOffState", null), null);
                } else {
                    obsval = new ObservationValue("false", new Property("onOffState", "http://www.symbiote-h2020.eu/ontology/bim/smartresidence#onOffState", null), null);
                }
                obslist.add(obsval);
                obs = new Observation(resourceId, null, timestamp, samplet, obslist);
                break;
            case "Image":
                log.debug("Image");
                break;
            case "Player":
                log.debug("Player");
                break;
            case "Rollershutter":
                log.debug("Rollershutter");
                break;
            case "Number:":
                log.debug("Number:");
                break;
            default:
                log.debug("default");
                break;
        }

        if (obs != null)
            log.info("Observation: \n" + obs.toString());
        else {
            log.warn("Observation is not initialized");
        }

        return obs;
    }

    private ArrayList<String> sendGet(String url) throws Exception {

        ArrayList<String> response = new ArrayList<>();
//        URL obj = new URL(url);
//        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
//        con.setRequestMethod("GET");
//
//        con.setRequestProperty("User-Agent", USER_AGENT);
//
//        int responseCode = con.getResponseCode();
//        response.add(Integer.toString(responseCode));
//        log.info("Sending 'GET' request to URL : " + url);
//        log.info("Response Code : " + responseCode);
//
//        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
//        String inputLine;
//        StringBuffer responseBody = new StringBuffer();
//
//        while ((inputLine = in.readLine()) != null) {
//            responseBody.append(inputLine);
//        }
//        in.close();

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity responseEntity = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);

        response.add(String.valueOf(responseEntity.getStatusCodeValue()));
        String responseBody = (String) responseEntity.getBody();
        log.debug("responseBody = " + responseBody);
        response.add(responseBody);

        return response;

    }

    private int sendPost(String urlText, String body) throws Exception {
        log.info("\nSending 'POST' request to URL : " + urlText);
        log.info("\nRequest body : " + body);

        URL url = new URL(urlText);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        con.setRequestProperty("Content-Type", "text/plain");

        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(body);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();
        log.info("Response Code : " + responseCode);

        return responseCode;
    }

//    private static String getSensorName(String json, String sensorId) {
//        return getValueFor(json, "$.[?(@.name =~ /" + sensorId + "/)].name");
//    }
//
//    private static String getSensorState(String json, String sensorId) {
//        return getValueFor(json, "$.[?(@.name =~ /" + sensorId + "/)].state");
//    }
//
//    private static String getSensorProperty(String json, String sensorId, String property) {
//        return getValueFor(json, "$.[?(@.name =~ /" + sensorId + "/)]." + property);
//    }
//
//    private static String getMeasurementSymbol(String json, String sensorId) {
//        return getValueFor(json, "$.[?(@.name =~ /" + sensorId + "/)].stateDescription.pattern");
//    }

    private static String getSensorProperty(String json, String property) {
        String result = null;
        log.debug("getSensorProperty: " + property);
        try {
            result = getValueFor(json, "$." + property);
        } catch (Throwable t) {
            log.debug("Throwable in getSensorProperty", t);
        }
        log.debug("result = " + result);
        return result;
    }

    private static String getValueFor(String json, String regex) {
//        JSONArray array = JsonPath.read(json, regex);
//
//        return (String) array.get(0);
        return JsonPath.read(json, regex);
    }
}


// regex for uom symbol match ^%([,.\d]*\w\s|\w{1,2}\s?)%?
// java string ^%([,.\\d]*\\w\\s|\\w{1,2}\\s?)%?

// JSONPath for gettin info from ONE item: $.+field