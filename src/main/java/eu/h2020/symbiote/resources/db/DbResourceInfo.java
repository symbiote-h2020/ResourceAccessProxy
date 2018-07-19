/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.resources.db;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import eu.h2020.symbiote.cloud.model.rap.ResourceInfo;

/**
 *
 * @author Matteo Pardi
 */
@Document(collection="resources")
public class DbResourceInfo {
    
    @Id
    private String id;
    private String internalId;
    private String type;
    private List<String> observedProperties;
    private List<String> sessionIdList;
    private String pluginId;
    
    public DbResourceInfo() {
        this.id = "";
        this.internalId = "";
        this.pluginId = null;
        this.observedProperties = null;
        this.sessionIdList = null;
        this.type = null;
    }
    
    public DbResourceInfo(String resourceId, String platformResourceId) {
        this.id = resourceId;
        this.internalId = platformResourceId;
        this.pluginId = null;
        this.observedProperties = null;
        this.sessionIdList = null;       
        this.type = null;
    }
    
    public String getSymbioteId() {
        return id;
    }
    
    public void setSymbioteId(String symbioteId) {
        this.id = symbioteId;
    }
    
    public String getInternalId() {
        return internalId;
    }
    
    public void setInternalId(String internalId) {
        this.internalId = internalId;
    }
    
    public List<String> getObservedProperties() {
        return observedProperties;
    }    
    
    public void setObservedProperties(List<String> observedProperties) {
        this.observedProperties = observedProperties;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public List<String> getSessionId() {
        return sessionIdList;
    }
    
    public void setSessionId(List<String> sessionIdList) {
        this.sessionIdList = sessionIdList;
    }
    
    public void addToSessionList(String sessionId) {
        if(this.sessionIdList == null)
            this.sessionIdList = new ArrayList<>();
        this.sessionIdList.add(sessionId);
    }
    
    public String getPluginId() {
        return pluginId;
    }
    
    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public ResourceInfo toResourceInfo() {
        ResourceInfo ri = new ResourceInfo();
        ri.setSymbioteId(getSymbioteId());
        ri.setInternalId(getInternalId());
        ri.setPluginId(getPluginId());
        ri.setObservedProperties(getObservedProperties());
        ri.setSessionId(getSessionId());
        ri.setType(getType());
        return ri;
    }
    
    public static List<ResourceInfo> toResourceInfos(List<DbResourceInfo> infos) {
        return infos.stream().map(ri -> ri.toResourceInfo()).collect(Collectors.toList());
    }
}
