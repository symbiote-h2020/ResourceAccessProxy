/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.h2020.symbiote.model.data.Location;
import eu.h2020.symbiote.model.data.WGS84Location;
import java.util.List;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
public class UpdateResourceMessage extends ResourceRegistrationMessage {
    @JsonProperty("internalId")
    private final String platformResourceId;
    //private final String platformId;
    @JsonProperty("name")
    private final String resourceName;
    
    @JsonProperty("location")
    private final WGS84Location location;
    
    @JsonProperty("observedProperties")
    private final List<String> obsProps;

    @JsonProperty("resourceURL")
    private final String url;
    
    @JsonProperty("description")
    private final String description;
    
    @JsonProperty("owner")
    private final String owner;
    
    
    @JsonCreator
    public UpdateResourceMessage(@JsonProperty("id") String resourceId, 
                                   @JsonProperty("internalId") String platformResourceId, 
                                   @JsonProperty("name") String resourceName, 
                                   @JsonProperty("location") WGS84Location location,
                                   @JsonProperty("observedProperties") List<String> obsProps, 
                                   @JsonProperty("resourceURL") String url, 
                                   @JsonProperty("description") String description, 
                                   @JsonProperty("owner") String owner) {
        this.actionType = RegistrationAction.REGISTER_RESOURCE;
        this.resourceId = resourceId;
        this.platformResourceId = platformResourceId;
        //this.platformId = platformId;
        this.resourceName = resourceName;
        this.location = location;
        this.obsProps = obsProps;
        this.url = url;
        this.description = description;
        this.owner = owner;
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
    
    @JsonProperty("observedProperties")
    public List<String> getObsProps() {
        return obsProps;
    }

    @JsonProperty("resourceURL")
    public String getUrl() {
        return url;
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("owner")
    public String getOwner() {
        return owner;
    }
}
