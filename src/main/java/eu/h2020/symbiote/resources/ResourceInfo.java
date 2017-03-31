/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.resources;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import javax.websocket.Session;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
@Document(collection="resources")
public class ResourceInfo {
    
    @Id
    @JsonProperty("resourceId")
    private final String resource_Id;
    @JsonProperty("platformResourceId")
    private final String platformResource_Id;
    @JsonProperty("platformId")
    private final String platform_Id;
    @JsonProperty("sessionIdList")
    private List<String> session_Id_List;
    
    public ResourceInfo() {
        this.resource_Id = "";
        this.platformResource_Id = "";
        this.platform_Id = "";
        this.session_Id_List = null;
    }
    
    @JsonCreator
    public ResourceInfo(@JsonProperty("resourceId") String resourceId, 
                        @JsonProperty("platformResourceId") String platformResourceId, 
                        @JsonProperty("platformId") String platformId) {
        this.resource_Id = resourceId;
        this.platformResource_Id = platformResourceId;
        this.platform_Id = platformId;
        this.session_Id_List = null;
    }
    
    @JsonCreator
    public ResourceInfo(@JsonProperty("resourceId") String resourceId, 
                        @JsonProperty("platformResourceId") String platformResourceId, 
                        @JsonProperty("platformId") String platformId,
                        @JsonProperty("sessionIdList") List<String> sessionId) {
        this.resource_Id = resourceId;
        this.platformResource_Id = platformResourceId;
        this.platform_Id = platformId;
        this.session_Id_List = sessionId;
    }
    
    @JsonProperty("resourceId")
    public String getResourceId() {
        return resource_Id;
    }
    
    @JsonProperty("platformResourceId")
    public String getPlatformResourceId() {
        return platformResource_Id;
    }
    
    @JsonProperty("platformId")
    public String getPlatformId() {
        return platform_Id;
    }
    
    @JsonProperty("sessionIdList")
    public List<String> getSessionId() {
        return session_Id_List;
    }
    
    public void setSessionId(List<String> sessionIdList) {
        this.session_Id_List = sessionIdList;
    }
    
    public void addToSessionList(String sessionId) {
        if(this.session_Id_List == null)
            this.session_Id_List = new ArrayList<String>();
        this.session_Id_List.add(sessionId);
    }
}
