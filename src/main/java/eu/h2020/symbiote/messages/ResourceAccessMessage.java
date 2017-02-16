/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.messages;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import eu.h2020.symbiote.resources.ResourceInfo;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 * 
 * Class modeling a message exchanged on the queue from the RAP to the plugin-RAP
 * in order to access to a resource through the platform layer
 *
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
	@Type(value = ResourceAccessGetMessage.class,       name = "GET"),
        @Type(value = ResourceAccessHistoryMessage.class,   name = "HISTORY"),
        @Type(value = ResourceAccessSetMessage.class,       name = "SET")
})
abstract public class ResourceAccessMessage {
    
    public enum AccessType {
        GET, HISTORY, SET
    }
    
    @JsonProperty("type")
    AccessType accessType;
    @JsonProperty("resourceInfo")
    ResourceInfo resInfo;

    @JsonProperty("type")
    public AccessType getAccessType() {
        return accessType;
    }
    
    @JsonProperty("resourceInfo")
    public ResourceInfo getResourceInfo() {
        return resInfo;
    }
}