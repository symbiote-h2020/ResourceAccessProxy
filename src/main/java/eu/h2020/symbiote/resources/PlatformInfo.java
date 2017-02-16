/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.resources;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
@Entity
@Table(name = "platform_plugins")
public class PlatformInfo {
    @Id
    @JsonProperty("platformId")
    private final String platformId;
    @JsonProperty("platformName")
    private final String platformName;
    
    public PlatformInfo() {
        platformId = "";
        platformName = "";
    }
    
    @JsonCreator
    public PlatformInfo(@JsonProperty("platformId") String platformId, 
                        @JsonProperty("platformName") String platformName) {
        this.platformId = platformId;
        this.platformName = platformName;
    }
    
    public String getPlatformId() {
        return platformId;
    }
    
    public String getPlatformName() {
        return platformName;
    }
}