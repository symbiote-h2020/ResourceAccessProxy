/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.messages.RegisterResourceMessage;
import eu.h2020.symbiote.messages.RegistrationMessage.RegistrationAction;
import eu.h2020.symbiote.messages.ResourceRegistrationMessage;
import eu.h2020.symbiote.resources.ResourceInfo;
import java.util.ArrayList;
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
    public void receiveMessage(String message) {
        try {
            log.info("Resource Registration message received: \n" + message + "");

            ObjectMapper mapper = new ObjectMapper();
            ResourceRegistrationMessage msg = mapper.readValue(message, ResourceRegistrationMessage.class);
            String resourceId = msg.getResourceId();
            RegistrationAction type = msg.getActionType();

            switch(type) {
                case REGISTER_RESOURCE: {
                    RegisterResourceMessage mess = (RegisterResourceMessage)msg;
                    String platformResourceId = mess.getPlatformResourceId();
                    String platformId = mess.getPlatformId();

                    log.debug("Registering resource for platform " + platformId + " with id " + resourceId);
                    addResource(resourceId, platformResourceId, platformId);
                    break;
                }
                case UNREGISTER_RESOURCE: {
                    log.debug("Unregistering resource with id %s" + resourceId);
                    deleteResource(resourceId);
                    break;
                }
                default:
                    throw new Exception("Wrong message format");
            }
        } catch (Exception e) {
            log.info("Error during registration process\n" + e.getMessage());
        }
    }

    public void notifyMonitoring(String resourceId, RegistrationAction action) {
        log.debug("Sending monitoring notification to " + action.toString() + " resource with id " + resourceId);
        
        //TODO
    }
    
    private void addResource(String resourceId, String platformResourceId, String platformId) {
        ResourceInfo resourceInfo = new ResourceInfo(resourceId, platformResourceId, platformId);
        ArrayList infos = new ArrayList();
        infos.add(resourceInfo);
        resourcesRepository.save(infos);
    }
    
    private void deleteResource(String resourceId) {
        resourcesRepository.delete(resourceId);
    }
}
