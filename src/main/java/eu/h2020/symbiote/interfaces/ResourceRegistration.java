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
import eu.h2020.symbiote.cloud.model.ResourceLocalSharingMessage;
import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.cloud.model.internal.FederationInfoBean;
import eu.h2020.symbiote.cloud.model.internal.ResourceSharingInformation;
import eu.h2020.symbiote.model.cim.MobileSensor;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.model.cim.StationarySensor;
import eu.h2020.symbiote.resources.db.AccessPolicy;
import eu.h2020.symbiote.resources.db.AccessPolicyRepository;
import eu.h2020.symbiote.resources.db.ResourceInfo;
import eu.h2020.symbiote.security.accesspolicies.IAccessPolicy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	 * 
	 * @param message
	 *            message that has resource description
	 */
	public void receiveRegistrationMessage(byte[] message) {
		try {
			log.info("Resource Registration message received: \n" + new String(message) + "");

			ObjectMapper mapper = new ObjectMapper();

			List<CloudResource> msgs = mapper.readValue(message, new TypeReference<List<CloudResource>>() {
			});
			for (CloudResource msg : msgs) {
				String internalId = msg.getInternalId();
				String pluginId = msg.getPluginId();
				Resource resource = msg.getResource();
				String resourceClass = resource.getClass().getName();
				FederationInfoBean federationInfo = msg.getFederationInfo();

				List<String> props = null;
				if (resource instanceof StationarySensor) {
					props = ((StationarySensor) resource).getObservesProperty();
				} else if (resource instanceof MobileSensor) {
					props = ((MobileSensor) resource).getObservesProperty();
				}

				if (federationInfo == null) {
					String symbioteId = resource.getId();

					log.debug("Registering " + resourceClass + " with symbioteId: " + symbioteId + ", internalId: "
							+ internalId);

					addPolicy(symbioteId, internalId, msg.getAccessPolicy());
					addResource(symbioteId, internalId, props, pluginId, federationInfo);
				} else {
					String symbioteId = null;
					//TODO L2-REWORK
					//MULTIPLE RESOURCES!!!
					
					Map<String, ResourceSharingInformation> sharingInformationList = federationInfo.getSharingInformation();

					for (Map.Entry<String, ResourceSharingInformation> sharingInformation:sharingInformationList.entrySet()) {
						String federationid = sharingInformation.getKey();
						ResourceSharingInformation rsi = sharingInformation.getValue();
						
						symbioteId = rsi.getSymbioteId();
						
						FederationInfoBean federationInfoReduced = new FederationInfoBean();
						Map<String,ResourceSharingInformation> newMap = new HashMap<>();
						newMap.put(federationid, rsi);
						
						federationInfoReduced.setSharingInformation(newMap);
						
						log.debug("Registering " + resourceClass + " with symbioteId: " + symbioteId + ", internalId: "
								+ internalId);
						addResource(symbioteId, internalId, props, pluginId, federationInfoReduced);
					}
					
					
				}
			}
			addCloudResourceInfoForOData(msgs);
		} catch (Exception e) {
			log.error("Error during registration process", e);
		}
	}

	/**
	 * Receive unregistration messages from RabbitMQ queue
	 * 
	 * @param message
	 *            message that has resource to unregister
	 */
	
	//TODO REDO FOR l2 RESOURCES!!!
	public void receiveUnregistrationMessage(byte[] message) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			List<String> ids = mapper.readValue(message, new TypeReference<List<String>>() {
			});
			log.info("Resource Unregistration message received: \n" + ids + "");
			for (String id : ids) {
				// TODO: to check if ID at this level is correct
				log.debug("Unregistering resource with internalId " + id);
				deletePolicy(id);
				deleteResources(id);
			}
		} catch (Exception e) {
			log.info("Error during unregistration process", e);
		}
	}

	/**
	 * Receive update messages from RabbitMQ queue
	 * 
	 * @param message
	 *            message that has resource for update
	 */
	public void receiveUpdateMessage(byte[] message) {
		try {
			log.info("Resource Update message received: \n" + new String(message) + "");

			ObjectMapper mapper = new ObjectMapper();
			List<CloudResource> msgs = mapper.readValue(message, new TypeReference<List<CloudResource>>() {
			});
			for (CloudResource msg : msgs) {
				String internalId = msg.getInternalId();
				String pluginId = msg.getPluginId();
				Resource resource = msg.getResource();
				FederationInfoBean federationInfo = msg.getFederationInfo();

				List<String> props = null;
				if (resource instanceof StationarySensor) {
					props = ((StationarySensor) resource).getObservesProperty();
				} else if (resource instanceof MobileSensor) {
					props = ((MobileSensor) resource).getObservesProperty();
				}

				if (federationInfo == null) {
					String symbioteId = resource.getId();

					log.debug("Updating resource with symbioteId: " + symbioteId + ", internalId: " + internalId);

					addPolicy(symbioteId, internalId, msg.getAccessPolicy());
					addResource(symbioteId, internalId, props, pluginId, federationInfo);
				} else {
					
					String symbioteId = null;
					//TODO L2-REWORK
					//MULTIPLE RESOURCES!!!
					
					Map<String, ResourceSharingInformation> sharingInformationList = federationInfo.getSharingInformation();

					for (Map.Entry<String, ResourceSharingInformation> sharingInformation:sharingInformationList.entrySet()) {
						String federationid = sharingInformation.getKey();
						ResourceSharingInformation rsi = sharingInformation.getValue();
						
						symbioteId = rsi.getSymbioteId();
						
						FederationInfoBean federationInfoReduced = new FederationInfoBean();
						Map<String,ResourceSharingInformation> newMap = new HashMap<>();
						newMap.put(federationid, rsi);
						
						federationInfoReduced.setSharingInformation(newMap);
						
						log.debug("Updating resource with symbioteId: " + symbioteId + ", internalId: " + internalId);
						addResource(symbioteId, internalId, props, pluginId, federationInfoReduced);
					}
					



				}
			}
			addCloudResourceInfoForOData(msgs);
		} catch (Exception e) {
			log.error("Error during registration process", e);
		}
	}

	/**
	 * Receive share messages from RabbitMQ queue
	 * 
	 * @param message
	 *            message that has resource for update
	 */
	public void receiveShareMessage(byte[] message) {
		try {
			log.info("Share Update message received: \n" + new String(message) + "");

			ObjectMapper mapper = new ObjectMapper();
			
			ResourceLocalSharingMessage rlsm = mapper.readValue(message,
					new TypeReference<ResourceLocalSharingMessage>() {
					});
			
//			Map<String, List<CloudResource>> msgs = mapper.readValue(message,
//					new TypeReference<HashMap<String, List<CloudResource>>>() {
//					});

			Map<String, List<CloudResource>> msgs = rlsm.getSharingMap();
			
			for (String key : msgs.keySet()) {
				List<CloudResource> resources = msgs.get(key);

				for (CloudResource res : resources) {
					String internalId = res.getInternalId();
					String pluginId = res.getPluginId();
					Resource resource = res.getResource();
					FederationInfoBean federationInfo = res.getFederationInfo();

					List<String> props = null;
					if (resource instanceof StationarySensor) {
						props = ((StationarySensor) resource).getObservesProperty();
					} else if (resource instanceof MobileSensor) {
						props = ((MobileSensor) resource).getObservesProperty();
					}

					String symbioteId = null;
					//TODO L2-REWORK
					//MULTIPLE RESOURCES!!!
					
					Map<String, ResourceSharingInformation> sharingInformationList = federationInfo.getSharingInformation();

					for (Map.Entry<String, ResourceSharingInformation> sharingInformation:sharingInformationList.entrySet()) {
						String federationid = sharingInformation.getKey();
						ResourceSharingInformation rsi = sharingInformation.getValue();
						
						symbioteId = rsi.getSymbioteId();
						
						FederationInfoBean federationInfoReduced = new FederationInfoBean();
						Map<String,ResourceSharingInformation> newMap = new HashMap<>();
						newMap.put(federationid, rsi);
						
						federationInfoReduced.setSharingInformation(newMap);
						
						log.debug("Sharing resource with symbioteId: " + symbioteId + ", internalId: " + internalId);
						addResource(symbioteId, internalId, props, pluginId, federationInfoReduced);
					}
					
				}
				addCloudResourceInfoForOData(resources);
			}
		} catch (Exception e) {
			log.error("Error during registration process", e);
		}
	}
	
	/**
	 * Receive unshare messages from RabbitMQ queue
	 * 
	 * @param message
	 *            message that has resource for update
	 */
	public void receiveUnShareMessage(byte[] message) {
		try {
			log.info("Share Update message received: \n" + new String(message) + "");

			ObjectMapper mapper = new ObjectMapper();
			
			ResourceLocalSharingMessage rlsm = mapper.readValue(message,
					new TypeReference<ResourceLocalSharingMessage>() {
					});
			
//			Map<String, List<CloudResource>> msgs = mapper.readValue(message,
//					new TypeReference<HashMap<String, List<CloudResource>>>() {
//					});

			Map<String, List<CloudResource>> msgs = rlsm.getSharingMap();
			
			for (String key : msgs.keySet()) {
				List<CloudResource> resources = msgs.get(key);

				for (CloudResource res : resources) {
					String internalId = res.getInternalId();
					FederationInfoBean federationInfo = res.getFederationInfo();

					String symbioteId = null;
					//TODO L2-REWORK
					//MULTIPLE RESOURCES!!!
					
					Map<String, ResourceSharingInformation> sharingInformationList = federationInfo.getSharingInformation();

					for (Map.Entry<String, ResourceSharingInformation> sharingInformation:sharingInformationList.entrySet()) {
						ResourceSharingInformation rsi = sharingInformation.getValue();
						
						symbioteId = rsi.getSymbioteId();
											
						log.debug("Unsharing resource with symbioteId: " + symbioteId + ", internalId: " + internalId);
						deleteResource(symbioteId);
					}
					
				}
				addCloudResourceInfoForOData(resources);
			}
		} catch (Exception e) {
			log.error("Error during registration process", e);
		}
	}

	private void addResource(String resourceId, String platformResourceId, List<String> obsProperties, String pluginId,
			FederationInfoBean federationInfo) {
		ResourceInfo resourceInfo = new ResourceInfo(resourceId, platformResourceId);
		if (obsProperties != null)
			resourceInfo.setObservedProperties(obsProperties);
		if (pluginId != null && pluginId.length() > 0)
			resourceInfo.setPluginId(pluginId);
		if (federationInfo != null)
			resourceInfo.setFederationInfo(federationInfo);

		resourcesRepository.save(resourceInfo);

		log.debug("Resource " + resourceId + " registered");
	}

	/**
	 * deleting resources with the same internalId
	 * @param internalId
	 */
	private void deleteResources(String internalId) {
		try {
			List<ResourceInfo> resourceList = resourcesRepository.findByInternalId(internalId);
			if (resourceList == null || resourceList.isEmpty())
				log.error("Resource " + internalId + " not found");
			while (!resourceList.isEmpty()) {
				resourcesRepository.delete(resourceList.get(resourceList.size()-1).getSymbioteId());
				resourceList.remove(resourceList.size()-1);
				log.info("Resource " + internalId + " unregistered");
			}
		} catch (Exception e) {
			log.error("Resource with id " + internalId + " not found", e);
		}
	}
	
	/**
	 * deleting a certain resource with symbioteId
	 * @param internalId
	 */
	private void deleteResource(String symbioteId) {
		try {
			Optional<ResourceInfo> resource = resourcesRepository.findById(symbioteId);;
			if (resource.isPresent()) {
				resourcesRepository.delete(symbioteId);;
			}
			else {
				log.error("Resource " + symbioteId + " not found");
			}
		} catch (Exception e) {
			log.error("Error deleting resource with id " + symbioteId + " not found", e);
		}
	}

	private void addPolicy(String resourceId, String internalId, IAccessPolicySpecifier accPolicy)
			throws InvalidArgumentsException {
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
			if (accessPolicy == null || accessPolicy.get() == null) {
				log.error("No policy stored for resource with internalId " + internalId);
				return;
			}

			accessPolicyRepository.delete(accessPolicy.get().getResourceId());
			log.info("Policy removed for resource " + internalId);

		} catch (Exception e) {
			log.error("Policy for resource with internalId " + internalId + " not found", e);
		}
	}

	private void addCloudResourceInfoForOData(List<CloudResource> cloudResourceList) {
		try {
			owlApiHelp.addCloudResourceList(cloudResourceList);
		} catch (Exception e) {
			log.error("Error add info registration for OData", e);
		}
	}
}
