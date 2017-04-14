/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.resources.db;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.h2020.symbiote.core.model.Property;
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
    @JsonProperty("observedProperties")
    private List<String> observedProperties;
    @JsonIgnore
    private List<String> sessionIdList;
    
    public ResourceInfo() {
        this.id = "";
        this.internalId = "";
        this.observedProperties = null;
        this.sessionIdList = null;
    }
    
    @JsonCreator
    public ResourceInfo(@JsonProperty("symbioteId") String resourceId, 
                        @JsonProperty("internalId") String platformResourceId) {
        this.id = resourceId;
        this.internalId = platformResourceId;
        this.observedProperties = null;
        this.sessionIdList = null;
    }
    
    @JsonProperty("symbioteId")
    public String getSymbioteId() {
        return id;
    }
    
    @JsonProperty("internalId")
    public String getInternalId() {
        return internalId;
    }
    
    @JsonProperty("observedProperties")
    public List<String> getObservedProperties() {
        return observedProperties;
    }    
    
    @JsonProperty("observedProperties")
    public void setObservedProperties(List<String> observedProperties) {
        this.observedProperties = observedProperties;
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
