/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.interfaces;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.exceptions.*;
import eu.h2020.symbiote.messages.ResourceAccessGetMessage;
import eu.h2020.symbiote.messages.ResourceAccessHistoryMessage;
import eu.h2020.symbiote.messages.ResourceAccessMessage.AccessType;
import eu.h2020.symbiote.messages.ResourceAccessSetMessage;
import eu.h2020.symbiote.resources.RapDefinitions;
import eu.h2020.symbiote.resources.ResourceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 *
 * REST controller which exposes the VNFM northbound APIs
 * used by the NFVO to interact with the VNFM
 * 
 */
@RestController
public class ResourceAccessRestController {

    private static final Logger log = LoggerFactory.getLogger(ResourceAccessRestController.class);
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    @Qualifier(RapDefinitions.PLUGINS_EXCHANGE_OUT)
    TopicExchange exchange;

    /**
     * Used to retrieve the current value of a registered resource
     * 
     * 
     * @param resourceId    the id of the resource to query 
     * @return  the current value read from the resource
     */
    @RequestMapping(value="/v1/resource/{resourceId}", method=RequestMethod.GET)
    public String readResource(@PathVariable String resourceId) {        
        log.info("Received read resource request for ID = " + resourceId);
        String res = "";
        try {
            ResourceInfo info /*= getResourceInfo(resourceId)*/ = new ResourceInfo("test", "test", "test");
            if(/*!checkPlatform()*/false)
                throw new EntityNotFoundException("This platform has not any plugin registered");

            ResourceAccessGetMessage msg = new ResourceAccessGetMessage(info);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(Include.NON_EMPTY);
            String json = mapper.writeValueAsString(msg);
            
            String routingKey = AccessType.GET.toString().toLowerCase() + "." + info.getPlatformId();
            rabbitTemplate.convertAndSend(exchange.getName(), routingKey, json);
        } catch (Exception e) {
            log.error("Unable to read resource with ID [" + resourceId + "]\n" + e.getMessage());
            throw new GenericException(e.getMessage());
        }
        return res;
    }
    
    /**
     * Used to retrieve the history values of a registered resource
     * 
     * 
     * @param resourceId    the id of the resource to query 
     * @return  the current value read from the resource
     */
    @RequestMapping(value="/v1/resource/{resourceId}/history", method=RequestMethod.GET)
    public String readResourceHistory(@PathVariable String resourceId) {
        log.info("Received read resource request for ID = " + resourceId);
        String res = "";
        try {
            ResourceInfo info /*= getResourceInfo(resourceId)*/ = new ResourceInfo("test", "test", "test");
            if(/*!checkPlatform()*/false)
                throw new EntityNotFoundException("");


            ResourceAccessHistoryMessage msg = new ResourceAccessHistoryMessage(info);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(Include.NON_EMPTY);
            String json = mapper.writeValueAsString(msg);
            
            String routingKey = AccessType.HISTORY.toString().toLowerCase() + "." + info.getPlatformId();
            rabbitTemplate.convertAndSend(exchange.getName(), routingKey, json);
        } catch (Exception e) {
            log.error("Unable to read resource with ID [" + resourceId + "]\n" + e.getMessage());
            throw new GenericException(e.getMessage());
        }
        return res;
    }
    
    /**
     * Used to write a value in a registered resource
     * 
     * 
     * @param resourceId    the id of the resource to query 
     * @param value         the value to write
     * @return              the http response code
     */
    @RequestMapping(value="/v1/{resourceId}", method=RequestMethod.POST)
    public ResponseEntity<?> writeResource(@PathVariable String resourceId, @RequestBody String value) {
        try {
            log.info("Received write resource request for ID = " + resourceId + " with value " + value);

            ResourceInfo info /*= getResourceInfo(resourceId)*/ = new ResourceInfo("test", "test", "test");
            if(/*!checkPlatform()*/false)
                throw new EntityNotFoundException("");
            
            ResourceAccessSetMessage msg = new ResourceAccessSetMessage(info, value);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(Include.NON_EMPTY);
            String json = mapper.writeValueAsString(msg);
            
            String routingKey = AccessType.SET.toString().toLowerCase() + "." + info.getPlatformId();
            rabbitTemplate.convertAndSend(exchange.getName(), routingKey, json);            
        } catch (Exception e) {
            log.error("Unable to receive grant ack " + e.getMessage());
        
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        
        return new ResponseEntity<String>(HttpStatus.OK);
    }

}
