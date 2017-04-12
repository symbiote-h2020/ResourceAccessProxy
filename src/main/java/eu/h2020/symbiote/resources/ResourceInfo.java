/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.resources;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
@Document(collection="resources")
public class ResourceInfo {
    
    @Id
    @JsonProperty("symbioteId")
    private final String id;
    @JsonProperty("internalId")
    private final String internalId;
    @JsonProperty("platformId")
    private final String platformId;
    @JsonIgnore
    private List<String> sessionIdList;
    
    public ResourceInfo() {
        this.id = "";
        this.internalId = "";
        this.platformId = "";
        this.sessionIdList = null;
    }
    
    @JsonCreator
    public ResourceInfo(@JsonProperty("symbioteId") String resourceId, 
                        @JsonProperty("internalId") String platformResourceId, 
                        @JsonProperty("platformId") String platformId) {
        this.id = resourceId;
        this.internalId = platformResourceId;
        this.platformId = platformId;
        this.sessionIdList = null;
    }
    
    @JsonProperty("internalId")
    public String getSymbioteId() {
        return id;
    }
    
    @JsonProperty("internalId")
    public String getInternalId() {
        return internalId;
    }
    
    @JsonProperty("platformId")
    public String getPlatformId() {
        return platformId;
    }
    
    @JsonIgnore
    public List<String> getSessionId() {
        return sessionIdList;
    }
    
    @JsonIgnore
    public void setSessionId(List<String> sessionIdList) {
        this.sessionIdList = sessionIdList;
    }
    
    @JsonIgnore
    public void addToSessionList(String sessionId) {
        if(this.sessionIdList == null)
            this.sessionIdList = new ArrayList();
        this.sessionIdList.add(sessionId);
    }
}
