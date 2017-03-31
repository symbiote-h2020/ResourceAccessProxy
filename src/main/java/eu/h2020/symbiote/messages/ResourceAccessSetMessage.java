/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.h2020.symbiote.model.data.ObservationValue;
import eu.h2020.symbiote.resources.ResourceInfo;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
public class ResourceAccessSetMessage extends ResourceAccessMessage {
    
    @JsonProperty("resourceInfo")
    ResourceInfo resInfo;
    
    @JsonProperty("value")
    private final ObservationValue value;
    /**
     * JSON Constructor
     * @param resInfo       the resource data information
     * @param value         the value to set
     */
    @JsonCreator
    public ResourceAccessSetMessage(@JsonProperty("resourceInfo") ResourceInfo resInfo, 
                                    @JsonProperty("value")ObservationValue value) {
        this.accessType = AccessType.SET;
        this.resInfo = resInfo;
        this.value = value;
    }
    
    @JsonProperty("resourceInfo")
    public ResourceInfo getResourceInfo() {
        return resInfo;
    }
    
    public ObservationValue getValue() {
        return value;
    }
}
