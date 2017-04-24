/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.interfaces;

import eu.h2020.symbiote.resources.db.ResourcesRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.core.model.Property;
import eu.h2020.symbiote.core.model.resources.MobileSensor;
import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.core.model.resources.StationarySensor;
import eu.h2020.symbiote.messages.registration.RegisterResourceMessage;
import eu.h2020.symbiote.messages.registration.RegistrationMessage.RegistrationAction;
import eu.h2020.symbiote.messages.registration.UnregisterResourceMessage;
import eu.h2020.symbiote.messages.registration.UpdateResourceMessage;
import eu.h2020.symbiote.resources.db.ResourceInfo;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
public class ResourceRegistration {
    
    private static final Logger log = LoggerFactory.getLogger(ResourceRegistration.class);

    @Autowired
    ResourcesRepository resourcesRepository;
    /**
     * Receive messages from Rabbit queue 
     * @param message 
     */
    public void receiveRegistrationMessage(byte[] message) {
        try {
            log.info("Resource Registration message received: \n" + new String(message) + "");
        
            ObjectMapper mapper = new ObjectMapper();
            
            List<RegisterResourceMessage> msgs = mapper.readValue(message, new TypeReference<List<RegisterResourceMessage>>(){});
            for(RegisterResourceMessage msg: msgs){
                String internalId = msg.getInternalId();
                Resource resource = msg.getResource();
                String symbioteId = resource.getId();
                /*List<Property> props = null;
                if(resource instanceof StationarySensor) {
                    props = ((StationarySensor)resource).getObservesProperty();
                } else if(resource instanceof MobileSensor) {
                    props = ((MobileSensor)resource).getObservesProperty();
                } */
                
                List<String> props = null;
                if(resource instanceof StationarySensor) {
                    props = ((StationarySensor)resource).getObservesProperty();
                } else if(resource instanceof MobileSensor) {
                    props = ((MobileSensor)resource).getObservesProperty();
                }

                // TO REMOVE - Resource Handler does not fill the ID
                if(symbioteId == null){
                    symbioteId = Integer.toString((int)(Math.random() * Integer.MAX_VALUE));
                }

                log.debug("Registering resource with symbioteId: " + symbioteId + ", internalId: " + internalId);
                addResource(symbioteId, internalId, props);
            }
        } catch (Exception e) {
            log.info("Error during registration process\n" + e.getMessage());
        }
    }
    
    public void receiveUnregistrationMessage(byte[] message) {
        try {
            log.info("Resource Unregistration message received: \n" + message + "");
        
            ObjectMapper mapper = new ObjectMapper();
            List<RegisterResourceMessage> msgs = mapper.readValue(message, new TypeReference<List<UnregisterResourceMessage>>(){});
            for(RegisterResourceMessage msg: msgs){            
                // TODO: to check if ID at this level is correct
                String resourceId = msg.getInternalId();

                log.debug("Unregistering resource with symbioteId %s" + resourceId);
                deleteResource(resourceId);
            }
        } catch (Exception e) {
            log.info("Error during unregistration process\n" + e.getMessage());
        }
    }
    
    public void receiveUpdateMessage(byte[] message) {
        try {
            log.info("Resource Update message received: \n" + message + "");
        
            ObjectMapper mapper = new ObjectMapper();
            List<UpdateResourceMessage> msgs = mapper.readValue(message, new TypeReference<List<UpdateResourceMessage>>(){});
            for(UpdateResourceMessage msg: msgs){            
                String internalId = msg.getInternalId();
                Resource resource = msg.getResource();
                String symbioteId = resource.getId();
                /*List<Property> props = null;
                if(resource instanceof StationarySensor) {
                    props = ((StationarySensor)resource).getObservesProperty();
                } else if(resource instanceof MobileSensor) {
                    props = ((MobileSensor)resource).getObservesProperty();
                }*/
                List<String> props = null;
                if(resource instanceof StationarySensor) {
                    props = ((StationarySensor)resource).getObservesProperty();
                } else if(resource instanceof MobileSensor) {
                    props = ((MobileSensor)resource).getObservesProperty();
                }
                log.debug("Updating resource with symbioteId: " + symbioteId + ", internalId: " + internalId);
                addResource(symbioteId, internalId, props);
            }
        } catch (Exception e) {
            log.info("Error during registration process\n" + e.getMessage());
        }
    }    
    
    public void notifyMonitoring(String resourceId, RegistrationAction action) {
        log.debug("Sending monitoring notification to " + action.toString() + " resource with id " + resourceId);        
        //TODO
    }
    
    /*
    private void addResource(String resourceId, String platformResourceId, List<Property> obsProperties) {
        ResourceInfo resourceInfo = new ResourceInfo(resourceId, platformResourceId);
        if(obsProperties != null)
            resourceInfo.setObservedProperties(obsProperties);
        resourcesRepository.save(resourceInfo);
    }*/
    
    private void addResource(String resourceId, String platformResourceId, List<String> obsProperties) {
        ResourceInfo resourceInfo = new ResourceInfo(resourceId, platformResourceId);
        if(obsProperties != null)
            resourceInfo.setObservedProperties(obsProperties);
        resourcesRepository.save(resourceInfo);
    }
    
    private void deleteResource(String resourceId) {
        resourcesRepository.delete(resourceId);
    }  
    
}
