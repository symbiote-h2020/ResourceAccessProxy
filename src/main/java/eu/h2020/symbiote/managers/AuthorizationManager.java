/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.managers;

import com.fasterxml.jackson.core.JsonProcessingException;

import eu.h2020.symbiote.interfaces.BarteringTradingCommunicationService;
import eu.h2020.symbiote.model.mim.Federation;
import eu.h2020.symbiote.model.mim.FederationMember;
import eu.h2020.symbiote.resources.db.AccessPolicy;
import eu.h2020.symbiote.resources.db.AccessPolicyRepository;
import eu.h2020.symbiote.resources.db.FederationRepository;
import eu.h2020.symbiote.resources.db.ResourceInfo;
import eu.h2020.symbiote.resources.db.ResourcesRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import eu.h2020.symbiote.security.ComponentSecurityHandlerFactory;
import eu.h2020.symbiote.security.accesspolicies.IAccessPolicy;
import eu.h2020.symbiote.security.accesspolicies.common.singletoken.SingleFederatedTokenAccessPolicy;

import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import eu.h2020.symbiote.security.commons.exceptions.custom.ValidationException;
import eu.h2020.symbiote.security.communication.payloads.SecurityRequest;
import eu.h2020.symbiote.security.handler.IComponentSecurityHandler;
import io.jsonwebtoken.Claims;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import eu.h2020.symbiote.security.commons.Coupon;
import eu.h2020.symbiote.security.commons.Token;
import eu.h2020.symbiote.security.commons.Token.Type;

/**
 * Component responsible for dealing with Symbiote Tokens and checking access right for requests.
 *
 * @author Matteo Pardi, Pavle Skocir
 */
@Component()
public class AuthorizationManager {

    private static Logger log = LoggerFactory.getLogger(AuthorizationManager.class);

    private final String componentOwnerName;
    private final String componentOwnerPassword;
    // TODO SL5.0 remove private final String coreAamAddress;
    private final String localAamAddress;
    private final String clientId;
    private final String keystoreName;
    private final String keystorePass;
    // TODO SL5.0 remove private final Boolean alwaysUseLocalAAMForValidation;
    private Boolean securityEnabled;
    
    @Autowired
    private AccessPolicyRepository accessPolicyRepo;
    
    @Autowired
    private FederationRepository fedRepo;

    @Autowired
    private ResourcesRepository resourceRepo;
    
    @Autowired
    BarteringTradingCommunicationService barteringTradingService;
    
    private IComponentSecurityHandler componentSecurityHandler;

    @Autowired
    public AuthorizationManager(@Value("${symbIoTe.component.username}") String componentOwnerName,
                                @Value("${symbIoTe.component.password}") String componentOwnerPassword,
                                @Value("${symbIoTe.core.interface.url}") String coreAamAddress,
                                @Value("${symbIoTe.localaam.url}") String localAamAddress,
                                @Value("${platform.id}") String clientId,
                                @Value("${symbIoTe.component.keystore.path}") String keystoreName,
                                @Value("${symbIoTe.component.keystore.password}") String keystorePass,
                                @Value("${rap.security.enabled:true}") Boolean securityEnabled,
                                @Value("${symbIoTe.validation.localaam}") Boolean alwaysUseLocalAAMForValidation)
            throws SecurityHandlerException, InvalidArgumentsException {

        Assert.notNull(componentOwnerName,"componentOwnerName can not be null!");
        this.componentOwnerName = componentOwnerName;

        Assert.notNull(componentOwnerPassword,"componentOwnerPassword can not be null!");
        this.componentOwnerPassword = componentOwnerPassword;

        // TODO remove SL5.0
//        Assert.notNull(coreAamAddress,"coreAamAddress can not be null!");
//        this.coreAamAddress = coreAamAddress;
        
        Assert.notNull(localAamAddress,"localAamAddress can not be null!");
        this.localAamAddress = localAamAddress;

        Assert.notNull(clientId,"clientId can not be null!");
        this.clientId = clientId;

        Assert.notNull(keystoreName,"keystoreName can not be null!");
        this.keystoreName = keystoreName;

        Assert.notNull(keystorePass,"keystorePass can not be null!");
        this.keystorePass = keystorePass;

        // TODO removeSL5.0
//        Assert.notNull(alwaysUseLocalAAMForValidation,"alwaysUseLocalAAMForValidation can not be null!");
//        this.alwaysUseLocalAAMForValidation = alwaysUseLocalAAMForValidation;

        Assert.notNull(securityEnabled,"securityEnabled can not be null!");
        this.securityEnabled = securityEnabled;

        if (securityEnabled)
            enableSecurity();
    }

