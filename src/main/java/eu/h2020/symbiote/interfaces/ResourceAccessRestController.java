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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.cloud.model.data.parameter.InputParameter;
import eu.h2020.symbiote.security.InternalSecurityHandler;
import eu.h2020.symbiote.security.enums.ValidationStatus;
import eu.h2020.symbiote.security.token.Token;
import eu.h2020.symbiote.security.exceptions.aam.TokenValidationException;
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
import eu.h2020.symbiote.resources.db.PlatformInfo;
import eu.h2020.symbiote.resources.db.PluginRepository;
import eu.h2020.symbiote.resources.db.ResourceInfo;
import eu.h2020.symbiote.resources.query.Query;
import io.jsonwebtoken.Claims;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


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
    private InternalSecurityHandler securityHandler;    

    /**
     * Used to retrieve the current value of a registered resource
     * 
     * 
     * @param resourceId    the id of the resource to query 
     * @param token 
     * @return  the current value read from the resource
     */
    @RequestMapping(value="/rap/Sensor/{resourceId}", method=RequestMethod.GET)
    public Observation readResource(@PathVariable String resourceId, @RequestHeader("X-Auth-Token") String token) {
        Exception e = null;
        String path = "/rap/Sensor/"+resourceId;
        try {
            log.info("Received read resource request for ID = " + resourceId);       
            
            checkToken(token);
            
            ResourceInfo info = getResourceInfo(resourceId);
            ResourceAccessGetMessage msg = new ResourceAccessGetMessage(info,null);
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
            
            String response = new String((byte[]) obj, "UTF-8");
            List<Observation> observationList = mapper.readValue(response, List.class);
            if(observationList == null || observationList.isEmpty())
                throw new Exception("Plugin error");
            
            Observation o = observationList.get(0);
            Observation ob = new Observation(resourceId, o.getLocation(), o.getResultTime(), o.getSamplingTime(), o.getObsValues());
            sendSuccessfulAccessMessage(resourceId, SuccessfulAccessMessageInfo.AccessType.NORMAL.name());
            return ob;
        } catch(EntityNotFoundException enf) {
            e = enf;
            log.error(e.toString());
        } catch (TokenValidationException tokenEx) { 
            e = tokenEx;
            log.error(e.toString());
        } catch (Exception ex) {
            e = ex;
            String err = "Unable to read resource with id: " + resourceId;
            log.error(err + "\n" + e.getMessage());
        }     
        sendFailMessage(path, resourceId, token, e);
        throw new GenericException(e.getMessage());
    }
    
    /**
     * Used to retrieve the history values of a registered resource
     * 
     * 
     * @param resourceId    the id of the resource to query 
     * @param token 
     * @return  the current value read from the resource
     */
    @RequestMapping(value="/rap/Sensor/{resourceId}/history", method=RequestMethod.GET)
    public List<Observation> readResourceHistory(@PathVariable String resourceId, @RequestHeader("X-Auth-Token") String token) {
        Exception e = null;
        String path = "/rap/Sensor/"+resourceId+"/history";
        try {
            log.info("Received read resource request for ID = " + resourceId);           
            
            checkToken(token);
        
            ResourceInfo info = getResourceInfo(resourceId);
            Query q = null;
            ResourceAccessHistoryMessage msg = new ResourceAccessHistoryMessage(info, TOP_LIMIT, q,null);
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
            String response = (String)rabbitTemplate.convertSendAndReceive(exchange.getName(), routingKey, json);
            List<Observation> observations = mapper.readValue(response, List.class);
            if(observations == null)
                throw new Exception("Plugin error");
            
            List<Observation> observationsList = new ArrayList();
            for(Observation o: observations){
                Observation ob = new Observation(resourceId, o.getLocation(), o.getResultTime(), o.getSamplingTime(), o.getObsValues());
                observationsList.add(ob);
            }
            sendSuccessfulAccessMessage(resourceId, SuccessfulAccessMessageInfo.AccessType.NORMAL.name());
            return observationsList;
        } catch(EntityNotFoundException enf) {
            e = enf;
            log.error(e.toString());
        } catch (TokenValidationException tokenEx) { 
            e = tokenEx;
            log.error(e.toString());
            throw new GenericException(e.toString());
        } catch (Exception ex) {
            e = ex;
            String err = "Unable to read history of resource with id: " + resourceId;
            log.error(err + "\n" + e.getMessage());
        }  
        sendFailMessage(path, resourceId, token, e);
        throw new GenericException(e.getMessage());
    }
    
    /**
     * Used to write a value in a registered resource
     * 
     * 
     * @param resourceId    the id of the resource to query 
     * @param valueList     the value list to write     
     * @param token     
     * @return              the http response code
     */
    @RequestMapping(value="/rap/Service/{resourceId}", method=RequestMethod.POST)
    public ResponseEntity<?> writeResource(@PathVariable String resourceId, @RequestBody List<InputParameter> valueList,
                                           @RequestHeader("X-Auth-Token") String token) {
        Exception e = null;
        String path = "/rap/Service/" + resourceId;
        try {
            log.info("Received write resource request for ID = " + resourceId + " with values " + valueList);
            
            checkToken(token);

            ResourceInfo info = getResourceInfo(resourceId);
            ResourceAccessSetMessage msg = new ResourceAccessSetMessage(info, valueList);            
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
            rabbitTemplate.convertSendAndReceive(exchange.getName(), routingKey, json);
            
            sendSuccessfulAccessMessage(resourceId, SuccessfulAccessMessageInfo.AccessType.NORMAL.name());
            return new ResponseEntity<>(HttpStatus.OK);
        } catch(EntityNotFoundException | TokenValidationException enf) {
            e = enf;
            log.error(e.toString());
        } catch (Exception ex) {
            e = ex;
            String err = "Unable to write resource with id: " + resourceId;
            log.error(err + "\n" + e.getMessage());
        }
        sendFailMessage(path, resourceId, token, e);
        throw new GenericException(e.getMessage());
    }
    
    private ResourceInfo getResourceInfo(String resourceId) {
        Optional<ResourceInfo> resInfo = resourcesRepo.findById(resourceId);
        if(!resInfo.isPresent())
            throw new EntityNotFoundException("Resource " + resourceId + " not found");
        
        return resInfo.get();
    }
    
    private void checkToken(String tokenString) throws TokenValidationException {
        log.debug("RAP received a request for the following token: " + tokenString);

        Token token = new Token(tokenString);

        ValidationStatus status = securityHandler.verifyHomeToken(token);
        switch (status){
            case VALID: {
                log.info("Token is VALID");  
                break;
            }
            case VALID_OFFLINE: {
                log.info("Token is VALID_OFFLINE");  
                break;
            }
            case EXPIRED: {
                log.info("Token is EXPIRED");
                throw new TokenValidationException("Token is EXPIRED");
            }
            case REVOKED: {
                log.info("Token is REVOKED");  
                throw new TokenValidationException("Token is REVOKED");
            }
            case INVALID: {
                log.info("Token is INVALID");  
                throw new TokenValidationException("Token is INVALID");
            }
            case NULL: {
                log.info("Token is NULL");  
                throw new TokenValidationException("Token is NULL");
            }
        } 
    }
    
    
    private void sendFailMessage(String path, String symbioteId, String token, Exception e) {
        String jsonNotificationMessage = null;
        String appId = "";String issuer = ""; String validationStatus = "";
        ObjectMapper mapper = new ObjectMapper();
        
        String code = Integer.toString(HttpStatus.INTERNAL_SERVER_ERROR.value());
        String message = e.getMessage();
        if(message == null)
            message = e.toString();
        
        if(e.getClass().equals(EntityNotFoundException.class))
            code = Integer.toString(HttpStatus.NOT_FOUND.value());
        else if(e.getClass().equals(TokenValidationException.class))
            code = Integer.toString(HttpStatus.FORBIDDEN.value());

        
        if(token != null && !token.isEmpty()){
            try{
                Token tok = new Token(token);
                Claims claims = tok.getClaims();
                appId = claims.getSubject();
                issuer = claims.getIssuer();
                ValidationStatus status = securityHandler.verifyHomeToken(tok);
                validationStatus = status.name();
            }
            catch(TokenValidationException tokenExc){
                validationStatus = tokenExc.getErrorMessage();
            }
            catch(Exception ex){
                log.error(ex.getMessage());
            }
        }
            
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        NotificationMessage notificationMessage = new NotificationMessage();
        try {
            notificationMessage.SetFailedAttempts(symbioteId, timestamp, 
            code, message, appId, issuer, validationStatus, path); 
            jsonNotificationMessage = mapper.writeValueAsString(notificationMessage);
        } catch (JsonProcessingException jsonEx) {
            log.error(jsonEx.getMessage());
        }
        NotificationMessage.SendFailAccessMessage(jsonNotificationMessage);
    }
    
    public static void sendSuccessfulAccessMessage(String symbioteId, String accessType){
        String jsonNotificationMessage = null;
        if(accessType == null || accessType.isEmpty())
            accessType = SuccessfulAccessMessageInfo.AccessType.NORMAL.name();
        ObjectMapper map = new ObjectMapper();
        map.configure(SerializationFeature.INDENT_OUTPUT, true);
        map.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        NotificationMessage notificationMessage = new NotificationMessage();
        
        try{
            notificationMessage.SetSuccessfulAttempts(symbioteId, timestamp, accessType);
            jsonNotificationMessage = map.writeValueAsString(notificationMessage);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
        NotificationMessage.SendSuccessfulAttemptsMessage(jsonNotificationMessage);
    }
}

