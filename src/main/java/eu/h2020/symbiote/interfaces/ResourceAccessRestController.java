/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.interfaces;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import eu.h2020.symbiote.resources.db.ResourcesRepository;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.security.handler.IComponentSecurityHandler;
import eu.h2020.symbiote.exceptions.*;
import eu.h2020.symbiote.interfaces.conditions.NBInterfaceRESTCondition;
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.managers.AuthorizationResult;
import eu.h2020.symbiote.managers.ServiceResponseResult;
import eu.h2020.symbiote.messages.access.ResourceAccessGetMessage;
import eu.h2020.symbiote.messages.access.ResourceAccessHistoryMessage;
import eu.h2020.symbiote.messages.access.ResourceAccessMessage.AccessType;
import eu.h2020.symbiote.messages.access.ResourceAccessSetMessage;
import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.messages.resourceAccessNotification.SuccessfulAccessMessageInfo;
import eu.h2020.symbiote.resources.RapDefinitions;
import eu.h2020.symbiote.resources.db.AccessPolicy;
import eu.h2020.symbiote.resources.db.AccessPolicyRepository;
import eu.h2020.symbiote.resources.db.PlatformInfo;
import eu.h2020.symbiote.resources.db.PluginRepository;
import eu.h2020.symbiote.resources.db.ResourceInfo;
import eu.h2020.symbiote.resources.query.Query;
import eu.h2020.symbiote.security.accesspolicies.IAccessPolicy;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import eu.h2020.symbiote.security.communication.payloads.SecurityRequest;
import java.util.*;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import static eu.h2020.symbiote.resources.RapDefinitions.JSON_OBJECT_TYPE_FIELD_NAME;


/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 *
 * REST controller to receive resource access requests
 * 
 */
@Conditional(NBInterfaceRESTCondition.class)
@RestController
public class ResourceAccessRestController {

    private static final Logger log = LoggerFactory.getLogger(ResourceAccessRestController.class);

    private final int TOP_LIMIT = 100;
    public final String SECURITY_RESPONSE_HEADER = "x-auth-response";
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    @Qualifier(RapDefinitions.PLUGIN_EXCHANGE_OUT)
    private TopicExchange exchange;
    
    @Autowired
    private ResourcesRepository resourcesRepo;
    
    @Autowired
    private PluginRepository pluginRepo;
    
    @Autowired
    private AuthorizationManager authManager; 
    
    @Autowired
    private AccessPolicyRepository accessPolicyRepo;
   
    @Value("${symbiote.rap.cram.url}") 
    private String notificationUrl;
        
    @Value("${rabbit.replyTimeout}")
    private int rabbitReplyTimeout;
    
    @PostConstruct
    public void init() {
        rabbitTemplate.setReplyTimeout(rabbitReplyTimeout);
    }

