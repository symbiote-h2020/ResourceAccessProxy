/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.resources.db.ResourcesRepository;
import eu.h2020.symbiote.messages.access.ResourceAccessGetMessage;
import eu.h2020.symbiote.messages.access.ResourceAccessHistoryMessage;
import eu.h2020.symbiote.messages.access.ResourceAccessMessage;
import eu.h2020.symbiote.messages.access.ResourceAccessSetMessage;
import eu.h2020.symbiote.messages.plugin.RapPluginErrorResponse;
import eu.h2020.symbiote.messages.plugin.RapPluginOkResponse;
import eu.h2020.symbiote.messages.plugin.RapPluginResponse;
import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.interfaces.ResourceAccessNotification;
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.managers.AuthorizationResult;
import eu.h2020.symbiote.messages.resourceAccessNotification.SuccessfulAccessMessageInfo;
import eu.h2020.symbiote.resources.db.ResourceInfo;
import eu.h2020.symbiote.resources.query.Comparison;
import eu.h2020.symbiote.resources.query.Filter;
import eu.h2020.symbiote.resources.query.Operator;
import eu.h2020.symbiote.resources.query.Query;
import eu.h2020.symbiote.resources.db.PlatformInfo;
import eu.h2020.symbiote.resources.db.PluginRepository;
import eu.h2020.symbiote.security.communication.payloads.SecurityRequest;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.commons.core.edm.primitivetype.EdmString;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.uri.queryoption.expression.Binary;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.Literal;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 *
 * @author Luca Tomaselli
 */
public class StorageHelper {
    private static final Logger log = LoggerFactory.getLogger(StorageHelper.class);
    
    private final int TOP_LIMIT = 100;
    
    private final ResourcesRepository resourcesRepo;
    private final PluginRepository pluginRepo;
    private final RabbitTemplate rabbitTemplate;
    private final TopicExchange exchange;
    private final String notificationUrl;
    private final AuthorizationManager authManager;

    private static final Pattern PATTERN = Pattern.compile(
            "\\p{Digit}{1,4}-\\p{Digit}{1,2}-\\p{Digit}{1,2}"
            + "T\\p{Digit}{1,2}:\\p{Digit}{1,2}(?::\\p{Digit}{1,2})?"
            + "(Z|([-+]\\p{Digit}{1,2}:\\p{Digit}{2}))?");

    public StorageHelper(ResourcesRepository resourcesRepository, PluginRepository pluginRepository,
            AuthorizationManager authMan, RabbitTemplate rabbit, int rabbitReplyTimeout, 
            TopicExchange topicExchange, String notifUrl) {
        //initSampleData();
        resourcesRepo = resourcesRepository;
        pluginRepo = pluginRepository;
        rabbitTemplate = rabbit;
        rabbitTemplate.setReplyTimeout(rabbitReplyTimeout);
        exchange = topicExchange;
        notificationUrl = notifUrl;
        authManager = authMan;
    }

