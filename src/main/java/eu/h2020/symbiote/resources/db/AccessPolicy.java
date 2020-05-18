/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.resources.db;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import eu.h2020.symbiote.security.accesspolicies.IAccessPolicy;

/**
 *
 * @author Matteo Pardi
 */
@Document(collection="policies")
public class AccessPolicy {
    @Id
    private final String resourceId;    
    private final String internalId;
    private final IAccessPolicy policy;
    
    public AccessPolicy() {
        resourceId = "";
        internalId = "";
        policy = null;
    }
    
    @PersistenceConstructor
    public AccessPolicy(String resourceId, String internalId, IAccessPolicy policy) {
        this.resourceId = resourceId;
        this.internalId = internalId;
        this.policy = policy;
    }
    
    public String getResourceId() {
        return resourceId;
    }
    
    public String getInternalId() {
        return internalId;
    }
    
    public IAccessPolicy getPolicy() {
        return policy;
    }        
}
