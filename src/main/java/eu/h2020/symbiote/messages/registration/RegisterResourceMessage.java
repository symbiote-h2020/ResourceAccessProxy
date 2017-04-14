/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.messages.registration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.h2020.symbiote.core.model.resources.Resource;


/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
public class RegisterResourceMessage extends ResourceRegistrationMessage {
    
    @JsonProperty("host")
    private final String host;
    @JsonProperty("resource")
    private final Resource resource;
    
    @JsonCreator
    public RegisterResourceMessage(@JsonProperty("host") String host,
                                   @JsonProperty("resource") Resource resource) {
        this.actionType = RegistrationAction.REGISTER_RESOURCE;
        this.host = host;
        this.resource = resource;
    }
    
    @JsonProperty("host")
    public String getHost() {
        return host;
    }
    
    @JsonProperty("resource")
    public Resource getResource() {
        return resource;
    }
    
    
    
    /*@JsonProperty("internalId")
    private final String platformResourceId;
    //private final String platformId;
    @JsonProperty("name")
    private final String resourceName;
    
    @JsonProperty("location")
    private final Location location;
    
    @JsonProperty("observedProperties")
    private final List<String> obsProps;

    @JsonProperty("resourceURL")
    private final String url;
    
    @JsonProperty("description")
    private final String description;
    
    @JsonProperty("owner")
    private final String owner;
    
    
    @JsonCreator
    public RegisterResourceMessage(@JsonProperty("id") String resourceId, 
                                   @JsonProperty("internalId") String platformResourceId, 
                                   @JsonProperty("name") String resourceName, 
                                   @JsonProperty("location") Location location,
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
    }*/
    
}