    public ResourceInfo getResourceInfo(List<UriParameter> keyParams) {
        ResourceInfo resInfo = null;
        if(keyParams != null && !keyParams.isEmpty()){
            final UriParameter key = keyParams.get(0);
            String keyName = key.getName();
            String keyText = key.getText();
            //remove quote
            keyText = keyText.replaceAll("'", "");
            try {
                if (keyName.equalsIgnoreCase("id")) {
                    Optional<ResourceInfo> resInfoOptional = resourcesRepo.findById(keyText);
                    if (resInfoOptional.isPresent()) {
                        resInfo = resInfoOptional.get();
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return resInfo;
    }

    public RapPluginResponse getRelatedObject(List<ResourceInfo> resourceInfoList, Integer top, Query filterQuery) throws ODataApplicationException {
        String symbioteId = null;
        RapPluginResponse response = null;
        try {
            top = (top == null) ? TOP_LIMIT : top;
            ResourceAccessMessage msg;
            
            String pluginId = null;
            for(ResourceInfo resourceInfo: resourceInfoList){
                String symbioteIdTemp = resourceInfo.getSymbioteId();
                if(symbioteIdTemp != null && !symbioteIdTemp.isEmpty())
                    symbioteId = symbioteIdTemp;
                String pluginIdTemp = resourceInfo.getPluginId();
                if(pluginIdTemp != null && !pluginIdTemp.isEmpty())
                    pluginId = pluginIdTemp;
            }

            // set default plugin if only one plugin registered in RAP
            if(pluginId == null) {
                if(pluginRepo.count() != 1)
                    throw new ODataApplicationException("No plugin found for specified resource", 
                            HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ROOT);
                
                List<PlatformInfo> lst = pluginRepo.findAll();
                pluginId = lst.get(0).getPlatformId();
            }        

            String routingKey;
            if (top == 1) {
                msg = new ResourceAccessGetMessage(resourceInfoList);
                routingKey =  pluginId + "." + ResourceAccessMessage.AccessType.GET.toString().toLowerCase();
            } else {
                msg = new ResourceAccessHistoryMessage(resourceInfoList, top, filterQuery);
                routingKey =  pluginId + "." + ResourceAccessMessage.AccessType.HISTORY.toString().toLowerCase();
            }

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            String json = mapper.writeValueAsString(msg);

            log.debug("Message: ");
            log.debug(json);
            Object obj = rabbitTemplate.convertSendAndReceive(exchange.getName(), routingKey, json);
            response = extractRapPluginResponse(obj);
            
            if(response instanceof RapPluginOkResponse) {
                RapPluginOkResponse okResponse = (RapPluginOkResponse) response;
                if(okResponse.getBody() != null) {
                    try {
                        // need to clean up response if top 1 is used and RAP plugin does not support filtering
                        if (top == 1) {
                            Observation internalObservation;
                            if(okResponse.getBody() instanceof List) {
                                List<?> list = (List<?>) okResponse.getBody();
                                if(list.size() != 0 && list.get(0) instanceof Observation) {
                                    @SuppressWarnings("unchecked")
                                    List<Observation> observations = (List<Observation>) list;
                                    internalObservation = observations.get(0);
                                } else {
                                    throw new IllegalStateException("When reading one resource returned list must have exactly one Observation. Got: " + list.size() + ".");
                                }
                            } else if(okResponse.getBody() instanceof Observation) {
                                internalObservation = (Observation) okResponse.getBody();
                            } else if(okResponse.getBody() instanceof Map) {
                                String jsonBody = mapper.writeValueAsString(okResponse.getBody());
                                internalObservation = mapper.readValue(jsonBody, Observation.class);
                            } else {
                                throw new IllegalStateException("Unsupported body response form RAP plugin when reading one resource. Got " + 
                                        okResponse.getBody().getClass().getName());
                            }
                            Observation observation = new Observation(symbioteId, internalObservation.getLocation(), 
                                    internalObservation.getResultTime(), internalObservation.getSamplingTime(), 
                                    internalObservation.getObsValues());
                            okResponse.setBody(Arrays.asList(observation));
                        } else { 
                            // top is not 1
                            if(okResponse.getBody() instanceof List) {
                                List<?> list = (List<?>) okResponse.getBody();
                                if(list.size() != 0 && list.get(0) instanceof Observation) {
                                    @SuppressWarnings("unchecked")
                                    List<Observation> internalObservations = (List<Observation>) list;

                                    List<Observation> observationsList = new ArrayList<>();
                                    int i = 0;
                                    for (Observation o : internalObservations) {
                                        i++;
                                        if(i > top) {
                                            break;
                                        }
                                        Observation ob = new Observation(symbioteId, o.getLocation(), o.getResultTime(), o.getSamplingTime(), o.getObsValues());
                                        observationsList.add(ob);
                                    }
                                    okResponse.setBody(observationsList);
                                }
                            } else {
                                throw new IllegalStateException("Unsupported body response form RAP plugin. Expected observation list but got " + okResponse.getBody().getClass().getName());
                            }
                        }
                    } catch (Exception e) {
                        throw new ODataApplicationException("Can not parse observation list from RAP plugin.\nCause: " + e.getMessage(), 
                                HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), 
                                Locale.ROOT,
                                e);
                    }
                }
            } else {
                RapPluginErrorResponse errorResponse = (RapPluginErrorResponse) response;
                throw new ODataApplicationException(errorResponse.getMessage(), errorResponse.getResponseCode(), null);
            }
            
            return response;
        } catch (ODataApplicationException ae) {
            throw ae;
        } catch (Exception e) {
            String err = "Unable to read resource " + symbioteId;
            err += "\n Error: " + e.getMessage();
            log.error(err);
            throw new ODataApplicationException(err, HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ROOT);
        }
    }

    private RapPluginResponse extractRapPluginResponse(Object obj)
            throws ODataApplicationException, UnsupportedEncodingException {
        ObjectMapper mapper = new ObjectMapper();
        if (obj == null) {
            log.error("No response from plugin");
            throw new ODataApplicationException("No response from plugin", HttpStatusCode.GATEWAY_TIMEOUT.getStatusCode(), Locale.ROOT);
        }

        String rawObj;
        if (obj instanceof byte[]) {
            rawObj = new String((byte[]) obj, "UTF-8");
        } else if (obj instanceof String){
            rawObj = (String) obj;
        } else {
            throw new ODataApplicationException("Can not parse response from RAP plugin. Expected byte[] or String but got " + obj.getClass().getName(), 
                    HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), 
                    Locale.ROOT);
        }
        
        try {
            return mapper.readValue(rawObj, RapPluginResponse.class);
        } catch (Exception e) {
            throw new ODataApplicationException("Can not parse response from RAP to JSON.\n Cause: " + e.getMessage(),
                    HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), 
                    Locale.ROOT,
                    e);
        }
    }

    public RapPluginResponse setService(List<ResourceInfo> resourceInfoList, String requestBody) throws ODataApplicationException {
        String type = "";
        try {
            ResourceAccessMessage msg;
            String pluginId = null;
            for(ResourceInfo resourceInfo: resourceInfoList){
                pluginId = resourceInfo.getPluginId();
                type = resourceInfo.getType();
                if(pluginId != null)
                    break;
            }
            if(pluginId == null) {
                List<PlatformInfo> lst = pluginRepo.findAll();
                if(lst == null || lst.isEmpty())
                    throw new Exception("No plugin found");
                
                pluginId = lst.get(0).getPlatformId();
            }
            
            if(type.toLowerCase().startsWith("service")) {
                requestBody = "[" + requestBody + "]";
            }
            
            String routingKey = pluginId + "." + ResourceAccessMessage.AccessType.SET.toString().toLowerCase();
            
            msg = new ResourceAccessSetMessage(resourceInfoList, requestBody);

            String json = "";
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
                mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

                json = mapper.writeValueAsString(msg);
            } catch (JsonProcessingException ex) {
                log.error("JSon processing exception: " + ex.getMessage());
            }
            log.info("Message Set: " + json);
            Object o = rabbitTemplate.convertSendAndReceive(exchange.getName(), routingKey, json);
            RapPluginResponse rpResponse = extractRapPluginResponse(o);
            if(rpResponse instanceof RapPluginErrorResponse) {
                RapPluginErrorResponse errorResponse = (RapPluginErrorResponse) rpResponse;
                throw new ODataApplicationException(errorResponse.getMessage(), errorResponse.getResponseCode(), null);
            }
            return rpResponse;
        } catch (ODataApplicationException ae) {
            throw ae;        
        } catch (Exception e) {
            throw new ODataApplicationException("Internal Error", HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ROOT);
        }
    }
    
