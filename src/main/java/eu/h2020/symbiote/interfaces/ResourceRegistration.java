/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.interfaces;

import eu.h2020.symbiote.resources.db.ResourcesRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.bim.OwlapiHelp;
import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.cloud.model.internal.FederationInfoBean;
import eu.h2020.symbiote.model.cim.MobileSensor;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.model.cim.StationarySensor;
import eu.h2020.symbiote.resources.db.AccessPolicy;
import eu.h2020.symbiote.resources.db.AccessPolicyRepository;
import eu.h2020.symbiote.resources.db.ResourceInfo;
import eu.h2020.symbiote.security.accesspolicies.IAccessPolicy;
import java.util.List;

import eu.h2020.symbiote.security.accesspolicies.common.AccessPolicyFactory;
import eu.h2020.symbiote.security.accesspolicies.common.IAccessPolicySpecifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import java.util.Optional;


/**
 *
 * @author Matteo Pardi
 */
public class ResourceRegistration {
    
    private static final Logger log = LoggerFactory.getLogger(ResourceRegistration.class);

    @Autowired
    ResourcesRepository resourcesRepository;
    
    @Autowired
    AccessPolicyRepository accessPolicyRepository;
    
    @Autowired
    OwlapiHelp owlApiHelp;
    
    /**
     * Receive registration messages from RabbitMQ queue 
     * @param message	message that has resource description
     */
    public void receiveRegistrationMessage(byte[] message) {
        try {
            log.info("Resource Registration message received: \n" + new String(message) + "");
        
            ObjectMapper mapper = new ObjectMapper();
            
            List<CloudResource> msgs = mapper.readValue(message, new TypeReference<List<CloudResource>>(){});
            for(CloudResource msg: msgs){
                String internalId = msg.getInternalId();
                Resource resource = msg.getResource();
                String pluginId = msg.getPluginId();
                String resourceClass = resource.getClass().getName();
                String symbioteId = resource.getId();
                FederationInfoBean federationInfo = msg.getFederationInfo();
                List<String> props = null;
                if(resource instanceof StationarySensor) {
                    props = ((StationarySensor)resource).getObservesProperty();
                } else if(resource instanceof MobileSensor) {
                    props = ((MobileSensor)resource).getObservesProperty();
                }
                // TO REMOVE - If Resource Handler does not fill the ID
                if(symbioteId == null){
                    symbioteId = Integer.toString((int)(Math.random() * Integer.MAX_VALUE));
                }
                log.debug("Registering "+ resourceClass +" with symbioteId: " + symbioteId + ", internalId: " + internalId);
                
                addPolicy(symbioteId, internalId, msg.getAccessPolicy());
                addResource(symbioteId, internalId, props, pluginId, federationInfo);
            }
            addCloudResourceInfoForOData(msgs);
        } catch (Exception e) {
            log.error("Error during registration process", e);
        }
    }
    
    /**
     * Receive unregistration messages from RabbitMQ queue 
     * @param message	message that has resource to unregister
     */
    public void receiveUnregistrationMessage(byte[] message) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<String> ids = mapper.readValue(message, new TypeReference<List<String>>(){});
            log.info("Resource Unregistration message received: \n" + ids + "");
            for(String id: ids){            
                // TODO: to check if ID at this level is correct
                log.debug("Unregistering resource with internalId " + id);
                deletePolicy(id);
                deleteResource(id);                
            }
        } catch (Exception e) {
            log.info("Error during unregistration process", e);
        }
    }
    
    /**
     * Receive update messages from RabbitMQ queue 
     * @param message	message that has resource for update 
     */
    public void receiveUpdateMessage(byte[] message) {
        try {
            log.info("Resource Update message received: \n" + new String(message) + "");
        
            ObjectMapper mapper = new ObjectMapper();
            List<CloudResource> msgs = mapper.readValue(message, new TypeReference<List<CloudResource>>(){});
            for(CloudResource msg: msgs){
                String internalId = msg.getInternalId();
                Resource resource = msg.getResource();
                String pluginId = msg.getPluginId();
                String symbioteId = resource.getId();
                FederationInfoBean federationInfo = msg.getFederationInfo();

                List<String> props = null;
                if(resource instanceof StationarySensor) {
                    props = ((StationarySensor)resource).getObservesProperty();
                } else if(resource instanceof MobileSensor) {
                    props = ((MobileSensor)resource).getObservesProperty();
                }                
                log.debug("Updating resource with symbioteId: " + symbioteId + ", internalId: " + internalId);
                
                addPolicy(symbioteId, internalId, msg.getAccessPolicy());
                addResource(symbioteId, internalId, props, pluginId, federationInfo);
            }
            addCloudResourceInfoForOData(msgs);
        } catch (Exception e) {
            log.error("Error during registration process", e);
        }
    }
    
    private void addResource(String resourceId, String platformResourceId, List<String> obsProperties, String pluginId, FederationInfoBean federationInfo) {
        ResourceInfo resourceInfo = new ResourceInfo(resourceId, platformResourceId);
        if(obsProperties != null)
            resourceInfo.setObservedProperties(obsProperties);
        if(pluginId != null && pluginId.length()>0)
            resourceInfo.setPluginId(pluginId);
        if(federationInfo != null)
            resourceInfo.setFederationInfo(federationInfo);
        	
        
        resourcesRepository.save(resourceInfo);
        
        log.debug("Resource " + resourceId + " registered");
    }
    
    private void deleteResource(String internalId) {
        try {
            List<ResourceInfo> resourceList = resourcesRepository.findByInternalId(internalId);
            if(resourceList != null && !resourceList.isEmpty()) {
                resourcesRepository.delete(resourceList.get(0).getSymbioteId());
                log.info("Resource " + internalId + " unregistered");
            } else {
                log.error("Resource " + internalId + " not found");
            }
        } catch (Exception e) {
            log.error("Resource with id " + internalId + " not found", e);
        }
    }  
    
    private void addPolicy(String resourceId, String internalId, IAccessPolicySpecifier accPolicy) throws InvalidArgumentsException {
        try {            
            IAccessPolicy policy = AccessPolicyFactory.getAccessPolicy(accPolicy);
            AccessPolicy ap = new AccessPolicy(resourceId, internalId, policy);
            accessPolicyRepository.save(ap);            
            
            log.info("Policy successfully added for resource " + resourceId);
        } catch (InvalidArgumentsException e) {
            throw new InvalidArgumentsException("Invalid Policy definition for resource with id " + resourceId, e);
        }
    }    
    
    private void deletePolicy(String internalId) {
        try {
            Optional<AccessPolicy> accessPolicy = accessPolicyRepository.findByInternalId(internalId);
            if(accessPolicy == null || accessPolicy.get() == null) {
                log.error("No policy stored for resource with internalId " + internalId);
                return;
            }
            
            accessPolicyRepository.delete(accessPolicy.get().getResourceId());
            log.info("Policy removed for resource " + internalId);
            
        } catch (Exception e) {
            log.error("Resource with internalId " + internalId + " not found", e);
        }
    }

    private void addCloudResourceInfoForOData(List<CloudResource> cloudResourceList) {
        try{
            owlApiHelp.addCloudResourceList(cloudResourceList);
        }
        catch(Exception e){
            log.error("Error add info registration for OData", e);
        }
    }
}
