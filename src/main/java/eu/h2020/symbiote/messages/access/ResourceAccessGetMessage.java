/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.messages.access;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.h2020.symbiote.resources.db.ResourceInfo;
import java.util.List;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
public class ResourceAccessGetMessage extends ResourceAccessMessage {
    
    @JsonProperty("resourceInfo")
    ResourceInfo resInfo;
    
    @JsonProperty("requestInfo")
    List<RequestInfo> requestInfo;
    
    /**
     * JSON Constructor
     * @param resInfo               the resource data information   
     * @param requestInfo           the path of request
     */
    @JsonCreator
    public ResourceAccessGetMessage(@JsonProperty("resourceInfo") ResourceInfo resInfo, 
            @JsonProperty("requestInfo") List<RequestInfo> requestInfo){
        this.accessType = AccessType.GET;
        this.resInfo = resInfo;
        this.requestInfo = requestInfo;
    }
    
    @JsonProperty("resourceInfo")
    public ResourceInfo getResourceInfo() {
        return resInfo;
    }
    
    @JsonProperty("requestInfo")
    public List<RequestInfo> getRequestInfo() {
        return requestInfo;
    }
}
