/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.interfaces;

import com.fasterxml.jackson.databind.JsonNode;
import eu.h2020.symbiote.core.cci.accessNotificationMessages.SuccessfulAccessMessageInfo;
import eu.h2020.symbiote.messages.plugin.RapPluginResponse;
import eu.h2020.symbiote.resources.db.ResourcesRepository;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import eu.h2020.symbiote.cloud.model.rap.access.ResourceAccessGetMessage;
import eu.h2020.symbiote.cloud.model.rap.access.ResourceAccessHistoryMessage;
import eu.h2020.symbiote.cloud.model.rap.access.ResourceAccessMessage.AccessType;
import eu.h2020.symbiote.cloud.model.rap.access.ResourceAccessSetMessage;
import eu.h2020.symbiote.cloud.model.rap.ResourceInfo;
import eu.h2020.symbiote.exceptions.*;
import eu.h2020.symbiote.interfaces.conditions.NBInterfaceRESTCondition;
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.managers.AuthorizationResult;
import eu.h2020.symbiote.managers.ServiceResponseResult;
import eu.h2020.symbiote.resources.RapDefinitions;
import eu.h2020.symbiote.resources.db.AccessPolicyRepository;
import eu.h2020.symbiote.resources.db.DbResourceInfo;
import eu.h2020.symbiote.resources.db.PlatformInfo;
import eu.h2020.symbiote.resources.db.PluginRepository;
import eu.h2020.symbiote.cloud.model.rap.query.Query;
import eu.h2020.symbiote.security.commons.exceptions.custom.ValidationException;
import eu.h2020.symbiote.security.communication.payloads.SecurityRequest;

