/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.h2020.symbiote.resources.ResourceInfo;
import eu.h2020.symbiote.resources.service.ServiceSet;

/**
 *
 * @author luca-
 */
public class ResourceAccessSetService extends ResourceAccessMessage{
    @JsonProperty("resourceInfo")
    ResourceInfo resInfo;
    
    @JsonProperty("value")
    private final ServiceSet value;
    /**
     * JSON Constructor
     * @param resInfo       the resource data information
     * @param value         the value to set
     */
    @JsonCreator
    public ResourceAccessSetService(@JsonProperty("resourceInfo") ResourceInfo resInfo, 
                                    @JsonProperty("value")ServiceSet value) {
        this.accessType = ResourceAccessMessage.AccessType.SET;
        this.resInfo = resInfo;
        this.value = value;
    }
    
    @JsonProperty("resourceInfo")
    public ResourceInfo getResourceInfo() {
        return resInfo;
    }
    
    public ServiceSet getValue() {
        return value;
    }
}
