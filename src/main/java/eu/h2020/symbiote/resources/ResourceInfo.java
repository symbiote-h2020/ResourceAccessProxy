/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.resources;

import com.fasterxml.jackson.annotation.JsonCreator;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
@Entity
@Table(name = "resources")
public class ResourceInfo {
    
    @Id
    private final String resourceId;
    private final String platformResourceId;
    private final String platformId;
    
    @JsonCreator
    public ResourceInfo(String resourceId, 
                        String platformResourceId, 
                        String platformId) {
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