    public AuthorizationResult checkResourceUrlRequest(String resourceId, SecurityRequest securityRequest) {
    	boolean bartering = false;
    	if (securityEnabled) {
            log.debug("Received SecurityRequest of ResourceUrlsRequest to be verified: (" + securityRequest + ")");

            if (securityRequest == null) {
                return new AuthorizationResult("SecurityRequest is null", false);
            }
            
            Set<String> checkedPolicies = new HashSet<String>();
            
            if(isCoreResourceId(resourceId)) {
	            try {
	                checkedPolicies = checkStoredResourcePolicies(securityRequest, resourceId);
	            } catch (Exception e) {
	                log.error(e.getMessage(), e);
	                return new AuthorizationResult(e.getMessage(), false);
	            }
	            
	            if (checkedPolicies.size() == 1) {
	                return new AuthorizationResult("ok", true);
	            } else {
	                return new AuthorizationResult("The stored resource access policy was not satisfied",
	                        false);
	            }
	            
            } else {
            	try {
	                checkedPolicies=checkFederatedResourcePolicies(securityRequest, resourceId);
	            } catch (Exception e) {
	                log.error(e.getMessage(), e);
	                return new AuthorizationResult(e.getMessage(), false);
	            }
            	
            	try {
            		if (barteredResource(resourceId))
                		bartering = checkBartering(securityRequest, resourceId);
            	} catch (Exception e) {
            		log.error(e.getMessage(), e);
	                return new AuthorizationResult(e.getMessage(), false);
            	}
            	
            	//TODO check if resource is bartered or not
            	if (barteredResource(resourceId)) {
            		if ((checkedPolicies.size() == 1)&&(bartering)) {
	                    return new AuthorizationResult("ok", true);
	                    
	                } else if (checkedPolicies.size() != 1){
	                	return new AuthorizationResult("The federated resource access policy was not satisfied",
	                            false);
            		} else if (!bartering) {
	                    return new AuthorizationResult("The bartering rights were not satisfied",
	                            false);
	                } else {
	                    return new AuthorizationResult("Error in checking federation access policy or bartering rights",
	                            false);
	                }
            		
            	} else {
            		if (checkedPolicies.size() == 1) {
	                    return new AuthorizationResult("ok", true);
	                } else {
	                    return new AuthorizationResult("The federated resource access policy was not satisfied",
	                            false);
	                }
            	}	
            }
 
        } else {
            log.debug("checkAccess: Security is disabled");
            //if security is disabled in properties
            return new AuthorizationResult("Security disabled", true);
        }
    }
    
