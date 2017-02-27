/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.resources;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
public class ResourceInfo {
    
    @Id
    @JsonProperty("resourceId")
    private final String resourceId;
    @JsonProperty("platformResourceId")
    private final String platformResourceId;
    @JsonProperty("platformId")
    private final String platformId;
    
    public ResourceInfo() {
        this.resourceId = "";
        this.platformResourceId = "";
        this.platformId = "";
    }
    
    @JsonCreator
    public ResourceInfo(@JsonProperty("resourceId") String resourceId, 
                        @JsonProperty("platformResourceId") String platformResourceId, 
                        @JsonProperty("platformId") String platformId) {
        this.resourceId = resourceId;
        this.platformResourceId = platformResourceId;
        this.platformId = platformId;
    }
    
    public String getResourceId() {
        return resourceId;
    }
    
    public String getPlatformResourceId() {
        return platformResourceId;
    }
    
    public String getPlatformId() {
        return platformId;
    }
}
