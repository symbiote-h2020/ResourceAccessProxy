/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.h2020.symbiote.resources.ResourceInfo;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
public class ResourceAccessGetMessage extends ResourceAccessMessage {
    
    /**
     * JSON Constructor
     * @param resInfo               the resource data information     
     */
    @JsonCreator
    public ResourceAccessGetMessage(@JsonProperty("resourceInfo") ResourceInfo resInfo){
        this.accessType = AccessType.GET;
        this.resInfo = resInfo;
    }
}