	/**
     * checking bartering rights
     * @param securityRequest
     * @param federatedResourceId
     * @return
     */
    private boolean checkBartering(SecurityRequest securityRequest, String federatedResourceId) {
    	boolean result = false;
    	Optional<ResourceInfo> optionalResourceInfo = resourceRepo.findById(federatedResourceId);
    	    	
    	if(!optionalResourceInfo.isPresent()) {
            log.error("No ResourceInfo for federatedResourceId={}.", federatedResourceId);
            return false;
        }
    	
    	try {
	    	ResourceInfo resourceInfo = optionalResourceInfo.get();
	    	
	    	//checking federation info - there will be only one federation info, lambda not needed??
//	    	resourceInfo.getFederationInfo().getSharingInformation().entrySet().stream()
//	    		.filter(entry -> entry.getValue().getSharingDate().getTime() < currentTime)
//	    		.map(entry -> entry.getKey())
//	    		.forEach(federationId -> {
	    			
	    			//if (federationId.equals(anObject))
	    			
//	    			clientPlatformId = securityRequest.getSecurityRequestHeaderParamsgetClass()
	    			//
	    	
	    			String federationId = resourceInfo.getFederationInfo().getSharingInformation().keySet().iterator().next();
	    	
	    			//get client platform ID and federation id from the security request
	    			String tokenString = securityRequest.getSecurityCredentials().iterator().next().getToken();
	    			
	    			Token token = null;
					try {
						token = new Token(tokenString);
					} catch (ValidationException e) {
			            log.error("Token is not valid", tokenString);
					}
					
	    			Type type = token.getType();
    				Claims claims = token.getClaims();
    		    	String clientPlatformID = null;

	    			
	    			if (type==Type.HOME) {
	    				clientPlatformID = claims.getIssuer();
	    			}
	    			else if (type == Type.FOREIGN) {
	    				clientPlatformID = claims.getSubject();
	    			}
	  
	    			barteringTradingService.addRequest(clientPlatformID, federationId, federatedResourceId, Coupon.Type.DISCRETE);
	    			result =  barteringTradingService.sendToCheck();		
	    			
//	    		});
            
        } catch (Exception e) {
            log.error("Exception thrown during checking bartering rights: " + e.getMessage(), e);
        }
    	
		return result;		
	}

	/**
     * method checks if the client can access a federated resource
     * @param securityRequest
     * @param federatedResourceId
     * @return
     */
    private Set<String> checkFederatedResourcePolicies(SecurityRequest securityRequest, String federatedResourceId) {
    	Optional<ResourceInfo> optionalResourceInfo = resourceRepo.findById(federatedResourceId);
    	if(!optionalResourceInfo.isPresent()) {
            log.error("No ResourceInfo for federatedResourceId={}.", federatedResourceId);
            return Collections.emptySet();
        }
    	
    	try {
	    	ResourceInfo resourceInfo = optionalResourceInfo.get();
	    	long currentTime = System.currentTimeMillis();
	    	
	    	Map<String, IAccessPolicy> resourceAccessPolicyMap = new HashMap<>();
	    	
	    	//security policy
	    	
	    	//creation of federation policy
	    	resourceInfo.getFederationInfo().getSharingInformation().entrySet().stream()
	    		.filter(entry -> entry.getValue().getSharingDate().getTime() < currentTime)
	    		.map(entry -> entry.getKey())
	    		.forEach(federationId -> {
	    			IAccessPolicy federationAccessPolicy = null;
	    			Set<String> federationMembers;
					try {
						Map<String, String> claims = new HashMap<>();
						federationMembers = getFederationMembers(federationId);
						log.info(federationMembers.toString());
						federationAccessPolicy = new SingleFederatedTokenAccessPolicy(federationId, federationMembers, clientId, claims, false);
					} catch (InvalidArgumentsException e) {
						log.error("error creating federationAccessPolicy" + e.getMessage(), e);
					}
	    			
	    			resourceAccessPolicyMap.put(federationId, federationAccessPolicy);  			
	    		});

            return componentSecurityHandler.getSatisfiedPoliciesIdentifiers(resourceAccessPolicyMap, securityRequest);
        } catch (Exception e) {
            log.error("Exception thrown during checking federation policies: " + e.getMessage(), e);
        }
        
		return Collections.emptySet();
	}

    /**
     * method finds the platformIds of a federation with id federationId
     * @param federationId
     * @return platformIds
     */
    private Set<String> getFederationMembers(String federationId) {
    	Set<String> fedMembersString = new HashSet<>();
    	
    	Federation fed = fedRepo.findById(federationId);
    	List<FederationMember> fedMembers = fed.getMembers();
    	for (int i = 0; i < fedMembers.size(); i++) {
			fedMembersString.add(fedMembers.get(i).getPlatformId());
		}
    	
		return fedMembersString;
	}

