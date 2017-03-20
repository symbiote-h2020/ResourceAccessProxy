/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.resources;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
@Document(collection="plugins")
public class PlatformInfo {
    @Id
    @JsonProperty("platformId")
    private final String platform_Id;
    @JsonProperty("platformName")
    private final String platform_Name;
    
    public PlatformInfo() {
        platform_Id = "";
        platform_Name = "";
    }
    
    @JsonCreator
    public PlatformInfo(@JsonProperty("platformId") String platformId, 
                        @JsonProperty("platformName") String platformName) {
        this.platform_Id = platformId;
        this.platform_Name = platformName;
    }
    
    @JsonProperty("platformId")
    public String getPlatformId() {
        return platform_Id;
    }
    
    @JsonProperty("platformName")
    public String getPlatformName() {
        return platform_Name;
    }
}
