/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.interfaces;

import eu.h2020.symbiote.resources.db.ResourcesRepository;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.cloud.model.data.parameter.InputParameter;
import eu.h2020.symbiote.security.SecurityHandler;
import eu.h2020.symbiote.security.session.AAM;
import eu.h2020.symbiote.security.enums.ValidationStatus;
import eu.h2020.symbiote.security.token.Token;
import eu.h2020.symbiote.security.exceptions.aam.TokenValidationException;
import eu.h2020.symbiote.security.exceptions.SecurityHandlerException;
import eu.h2020.symbiote.exceptions.*;
import eu.h2020.symbiote.interfaces.conditions.NBInterfaceRESTCondition;
import eu.h2020.symbiote.messages.access.ResourceAccessGetMessage;
import eu.h2020.symbiote.messages.access.ResourceAccessHistoryMessage;
import eu.h2020.symbiote.messages.access.ResourceAccessMessage.AccessType;
import eu.h2020.symbiote.messages.access.ResourceAccessSetMessage;
import eu.h2020.symbiote.core.model.Observation;
import eu.h2020.symbiote.resources.RapDefinitions;
import eu.h2020.symbiote.resources.db.ResourceInfo;
import eu.h2020.symbiote.resources.query.Query;
import java.util.List;
import java.util.Iterator;
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
import org.springframework.beans.factory.annotation.Value;


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
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    @Qualifier(RapDefinitions.PLUGIN_EXCHANGE_OUT)
    private TopicExchange exchange;
    
    @Autowired
    private ResourcesRepository resourcesRepo;
    
    @Autowired
    private SecurityHandler securityHandler;    

    @Value("${platform.id}") 
    private String platformId;

    @Autowired
    private AAM platformAAM;

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
        try {
            log.info("Received read resource request for ID = " + resourceId);       
            
            checkToken(platformAAM, token);
            
            ResourceInfo info = getResourceInfo(resourceId);
            ResourceAccessGetMessage msg = new ResourceAccessGetMessage(info);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(Include.NON_EMPTY);
            String json = mapper.writeValueAsString(msg);
            
            String routingKey = AccessType.GET.toString().toLowerCase();
            Object obj = rabbitTemplate.convertSendAndReceive(exchange.getName(), routingKey, json);
            String response = null;
            if(obj != null)
                response = new String((byte[]) obj, "UTF-8");
            List<Observation> observationList = mapper.readValue(response, List.class);
            if(observationList == null || observationList.isEmpty())
                throw new Exception("Plugin error");
            
            return observationList.get(0);
        } catch(EntityNotFoundException enf) {
            throw enf;
        } catch (TokenValidationException e) { 
            log.error(e.toString());
            throw new GenericException(e.toString());
        } catch (SecurityHandlerException e) {
            log.error(e.toString()); 
            throw new GenericException(e.toString());
        } catch (Exception e) {
            String err = "Unable to read resource with id: " + resourceId;
            log.error(err + "\n" + e.getMessage());
            throw new GenericException(err);
        }        
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
        try {
            log.info("Received read resource request for ID = " + resourceId);           
            
            checkToken(platformAAM, token);
        
            ResourceInfo info = getResourceInfo(resourceId);
            Query q = null;
            ResourceAccessHistoryMessage msg = new ResourceAccessHistoryMessage(info,q);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(Include.NON_EMPTY);
            String json = mapper.writeValueAsString(msg);
            
            String routingKey = AccessType.HISTORY.toString().toLowerCase();
            String response = (String)rabbitTemplate.convertSendAndReceive(exchange.getName(), routingKey, json);
            List<Observation> observationList = mapper.readValue(response, List.class);
            if(observationList == null)
                throw new Exception("Plugin error");
            
            return observationList;
        } catch(EntityNotFoundException enf) {
            throw enf;
        } catch (TokenValidationException e) { 
            log.error(e.toString());
            throw new GenericException(e.toString());
        } catch (SecurityHandlerException e) {
            log.error(e.toString()); 
            throw new GenericException(e.toString());
        } catch (Exception e) {
            String err = "Unable to read history of resource with id: " + resourceId;
            log.error(err + "\n" + e.getMessage());
            throw new GenericException(err);
        }        
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
        try {
            log.info("Received write resource request for ID = " + resourceId + " with values " + valueList);
            
            checkToken(platformAAM, token);

            ResourceInfo info = getResourceInfo(resourceId);
            ResourceAccessSetMessage msg = new ResourceAccessSetMessage(info, valueList);            
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(Include.NON_EMPTY);
            String json = mapper.writeValueAsString(msg);
            
            String routingKey = AccessType.SET.toString().toLowerCase();
            rabbitTemplate.convertSendAndReceive(exchange.getName(), routingKey, json);
            
            return new ResponseEntity<>(HttpStatus.OK);
        } catch(EntityNotFoundException enf) {
            throw enf;
        } catch (TokenValidationException e) { 
            log.error(e.toString());
            throw new GenericException(e.toString());
        } catch (SecurityHandlerException e) {
            log.error(e.toString()); 
            throw new GenericException(e.toString());
        } catch (Exception e) {
            String err = "Unable to write resource with id: " + resourceId;
            log.error(err + "\n" + e.getMessage());
            throw new GenericException(err);
        }
    }
    
    private ResourceInfo getResourceInfo(String resourceId) {
        Optional<ResourceInfo> resInfo = resourcesRepo.findById(resourceId);
        if(!resInfo.isPresent())
            throw new EntityNotFoundException("Resource " + resourceId + " not found");
        
        return resInfo.get();
    }
    
    private void checkToken(AAM platformAAM, String tokenString) throws TokenValidationException, SecurityHandlerException {
        log.debug("RAP received a request for the following token: " + tokenString);
        try {
            
            if (platformAAM.getAamInstanceId() != this.platformId) {
                refreshPlatformAAM();
                if (platformAAM.getAamInstanceId() != this.platformId)
                    throw new TokenValidationException("The platform AAM is not registered");
            }

            Token token = new Token(tokenString);

            ValidationStatus status = securityHandler.verifyPlatformToken(platformAAM, token);
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
        } catch (TokenValidationException e) { 
            log.error(e.toString());
        }
        catch (SecurityHandlerException e) {
            log.info(e.toString()); 
        }

    }

    private void refreshPlatformAAM() throws SecurityHandlerException {
        List<AAM> listOfAAMs = securityHandler.getAvailableAAMs();

        for(Iterator iter = listOfAAMs.iterator(); iter.hasNext();) {
            AAM aam = (AAM) iter.next();
            if (aam.getAamInstanceId() == this.platformId) {
                platformAAM.setAamInstanceId(aam.getAamInstanceId());
                platformAAM.setAamAddress(aam.getAamAddress());
                platformAAM.setAamInstanceFriendlyName(aam.getAamInstanceFriendlyName());
                platformAAM.setCertificate(aam.getCertificate());
            }
        }
    }
}

