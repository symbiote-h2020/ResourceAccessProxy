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
import eu.h2020.symbiote.model.data.Observation;
import eu.h2020.symbiote.model.data.ObservationValue;
import eu.h2020.symbiote.resources.PlatformInfo;
import eu.h2020.symbiote.resources.RapDefinitions;
import eu.h2020.symbiote.resources.ResourceInfo;
import java.util.List;
import java.util.Optional;
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
    @Qualifier(RapDefinitions.PLUGIN_EXCHANGE_OUT)
    TopicExchange exchange;
    
    @Autowired
    ResourcesRepository resourcesRepo;
    
    @Autowired
    PluginRepository pluginRepo;

    /**
     * Used to retrieve the current value of a registered resource
     * 
     * 
     * @param resourceId    the id of the resource to query 
     * @return  the current value read from the resource
     */
    @RequestMapping(value="/rap/Sensor('{resourceId}')", method=RequestMethod.GET)
    public Observation readResource(@PathVariable String resourceId) {        
        try {
            log.info("Received read resource request for ID = " + resourceId);
        
            Observation observation = null;        
            ResourceInfo info = getResourceInfo(resourceId);
        //   if(!checkPlatformPluginPresent(info.getPlatformId()))
        //        throw new EntityNotFoundException("Plugin for platform " + info.getPlatformId() + " not found");

            ResourceAccessGetMessage msg = new ResourceAccessGetMessage(info);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(Include.NON_EMPTY);
            String json = mapper.writeValueAsString(msg);
            
            String routingKey = /*info.getPlatformId() + "." +*/ AccessType.GET.toString().toLowerCase();
            Object obj = rabbitTemplate.convertSendAndReceive(exchange.getName(), routingKey, json);
            String response = null;
            if(obj != null)
                response = new String((byte[]) obj, "UTF-8");
            observation = mapper.readValue(response, Observation.class);
            
            return observation;
        } catch(EntityNotFoundException enf) {
            throw enf;
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
     * @return  the current value read from the resource
     */
    @RequestMapping(value="/rap/Sensor('{resourceId}')/history", method=RequestMethod.GET)
    public List<Observation> readResourceHistory(@PathVariable String resourceId) {
        try {
            log.info("Received read resource request for ID = " + resourceId);

            List<Observation> observationList = null;
        
            ResourceInfo info = getResourceInfo(resourceId);
        //    if(!checkPlatformPluginPresent(info.getPlatformId()))
        //        throw new EntityNotFoundException("Plugin for platform " + info.getPlatformId() + " not found");
            
            ResourceAccessHistoryMessage msg = new ResourceAccessHistoryMessage(info);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(Include.NON_EMPTY);
            String json = mapper.writeValueAsString(msg);
            
            String routingKey = /*info.getPlatformId() + "." + */AccessType.HISTORY.toString().toLowerCase();
            String response = (String)rabbitTemplate.convertSendAndReceive(exchange.getName(), routingKey, json);
            observationList = mapper.readValue(response, List.class);
            if(observationList == null)
                throw new Exception("Plugin error");
            
            return observationList;
        } catch(EntityNotFoundException enf) {
            throw enf;
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
     * @param value         the value to write
     * @return              the http response code
     */
    @RequestMapping(value="/rap/Resource('{resourceId}')", method=RequestMethod.POST)
    public ResponseEntity<?> writeResource(@PathVariable String resourceId, @RequestBody ObservationValue value) {
        try {
            log.info("Received write resource request for ID = " + resourceId + " with value " + value);

            ResourceInfo info = getResourceInfo(resourceId);
        //    if(!checkPlatformPluginPresent(info.getPlatformId()))
        //        throw new EntityNotFoundException("Plugin for platform " + info.getPlatformId() + " not found");
            
            ResourceAccessSetMessage msg = new ResourceAccessSetMessage(info, value);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(Include.NON_EMPTY);
            String json = mapper.writeValueAsString(msg);
            
            String routingKey = /*info.getPlatformId() + "." + */AccessType.SET.toString().toLowerCase();
            rabbitTemplate.convertSendAndReceive(exchange.getName(), routingKey, json);
            
            return new ResponseEntity<>(HttpStatus.OK);
        } catch(EntityNotFoundException enf) {
            throw enf;
        } catch (Exception e) {
            String err = "Unable to write resource with id: " + resourceId;
            log.error(err + "\n" + e.getMessage());
            throw new GenericException(err);
        }
    }

    /*
    private ResourceInfo getResourceInfo(String resourceId) {
        Optional<ResourceInfo> resInfo = resourcesRepo.findByResourceId(resourceId);
        if(!resInfo.isPresent())
            throw new EntityNotFoundException("Resource " + resourceId + " not found");
        
        return resInfo.get();
    }*/
    
    private ResourceInfo getResourceInfo(String resourceId) {
        List<ResourceInfo> resInfo2 = resourcesRepo.findAll();
        //Optional<ResourceInfo> resInfo = resourcesRepo.findByResourceId(resourceId);
        ResourceInfo resInfo = resInfo2.get(1);
        
        return resInfo;
    }
    
    private boolean checkPlatformPluginPresent(String platformId) {
        Optional<PlatformInfo> pluginInfo = pluginRepo.findByPlatformId(platformId);
   
        return pluginInfo.isPresent();
    }
}
