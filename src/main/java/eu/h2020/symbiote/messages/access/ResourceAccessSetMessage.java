/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.messages.access;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.h2020.symbiote.cloud.model.data.parameter.InputParameter;
import eu.h2020.symbiote.resources.db.ResourceInfo;
import java.util.List;

/**
 *
* @author Luca Tomaselli <l.tomaselli@nextworks.it>
*/
public class ResourceAccessSetMessage extends ResourceAccessMessage{
    @JsonProperty("resourceInfo")
    private final ResourceInfo resInfo;
    
    @JsonProperty("body")
    private final String body;
    
    @JsonProperty("requestInfo")
    private List<RequestInfo> requestInfo;
    
    /**
     * JSON Constructor
     * @param resInfo               the resource data information
     * @param body                  the body of request
     */
    @JsonCreator
    public ResourceAccessSetMessage(@JsonProperty("resourceInfo") ResourceInfo resInfo, 
                                    @JsonProperty("body") String body) {
        this.accessType = ResourceAccessMessage.AccessType.SET;
        this.resInfo = resInfo;
        this.body = body;
        requestInfo = null;
    }
    
    /**
     * JSON Constructor
     * @param resInfo               the resource data information
     * @param body                  the body of request
     * @param requestInfo           the path of request
     */
    @JsonCreator
    public ResourceAccessSetMessage(@JsonProperty("resourceInfo") ResourceInfo resInfo, 
                                    @JsonProperty("body") String body,
                                    @JsonProperty("requestInfo") List<RequestInfo> requestInfo) {
        this.accessType = ResourceAccessMessage.AccessType.SET;
        this.resInfo = resInfo;
        this.body = body;
        this.requestInfo = requestInfo;
    }
    
    @JsonProperty("resourceInfo")
    public ResourceInfo getResourceInfo() {
        return resInfo;
    }
    
    @JsonProperty("body")
    public String getBody() {
        return body;
    }
}