	private boolean isCoreResourceId(String resourceId) {
    	Optional<ResourceInfo> optionalResourceInfo = resourceRepo.findById(resourceId);
    	if(!optionalResourceInfo.isPresent()) {
            log.error("No ResourceInfo for resource");
            return true;
        }
    	
		return optionalResourceInfo.get().getFederationInfo() == null;
	}
	
	private boolean barteredResource(String resourceId) {
		Optional<ResourceInfo> optionalResourceInfo = resourceRepo.findById(resourceId);
		
		return optionalResourceInfo.get().getFederationInfo().getSharingInformation().values().iterator().next().getBartering();
	}

	public ServiceRequest getServiceRequestHeaders(){
        if (securityEnabled) {
            try {
                Map<String, String> securityRequestHeaders = null;        
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);

                SecurityRequest securityRequest = componentSecurityHandler.generateSecurityRequestUsingLocalCredentials();
                securityRequestHeaders = securityRequest.getSecurityRequestHeaderParams();

                for (Map.Entry<String, String> entry : securityRequestHeaders.entrySet()) {
                    httpHeaders.add(entry.getKey(), entry.getValue());
                }
                log.info("request headers: " + httpHeaders);
                
                return new ServiceRequest(httpHeaders, true);
            } catch (SecurityHandlerException | JsonProcessingException e) {
                log.error("Fail to take header", e);
                return new ServiceRequest(new HttpHeaders(), false);
            }
        } else {
            log.debug("generateServiceRequest: Security is disabled");
            return new ServiceRequest(new HttpHeaders(), false);
        }
    }

    public ServiceResponseResult generateServiceResponse() {
        if (securityEnabled) {
            try {
                String serviceResponse = componentSecurityHandler.generateServiceResponse();
                return new ServiceResponseResult(serviceResponse, true);
            } catch (SecurityHandlerException e) {
                log.error(e.getMessage(), e);
                return new ServiceResponseResult("", false);
            }
        } else {
            log.debug("generateServiceResponse: Security is disabled");
            return new ServiceResponseResult("", false);
        }
    }

    private void enableSecurity() throws SecurityHandlerException {
        securityEnabled = true;
        componentSecurityHandler = ComponentSecurityHandlerFactory.getComponentSecurityHandler(
                keystoreName,
                keystorePass,
                "rap@" + clientId,
                localAamAddress,
                componentOwnerName,
                componentOwnerPassword);
        
        // workaround to speed up following calls
        componentSecurityHandler.generateServiceResponse();

    }
    
    private Set<String> checkStoredResourcePolicies(SecurityRequest request, String resourceId) {
        Set<String> ids = null;
        try {       

            log.debug("Received a security request : " + request.toString());
             // building dummy access policy
            Map<String, IAccessPolicy> accessPolicyMap = new HashMap<>();
            // to get policies here
            Optional<AccessPolicy> accPolicy = accessPolicyRepo.findById(resourceId);
            if(accPolicy == null) {
                log.error("No access policies for resource");
                return Collections.emptySet();
            }

            accessPolicyMap.put(resourceId, accPolicy.get().getPolicy());
            String mapString = accessPolicyMap.entrySet().stream().map(entry -> entry.getKey() + " - " + entry.getValue())
                    .collect(Collectors.joining(", "));
            log.info("accessPolicyMap: " + mapString);
            log.info("request: " + request.toString());

            ids = componentSecurityHandler.getSatisfiedPoliciesIdentifiers(accessPolicyMap, request);
        } catch (Exception e) {
            log.error("Exception thrown during checking policies: " + e.getMessage(), e);
        }
        
        return ids;
    }

    /**
     * Setters and Getters
     * @return component security handler
     */

    public IComponentSecurityHandler getComponentSecurityHandler() {
        return componentSecurityHandler;
    }

    public void setComponentSecurityHandler(IComponentSecurityHandler componentSecurityHandler) {
        this.componentSecurityHandler = componentSecurityHandler;
    }
}