    public static Query calculateFilter(Expression expression) throws ODataApplicationException {

        if (expression instanceof Binary) {
            Expression left = ((Binary) expression).getLeftOperand();
            BinaryOperatorKind operator = ((Binary) expression).getOperator();
            Expression right = ((Binary) expression).getRightOperand();

            if (left instanceof Binary && right instanceof Binary) {
                List<Query> exprs = new ArrayList<>();
                Operator op = null;
                try {
                    op = new Operator(operator.name());
                } catch (Exception ex) {
                    throw new ODataApplicationException(ex.getMessage(), HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
                }

                Query leftQuery = calculateFilter(left);
                exprs.add(0, leftQuery);

                Query rightQuery = calculateFilter(right);
                exprs.add(1, (Query) rightQuery);

                Filter f = new Filter(op.getLop(), exprs);
                return f;
            } else if (left instanceof Member && right instanceof Literal) {
                Member member = (Member) left;
                String key = member.toString();

                Literal literal = (Literal) right;
                String value = literal.getText();
                if (literal.getType() instanceof EdmString) {
                    value = value.substring(1, value.length() - 1);
                }

                if (key.contains("resultTime") || key.contains("samplingTime")) {
                    Matcher matcher = PATTERN.matcher(value);
                    if (!matcher.matches()) {
                        throw new ODataApplicationException("Data format not correct",
                                HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
                    }
                    value = parseDate(value);

                }

                Comparison cmp;
                try {
                    cmp = new Comparison(operator.name());
                } catch (Exception ex) {
                    throw new ODataApplicationException(ex.getMessage(), HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
                }

                eu.h2020.symbiote.resources.query.Expression expr = new eu.h2020.symbiote.resources.query.Expression(key, cmp.getCmp(), value);

                return expr;
            } else {
                log.error("Not implemented");
                throw new ODataApplicationException("Not implemented", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
            }
        }
        return null;
    }

    private static String parseDate(String dateParse) throws ODataApplicationException {

        TimeZone zoneUTC = TimeZone.getTimeZone("UTC");
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateFormat.setTimeZone(zoneUTC);
        DateFormat dateFormat1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        dateFormat1.setTimeZone(zoneUTC);
        DateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmXXX");
        dateFormat2.setTimeZone(zoneUTC);
        DateFormat dateFormat3 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        dateFormat3.setTimeZone(zoneUTC);

        dateParse = dateParse.replaceAll("Z", "+00:00");
        Date date = null;
        String parsedData;
        try {
            date = dateFormat3.parse(dateParse);
        } catch (ParseException e3) {
            try {
                date = dateFormat2.parse(dateParse);
            } catch (ParseException e2) {
                try {
                    date = dateFormat.parse(dateParse);
                } catch (ParseException e) {
                    try {
                        date = dateFormat1.parse(dateParse);
                    } catch (ParseException e1) {

                    }
                }
            }
        }

        if (date == null) {
            log.error("Incorrect data format");
            throw new ODataApplicationException("Data format not correct",
                    HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
        }

        parsedData = dateFormat.format(date);
        return parsedData;
    }

    public List<ResourceInfo> getResourceInfoList(List<String> typeNameList, List<UriParameter> keyPredicates) throws ODataApplicationException {
        Boolean noResourceFound = true;
        List<ResourceInfo> resourceInfoList = new ArrayList<>();
        for(int i = 0; i< typeNameList.size(); i++){
            ResourceInfo resInfo = new ResourceInfo();
            resInfo.setType(typeNameList.get(i));
            if(i < keyPredicates.size()){
                UriParameter key = keyPredicates.get(i);
                String keyName = key.getName();
                String keyText = key.getText();
                //remove quote
                keyText = keyText.replaceAll("'", "");

                try {
                    if (keyName.equalsIgnoreCase("id")) {
                        resInfo.setSymbioteId(keyText);
                        Optional<ResourceInfo> resInfoOptional = resourcesRepo.findById(keyText);
                        if (resInfoOptional.isPresent()) {
                            noResourceFound = false;
                            resInfo.setInternalId(resInfoOptional.get().getInternalId());
                            resInfo.setPluginId(resInfoOptional.get().getPluginId());
                        }
                    }
                } catch (Exception e) {
                }
            }
            resourceInfoList.add(resInfo);
        }
        if(noResourceFound) {
            log.error("No entity found with id specified in request");
            throw new ODataApplicationException("Entity not found",
                    HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ROOT);
        }
        return resourceInfoList;
    }
    
    public boolean checkAccessPolicies(ODataRequest request, String resourceId) throws Exception {
        log.debug("Checking access policies for resource " + resourceId);
        Map<String,List<String>> headers = request.getAllHeaders();
        Map<String, String> secHdrs = new HashMap<>();
        for(String key : headers.keySet()) {
            secHdrs.put(key, request.getHeader(key));
        }
        log.debug("Headers: " + secHdrs);
        SecurityRequest securityReq = new SecurityRequest(secHdrs);

        AuthorizationResult result = authManager.checkResourceUrlRequest(resourceId, securityReq);
        log.info(result.getMessage());
        
        return result.isValidated();
    }
    
    public void sendSuccessfulAccessMessage(String symbioteId, String accessType){
        try{
            String jsonNotificationMessage = null;
            if(accessType == null || accessType.isEmpty())
                accessType = SuccessfulAccessMessageInfo.AccessType.NORMAL.name();
            ObjectMapper map = new ObjectMapper();
            map.configure(SerializationFeature.INDENT_OUTPUT, true);
            map.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

            List<Date> dateList = new ArrayList<>();
            dateList.add(new Date());
            ResourceAccessNotification notificationMessage = new ResourceAccessNotification(authManager, notificationUrl);

            try{
                notificationMessage.SetSuccessfulAttempts(symbioteId, dateList, accessType);
                jsonNotificationMessage = map.writeValueAsString(notificationMessage);
            } catch (JsonProcessingException e) {
                log.error(e.toString(), e);
            }
            notificationMessage.SendSuccessfulAttemptsMessage(jsonNotificationMessage);
        }catch(Exception e){
            log.error("Error to send SetSuccessfulAttempts to CRAM");
            log.error(e.getMessage(),e);
        }
    }
}