import java.io.UnsupportedEncodingException;
import java.util.*;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

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
 * @author Matteo Pardi
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
    ResourceAccessNotificationService notificationService;
    
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
    
    @SuppressWarnings("unused")
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
     * @param request 		HTTP request
     * @return  the current value read from the resource
     * @throws java.lang.Exception can throw any exception
     */
    @RequestMapping(value="/rap/Sensor/{resourceId}", method=RequestMethod.GET)
    public ResponseEntity<Object> readResource(@PathVariable String resourceId, HttpServletRequest request) throws Exception {
        Exception e = null;
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        HttpStatus httpStatus = null;
        HttpHeaders responseHeaders = new HttpHeaders();
        RapPluginResponse response = null;
        try {
            log.info("Received read resource request for ID = " + resourceId);       
            
            // checking access policies
            checkAccessPolicies(request, resourceId);
            
            ResourceInfo info = getResourceInfo(resourceId);
            List<ResourceInfo> infoList = new ArrayList<>();
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
            /*
            response = obj;
            if (obj instanceof byte[]) {
                response = new String((byte[]) obj, "UTF-8");
            }
            if(response == null)
                throw new Exception("No response from plugin");
            
            try{
                List<Observation> observations = mapper.readValue(response.toString(), new TypeReference<List<Observation>>() {});
                if(observations != null && !observations.isEmpty()){
                    Observation o = observations.get(0);
                    Observation ob = new Observation(resourceId, o.getLocation(), o.getResultTime(), o.getSamplingTime(), o.getObsValues());
                    response = ob;
                }
            }catch (Exception ex) {
            }*/
            response = extractRapPluginResponse(obj);
            sendSuccessfulAccessMessage(resourceId, SuccessfulAccessMessageInfo.AccessType.NORMAL.name());
            httpStatus = HttpStatus.valueOf(response.getResponseCode());
        } catch(EntityNotFoundException enf) {
            e = enf;
            log.error(e.toString(), e);
            httpStatus = HttpStatus.NOT_FOUND;
            sendFailMessage(path, resourceId, e);
        } catch (Exception ex) {
            e = ex;
            String err = "Unable to read resource with id: " + resourceId;
            log.error(err + "\n" + e.getMessage(),e);
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            sendFailMessage(path, resourceId, e);
        }
        
        ServiceResponseResult serResponse = authManager.generateServiceResponse();
        if(serResponse.isCreatedSuccessfully()) {
            responseHeaders.set(SECURITY_RESPONSE_HEADER, serResponse.getServiceResponse());
        }
        
        return new ResponseEntity<>(response.getContent(), responseHeaders, httpStatus);
    }
    
    /**
     * Used to retrieve the history values of a registered resource
     * 
     * 
     * @param resourceId    the id of the resource to query 
     * @param request 		HTTP request
     * @return  the current value read from the resource
     * @throws Exception		can throw exception
     */
    @RequestMapping(value="/rap/Sensor/{resourceId}/history", method=RequestMethod.GET)
    public ResponseEntity<Object> readResourceHistory(@PathVariable String resourceId, HttpServletRequest request) throws Exception {
        Exception e = null;
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        HttpStatus httpStatus = null;
        HttpHeaders responseHeaders = new HttpHeaders();
        RapPluginResponse response = null;
        try {
            log.info("Received read resource request for ID = " + resourceId);           
            
            checkAccessPolicies(request, resourceId);
        
            ResourceInfo info = getResourceInfo(resourceId);
            List<ResourceInfo> infoList = new ArrayList<>();
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

            /*
            response = obj;
            if (obj instanceof byte[]) {
                response = new String((byte[]) obj, "UTF-8");
            }
            try{
                List<Observation> observations = mapper.readValue(response.toString(), new TypeReference<List<Observation>>() {});
                if(observations != null && !observations.isEmpty()){
                    List<Observation> observationsList = new ArrayList<>();
                    for(Observation o: observations){
                        Observation ob = new Observation(resourceId, o.getLocation(), o.getResultTime(), o.getSamplingTime(), o.getObsValues());
                        observationsList.add(ob);
                    }
                    response = observationsList;
                }
            }catch (Exception ex) {
            }
            */
            response = extractRapPluginResponse(obj);
            sendSuccessfulAccessMessage(resourceId, SuccessfulAccessMessageInfo.AccessType.NORMAL.name());
            httpStatus = HttpStatus.valueOf(response.getResponseCode());
            
        } catch(EntityNotFoundException enf) {
            e = enf;
            log.error(e.toString(),e);
            httpStatus = HttpStatus.NOT_FOUND;
            sendFailMessage(path, resourceId, e);
        } catch (Exception ex) {
            e = ex;
            String err = "Unable to read resource with id: " + resourceId;
            log.error(err + "\n" + e.getMessage(),e);
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            sendFailMessage(path, resourceId, e);
        }     

        ServiceResponseResult serResponse = authManager.generateServiceResponse();
        if(serResponse.isCreatedSuccessfully()) {
            responseHeaders.set(SECURITY_RESPONSE_HEADER, serResponse.getServiceResponse());
        }
        
        return new ResponseEntity<>(response.getContent(), responseHeaders, httpStatus);
    }
    
    /**
     * Used to write a value in a registered resource
     * 
     * 
     * @param resourceId    the id of the resource to query 
     * @param body 			actuator body
     * @param request 		HTTP request
     * @return              the http response code
     * @throws Exception		can throw exception
     */
    @RequestMapping(value={"/rap/Actuator/{resourceId}","/rap/Service/{resourceId}"}, method=RequestMethod.POST)
    public ResponseEntity<?> writeResource(@PathVariable String resourceId, @RequestBody String body, HttpServletRequest request) throws Exception {
        Exception e = null;
        HttpStatus httpStatus = null;
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        RapPluginResponse response = null;
        HttpHeaders responseHeaders = new HttpHeaders();
        try {
            log.info("Received write resource request for ID = " + resourceId + " with values " + body);
            
            checkAccessPolicies(request, resourceId);

            ResourceInfo info = getResourceInfo(resourceId);
            List<ResourceInfo> infoList = new ArrayList<>();
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
            /*response = "";
            if(obj != null){
                if (obj instanceof byte[]) {
                    response = new String((byte[]) obj, "UTF-8");
                } else {
                    response = (String) obj;
                }
            }*/
            response = extractRapPluginResponse(obj);
            sendSuccessfulAccessMessage(resourceId, SuccessfulAccessMessageInfo.AccessType.NORMAL.name());
            httpStatus = HttpStatus.valueOf(response.getResponseCode());
            
        } catch(EntityNotFoundException ex) {
            e = ex;
            log.error(e.toString(), e);
            httpStatus = HttpStatus.NOT_FOUND;
            sendFailMessage(path, resourceId, e);
        } catch (GenericException ex) {
            e = ex;
            log.error(e.toString(), e);
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            sendFailMessage(path, resourceId, e);
        } catch (TimeoutException ex) {
            e = ex;
            log.error(e.toString(), e);
            httpStatus = HttpStatus.GATEWAY_TIMEOUT;
            sendFailMessage(path, resourceId, e);
        } catch (Exception ex) {
            e = ex;
            String err = "Unable to write resource with id: " + resourceId;
            log.error(err + "\n" + e.getMessage(),e);
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            sendFailMessage(path, resourceId, e);
        }

        ServiceResponseResult serResponse = authManager.generateServiceResponse();
        if(serResponse.isCreatedSuccessfully()) {
            responseHeaders.set(SECURITY_RESPONSE_HEADER, serResponse.getServiceResponse());
        }
        
        return new ResponseEntity<>(response.getContent(), responseHeaders, httpStatus);
    }

    private RapPluginResponse extractRapPluginResponse(Object obj)
            throws GenericException, UnsupportedEncodingException {
        ObjectMapper mapper = new ObjectMapper();
        if (obj == null) {
            log.error("No response from plugin");
            throw new TimeoutException("No response from plugin");
        }

        String rawObj;
        if (obj instanceof byte[]) {
            rawObj = new String((byte[]) obj, "UTF-8");
        } else if (obj instanceof String){
            rawObj = (String) obj;
        } else {
            throw new GenericException("Can not parse response from RAP plugin. Expected byte[] or String but got " + obj.getClass().getName());
        }

        try {
            RapPluginResponse resp = mapper.readValue(rawObj, RapPluginResponse.class);
            String content = resp.getContent();
            if(content != null && content.length() > 0) {
                JsonNode jsonObj = mapper.readTree(content);
                if (!jsonObj.has(JSON_OBJECT_TYPE_FIELD_NAME)) {
                    log.error("Field " + JSON_OBJECT_TYPE_FIELD_NAME + " is mandatory");
                }
            }
            return resp;
        } catch (Exception e) {
            throw new GenericException("Can not parse response from RAP to JSON.\n Cause: " + e.getMessage());
        }
    }

    private ResourceInfo getResourceInfo(String resourceId) {
        Optional<DbResourceInfo> resInfo = resourcesRepo.findById(resourceId);
        if(!resInfo.isPresent())
            throw new EntityNotFoundException("Resource " + resourceId + " not found");
        
        return resInfo.get().toResourceInfo();
    }
    
    private void checkAccessPolicies(HttpServletRequest request, String resourceId) throws Exception {
        Map<String, String> secHdrs = new HashMap<>();
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
        
        if (!result.isValidated())
            throw new ValidationException("The access policies were not satisfied for resource " + resourceId + ". MSG: " + result.getMessage());
    }
    
    private String sendFailMessage(String path, String symbioteId, Exception e) {
        String message = null;
        try{
            String appId = "";String issuer = ""; String validationStatus = "";
            message = e.getMessage();
            if(message == null)
                message = e.toString();

            String code;
            if(e.getClass().equals(EntityNotFoundException.class))
                code = Integer.toString(HttpStatus.NOT_FOUND.value());
            else
                code = Integer.toString(HttpStatus.FORBIDDEN.value());

            List<Date> dateList = new ArrayList<>();
            dateList.add(new Date());
    
            notificationService.addFailedAttempts(symbioteId, dateList,code, message, appId, issuer, validationStatus, path); 
            notificationService.sendAccessData();
        }catch(Exception ex){
            log.error("Error to send FailAccessMessage to Monitoring", ex);
        }
        return message;    
        
    }
    
    private void sendSuccessfulAccessMessage(String symbioteId, String accessType){
        try{
            if(accessType == null || accessType.isEmpty())
                accessType = SuccessfulAccessMessageInfo.AccessType.NORMAL.name();

            List<Date> dateList = new ArrayList<>();
            dateList.add(new Date());
          
            notificationService.addSuccessfulAttempts(symbioteId, dateList, accessType);
            notificationService.sendAccessData();
        }catch(Exception e){
            log.error("Error to send SetSuccessfulAttempts to Monitoring", e);
        }
    }
}