    /**
     * Used to retrieve the current value of a registered resource
     * 
     * 
     * @param resourceId    the id of the resource to query 
     * @param request 
     * @return  the current value read from the resource
     * @throws java.lang.Exception
     */
    @RequestMapping(value="/rap/Sensor/{resourceId}", method=RequestMethod.GET)
    public ResponseEntity<Object> readResource(@PathVariable String resourceId, HttpServletRequest request) throws Exception {
        Exception e = null;
        String path = "/rap/Sensor/"+resourceId;
        HttpStatus httpStatus = null;
        HttpHeaders responseHeaders = new HttpHeaders();
        //Observation ob = null;
        Object response = null;
        try {
            log.info("Received read resource request for ID = " + resourceId);       
            
            // checking access policies
            if(!checkAccessPolicies(request, resourceId))
                throw new Exception("Auhtorization refused");
            
            ResourceInfo info = getResourceInfo(resourceId);
            List<ResourceInfo> infoList = new ArrayList();
            infoList.add(info);
            ResourceAccessGetMessage msg = new ResourceAccessGetMessage(infoList);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(Include.NON_EMPTY);
            String json = mapper.writeValueAsString(msg);
            
            String pluginId = info.getPluginId();
            if(pluginId == null) {
                List<PlatformInfo> lst = pluginRepo.findAll();
                if(lst == null || lst.isEmpty())
                    throw new Exception("No plugin found");
                
                pluginId = lst.get(0).getPlatformId();
            }
            String routingKey =  pluginId + "." + AccessType.GET.toString().toLowerCase();
            Object obj = rabbitTemplate.convertSendAndReceive(exchange.getName(), routingKey, json);
            if(obj == null)
                throw new Exception("No response from plugin");

            String resp = (obj instanceof byte[]) ? new String((byte[]) obj, "UTF-8") : obj.toString();
            // checking if plugin response is a valid json
            try {
                JsonNode jsonObj = mapper.readTree(resp.toString());
                if(!jsonObj.has(JSON_OBJECT_TYPE_FIELD_NAME)) {
                    log.error("Field " + JSON_OBJECT_TYPE_FIELD_NAME + " is mandatory in plugin response");
                //    throw new Exception("Field " + JSON_OBJECT_TYPE_FIELD_NAME + " is mandatory in plugin response");
                }
                response = jsonObj;
            } catch (Exception ex){
                log.error("Response from plugin is not a valid json", ex);
                throw new Exception("Response from plugin is not a valid json");
            }
            sendSuccessfulAccessMessage(resourceId, SuccessfulAccessMessageInfo.AccessType.NORMAL.name());
        
            httpStatus = HttpStatus.OK;
            
        } catch(EntityNotFoundException enf) {
            e = enf;
            log.error(e.toString(), e);
            httpStatus = HttpStatus.NOT_FOUND;
        } catch (Exception ex) {
            e = ex;
            String err = "Unable to read resource with id: " + resourceId;
            log.error(err + "\n" + e.getMessage(),e);
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }     
        
        if(!httpStatus.equals(HttpStatus.OK)){
            if(e == null)
                e = new GenericException("Generic error");
            sendFailMessage(path, resourceId, e);
        }
        
        ServiceResponseResult serResponse = authManager.generateServiceResponse();
        if(serResponse.isCreatedSuccessfully()) {
            responseHeaders.set(SECURITY_RESPONSE_HEADER, serResponse.getServiceResponse());
        }
        
        return new ResponseEntity<>(response , responseHeaders, httpStatus);
    }
    
    /**
     * Used to retrieve the history values of a registered resource
     * 
     * 
     * @param resourceId    the id of the resource to query 
     * @param request 
     * @return  the current value read from the resource
     */
    @RequestMapping(value="/rap/Sensor/{resourceId}/history", method=RequestMethod.GET)
    public ResponseEntity<Object> readResourceHistory(@PathVariable String resourceId, HttpServletRequest request) throws Exception {
        Exception e = null;
        String path = "/rap/Sensor/"+resourceId+"/history";
        HttpStatus httpStatus = null;
        HttpHeaders responseHeaders = new HttpHeaders();
        //List<Observation> observationsList = null;
        Object response = null;
        try {
            log.info("Received read resource request for ID = " + resourceId);

            if(!checkAccessPolicies(request, resourceId))
                throw new Exception("Auhtorization refused");
        
            ResourceInfo info = getResourceInfo(resourceId);
            List<ResourceInfo> infoList = new ArrayList();
            infoList.add(info);
            Query q = null;
            ResourceAccessHistoryMessage msg = new ResourceAccessHistoryMessage(infoList, TOP_LIMIT, q);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(Include.NON_EMPTY);
            String json = mapper.writeValueAsString(msg);
            
            String pluginId = info.getPluginId();
            if(pluginId == null) {
                List<PlatformInfo> lst = pluginRepo.findAll();
                if(lst == null || lst.isEmpty())
                    throw new Exception("No plugin found");
                
                pluginId = lst.get(0).getPlatformId();
            }
            String routingKey =  pluginId + "." + AccessType.HISTORY.toString().toLowerCase();
            Object obj = rabbitTemplate.convertSendAndReceive(exchange.getName(), routingKey, json);
            if(obj== null)
                throw new Exception("No response from plugin");

            String resp = (obj instanceof byte[]) ? new String((byte[]) obj, "UTF-8") : obj.toString();
            // checking if plugin response is a valid json
            try {
                JsonNode jsonObj = mapper.readTree(resp.toString());
                if(!jsonObj.has(JSON_OBJECT_TYPE_FIELD_NAME)) {
                    log.error("Field " + JSON_OBJECT_TYPE_FIELD_NAME + " is mandatory in plugin response");
                    //    throw new Exception("Field " + JSON_OBJECT_TYPE_FIELD_NAME + " is mandatory in plugin response");
                }
                response = jsonObj;
            } catch (Exception ex){
                log.error("Response from plugin is not a valid json", ex);
                throw new Exception("Response from plugin is not a valid json");
            }
            sendSuccessfulAccessMessage(resourceId, SuccessfulAccessMessageInfo.AccessType.NORMAL.name());

            httpStatus = HttpStatus.OK;
            
        } catch(EntityNotFoundException enf) {
            e = enf;
            log.error(e.toString(),e);
            httpStatus = HttpStatus.NOT_FOUND;
        } catch (Exception ex) {
            e = ex;
            String err = "Unable to read resource with id: " + resourceId;
            log.error(err + "\n" + e.getMessage(),e);
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }     
        
        if(!httpStatus.equals(HttpStatus.OK)){
            if(e == null)
                e = new GenericException("Generic error");
            sendFailMessage(path, resourceId, e);
        }
        
        ServiceResponseResult serResponse = authManager.generateServiceResponse();
        if(serResponse.isCreatedSuccessfully()) {
            responseHeaders.set(SECURITY_RESPONSE_HEADER, serResponse.getServiceResponse());
        }
        
        return new ResponseEntity<>(response, responseHeaders, httpStatus);
    }
    
