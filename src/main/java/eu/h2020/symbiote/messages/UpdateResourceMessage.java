/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.h2020.symbiote.model.data.Location;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
public class UpdateResourceMessage extends ResourceRegistrationMessage {
    @JsonProperty("internal_id")
    private final String platformResourceId;
    //private final String platformId;
    @JsonProperty("name")
    private final String resourceName;
    @JsonProperty("location")
    private final Location location;
    
    @JsonCreator
    public UpdateResourceMessage(@JsonProperty("id") String resourceId, 
                                 @JsonProperty("internal_id") String platformResourceId, 
                                 @JsonProperty("name") String resourceName, 
                                 @JsonProperty("location") Location location) {
        this.actionType = RegistrationAction.REGISTER_RESOURCE;
        this.resourceId = resourceId;
        this.platformResourceId = platformResourceId;
        //this.platformId = platformId;
        this.resourceName = resourceName;
        this.location = location;
    }
    
    @JsonProperty("internal_id")
    public String getPlatformResourceId() {
        return platformResourceId;
    }
    /*
    @JsonProperty("platformId")
    public String getPlatformId() {
        return platformId;
    }*/
    
    @JsonProperty("name")
    public String getResourceName() {
        return resourceName;
    }
    
    @JsonProperty("location")
    public Location getLocation() {
        return location;
    }
}
