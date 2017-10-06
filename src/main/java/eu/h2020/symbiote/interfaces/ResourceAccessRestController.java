/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.interfaces;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.h2020.symbiote.resources.db.ResourcesRepository;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.security.handler.IComponentSecurityHandler;
import eu.h2020.symbiote.exceptions.*;
import eu.h2020.symbiote.interfaces.conditions.NBInterfaceRESTCondition;
import eu.h2020.symbiote.messages.access.ResourceAccessGetMessage;
import eu.h2020.symbiote.messages.access.ResourceAccessHistoryMessage;
import eu.h2020.symbiote.messages.access.ResourceAccessMessage.AccessType;
import eu.h2020.symbiote.messages.access.ResourceAccessSetMessage;
import eu.h2020.symbiote.cloud.model.data.observation.Observation;
import eu.h2020.symbiote.messages.accessNotificationMessages.NotificationMessage;
import eu.h2020.symbiote.messages.accessNotificationMessages.SuccessfulAccessMessageInfo;
import eu.h2020.symbiote.resources.RapDefinitions;
import eu.h2020.symbiote.resources.db.AccessPolicy;
import eu.h2020.symbiote.resources.db.AccessPolicyRepository;
import eu.h2020.symbiote.resources.db.PlatformInfo;
import eu.h2020.symbiote.resources.db.PluginRepository;
import eu.h2020.symbiote.resources.db.ResourceInfo;
import eu.h2020.symbiote.resources.query.Query;
import eu.h2020.symbiote.security.SecurityHelper;
import eu.h2020.symbiote.security.accesspolicies.IAccessPolicy;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import eu.h2020.symbiote.security.communication.payloads.SecurityRequest;
import java.util.*;
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
    private IComponentSecurityHandler securityHandler; 
    
    @Autowired
    private AccessPolicyRepository accessPolicyRepo;
   
    @Value("${symbiote.notification.url}") 
    private String notificationUrl;
    
    @Autowired
    private SecurityHelper securityHelper;
    

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
    public ResponseEntity<Observation> readResource(@PathVariable String resourceId, HttpServletRequest request) throws Exception {
        Exception e = null;
        String path = "/rap/Sensor/"+resourceId;
        HttpStatus httpStatus = null;
        HttpHeaders responseHeaders = new HttpHeaders();
        Observation ob = null;
        try {
            log.info("Received read resource request for ID = " + resourceId);       
            
            // checking access policies
            checkAccessPolicies(request, resourceId);
            
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
            String response;
            if (obj instanceof byte[]) {
                response = new String((byte[]) obj, "UTF-8");
            } else {
                response = (String) obj;
            }
            if(response == null)
                throw new Exception("No response from plugin");
            List<Observation> observations = mapper.readValue(response, new TypeReference<List<Observation>>() {});
            if(observations == null || observations.isEmpty())
                throw new Exception("Plugin error");
            
            Observation o = observations.get(0);
            ob = new Observation(resourceId, o.getLocation(), o.getResultTime(), o.getSamplingTime(), o.getObsValues());
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
        
        try{
            String serResponse = securityHandler.generateServiceResponse();
            responseHeaders.set(SECURITY_RESPONSE_HEADER, serResponse);
        }
        catch(SecurityHandlerException sce){
            log.error(sce.getMessage(), sce);
            throw sce;
        }
        return new ResponseEntity<>(ob , responseHeaders, httpStatus);
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
    public ResponseEntity<List<Observation> > readResourceHistory(@PathVariable String resourceId, HttpServletRequest request) throws Exception {
        Exception e = null;
        String path = "/rap/Sensor/"+resourceId+"/history";
        HttpStatus httpStatus = null;
        HttpHeaders responseHeaders = new HttpHeaders();
        List<Observation> observationsList = null;
        try {
            log.info("Received read resource request for ID = " + resourceId);           
            
            checkAccessPolicies(request, resourceId);
        
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
            String response;
            if (obj instanceof byte[]) {
                response = new String((byte[]) obj, "UTF-8");
            } else {
                response = (String) obj;
            }
            List<Observation> observations = mapper.readValue(response, new TypeReference<List<Observation>>() {});
            if(observations == null || observations.isEmpty())
                throw new Exception("Plugin error");
            
            observationsList = new ArrayList();
            for(Observation o: observations){
                Observation ob = new Observation(resourceId, o.getLocation(), o.getResultTime(), o.getSamplingTime(), o.getObsValues());
                observationsList.add(ob);
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
        
        try{
            String serResponse = securityHandler.generateServiceResponse();
            responseHeaders.set(SECURITY_RESPONSE_HEADER, serResponse);
        }
        catch(SecurityHandlerException sce){
            log.error(sce.getMessage(), sce);
            throw sce;
        }
        return new ResponseEntity<>(observationsList, responseHeaders, httpStatus);
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
        String response = null;
        HttpHeaders responseHeaders = new HttpHeaders();
        try {
            log.info("Received write resource request for ID = " + resourceId + " with values " + body);
            
            checkAccessPolicies(request, resourceId);

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
            response = "";
            if(obj != null){
                if (obj instanceof byte[]) {
                    response = new String((byte[]) obj, "UTF-8");
                } else {
                    response = (String) obj;
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
        
        try{
            String serResponse = securityHandler.generateServiceResponse();
            responseHeaders.set(SECURITY_RESPONSE_HEADER, serResponse);
        }
        catch(SecurityHandlerException sce){
            log.error(sce.getMessage(), sce);
            throw sce;
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

        checkAuthorization(securityReq, resourceId);
        
        return true;
    }
    
    private void checkAuthorization(SecurityRequest request, String resourceId) throws Exception {
        log.debug("RAP received a security request : " + request.toString());        
         // building dummy access policy
        Map<String, IAccessPolicy> accessPolicyMap = new HashMap<>();
        // to get policies here
        Optional<AccessPolicy> accPolicy = accessPolicyRepo.findById(resourceId);
        if(accPolicy == null)
            throw new Exception("No access policies for resource");
        
        accessPolicyMap.put(resourceId, accPolicy.get().getPolicy());
        Set<String> ids = securityHandler.getSatisfiedPoliciesIdentifiers(accessPolicyMap, request);
        if(!ids.contains(resourceId))
            throw new Exception("Security Policy is not valid");
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
            NotificationMessage notificationMessage = new NotificationMessage(securityHelper,notificationUrl);
            try {
                notificationMessage.SetFailedAttempts(symbioteId, dateList,code, message, appId, issuer, validationStatus, path); 
                jsonNotificationMessage = mapper.writeValueAsString(notificationMessage);
            } catch (JsonProcessingException jsonEx) {
                //log.error(jsonEx.getMessage());
            }
            notificationMessage.SendFailAccessMessage(jsonNotificationMessage);
        }catch(Exception ex){
            log.error("Error to send FailAccessMessage to CRAM");
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
            NotificationMessage notificationMessage = new NotificationMessage(securityHelper,notificationUrl);
            try{
                notificationMessage.SetSuccessfulAttempts(symbioteId, dateList, accessType);
                jsonNotificationMessage = map.writeValueAsString(notificationMessage);
            } catch (JsonProcessingException e) {
                //log.error(e.getMessage());
            }
            notificationMessage.SendSuccessfulAttemptsMessage(jsonNotificationMessage);
        }catch(Exception e){
            log.error("Error to send SetSuccessfulAttempts to CRAM");
            log.error(e.getMessage(),e);
        }
    }
}