    /**
     * Used to write a value in a registered resource
     * 
     * 
     * @param resourceId    the id of the resource to query 
     * @param body 
     * @param request 
     * @return              the http response code
     */
    @RequestMapping(value={"/rap/Actuator/{resourceId}","/rap/Service/{resourceId}"}, method=RequestMethod.POST)
    public ResponseEntity<?> writeResource(@PathVariable String resourceId, @RequestBody String body, HttpServletRequest request) throws Exception{
        Exception e = null;
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        HttpStatus httpStatus = null;
        Object response = null;
        HttpHeaders responseHeaders = new HttpHeaders();
        try {
            log.info("Received write resource request for ID = " + resourceId + " with values " + body);
            
            if(!checkAccessPolicies(request, resourceId))
                throw new Exception("Auhtorization refused");

            ResourceInfo info = getResourceInfo(resourceId);
            List<ResourceInfo> infoList = new ArrayList();
            infoList.add(info);
            ResourceAccessSetMessage msg = new ResourceAccessSetMessage(infoList, body);            
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(Include.NON_EMPTY);
            String json = mapper.writeValueAsString(msg);
            
            String pluginId = info.getPluginId();
            if(pluginId == null) {
                List<PlatformInfo> lst = pluginRepo.findAll();
                if(lst == null || lst.isEmpty())
                    throw new Exception("No plugin found");
                
                pluginId = lst.get(0).getPlatformId();
            }
            String routingKey =  pluginId + "." + AccessType.SET.toString().toLowerCase();
            Object obj = rabbitTemplate.convertSendAndReceive(exchange.getName(), routingKey, json);
            if(obj != null){
                String resp = (obj instanceof byte[]) ? new String((byte[]) obj, "UTF-8") : obj.toString();
                // checking if plugin response is a valid json
                try {
                    JsonNode jsonObj = mapper.readTree(resp.toString());
                    if(!jsonObj.has(JSON_OBJECT_TYPE_FIELD_NAME)) {
                        log.error("Field " + JSON_OBJECT_TYPE_FIELD_NAME + " is mandatory in plugin response");
                        //    throw new Exception("Field " + JSON_OBJECT_TYPE_FIELD_NAME + " is mandatory in plugin response");
                    }
                    response = jsonObj;
                } catch (Exception ex){
                    log.error("Response from plugin is not a valid json", ex);
                    throw new Exception("Response from plugin is not a valid json");
                }
            }
            sendSuccessfulAccessMessage(resourceId, SuccessfulAccessMessageInfo.AccessType.NORMAL.name());
            httpStatus = HttpStatus.OK;
            
        } catch(EntityNotFoundException enf) {
            e = enf;
            log.error(e.toString(),e);
            httpStatus = HttpStatus.NOT_FOUND;
        } catch (Exception ex) {
            e = ex;
            String err = "Unable to read resource with id: " + resourceId;
            log.error(err + "\n" + e.getMessage(),e);
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }     
        
        if(!httpStatus.equals(HttpStatus.OK)){
            if(e == null)
                e = new GenericException("Generic error");
            sendFailMessage(path, resourceId, e);
        }
        
        ServiceResponseResult serResponse = authManager.generateServiceResponse();
        if(serResponse.isCreatedSuccessfully()) {
            responseHeaders.set(SECURITY_RESPONSE_HEADER, serResponse.getServiceResponse());
        }
        
        return new ResponseEntity<>(response, responseHeaders, httpStatus);
    }
    
