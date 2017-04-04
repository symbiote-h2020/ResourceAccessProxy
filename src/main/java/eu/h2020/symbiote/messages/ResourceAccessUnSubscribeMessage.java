/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.h2020.symbiote.resources.ResourceInfo;
import java.util.List;

/**
 *
 * @author luca-
 */
public class ResourceAccessUnSubscribeMessage extends ResourceAccessMessage {
    
    @JsonProperty("resourceInfoList")
    List<ResourceInfo> resInfoList;
    /**
     * JSON Constructor
     * @param resInfo               the list of resource data information     
     */
    @JsonCreator
    public ResourceAccessUnSubscribeMessage(@JsonProperty("resourceInfo") List<ResourceInfo> resInfoList){
        this.accessType = AccessType.UNSUBSCRIBE;
        this.resInfoList = resInfoList;
    }
    
    @JsonProperty("resourceInfoList")
    public List<ResourceInfo> getResourceInfoList() {
        return resInfoList;
    }
}