    private ResourceInfo getResourceInfo(String resourceId) {
        Optional<ResourceInfo> resInfo = resourcesRepo.findById(resourceId);
        if(!resInfo.isPresent())
            throw new EntityNotFoundException("Resource " + resourceId + " not found");
        
        return resInfo.get();
    }

    public boolean checkAccessPolicies(HttpServletRequest request, String resourceId) throws Exception {
        Map<String, String> secHdrs = new HashMap();
        Enumeration<String> headerNames = request.getHeaderNames();

        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String header = headerNames.nextElement();
                secHdrs.put(header, request.getHeader(header));
            }
        }
        log.info("secHeaders: " + secHdrs);
        SecurityRequest securityReq = new SecurityRequest(secHdrs);

        AuthorizationResult result = authManager.checkResourceUrlRequest(resourceId, securityReq);
        log.info(result.getMessage());

        return result.isValidated();
    }
    
    private String sendFailMessage(String path, String symbioteId, Exception e) {
        String message = null;
        try{
            String jsonNotificationMessage = null;
            String appId = "";String issuer = ""; String validationStatus = "";
            ObjectMapper mapper = new ObjectMapper();
            message = e.getMessage();
            if(message == null)
                message = e.toString();

            String code;
            if(e.getClass().equals(EntityNotFoundException.class))
                code = Integer.toString(HttpStatus.NOT_FOUND.value());
            else
                code = Integer.toString(HttpStatus.FORBIDDEN.value());

            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            List<Date> dateList = new ArrayList();
            dateList.add(new Date());
            ResourceAccessNotification notificationMessage = new ResourceAccessNotification(authManager, notificationUrl);
            try {
                notificationMessage.SetFailedAttempts(symbioteId, dateList,code, message, appId, issuer, validationStatus, path); 
                jsonNotificationMessage = mapper.writeValueAsString(notificationMessage);
            } catch (JsonProcessingException jsonEx) {
                log.error("Error while processing json", jsonEx);
            }
            notificationMessage.SendFailAccessMessage(jsonNotificationMessage);
        }catch(Exception ex){
            log.error("Error to send FailAccessMessage to CRAM", ex);
            log.error(ex.getMessage(),ex);
        }
        return message;    
        
    }
    
    private void sendSuccessfulAccessMessage(String symbioteId, String accessType){
        try{
            String jsonNotificationMessage = null;
            if(accessType == null || accessType.isEmpty())
                accessType = SuccessfulAccessMessageInfo.AccessType.NORMAL.name();
            ObjectMapper map = new ObjectMapper();
            map.configure(SerializationFeature.INDENT_OUTPUT, true);
            map.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

            List<Date> dateList = new ArrayList();
            dateList.add(new Date());
            ResourceAccessNotification notificationMessage = new ResourceAccessNotification(authManager, notificationUrl);
            try{
                notificationMessage.SetSuccessfulAttempts(symbioteId, dateList, accessType);
                jsonNotificationMessage = map.writeValueAsString(notificationMessage);
            } catch (JsonProcessingException e) {
                log.error("Error while processing json", e);
            }
            notificationMessage.SendSuccessfulAttemptsMessage(jsonNotificationMessage);
        }catch(Exception e){
            log.error("Error to send SetSuccessfulAttempts to CRAM");
            log.error(e.getMessage(),e);
        }
    }
}

