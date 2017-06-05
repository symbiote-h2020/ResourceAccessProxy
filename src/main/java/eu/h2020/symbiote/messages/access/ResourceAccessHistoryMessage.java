/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.messages.access;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.h2020.symbiote.resources.db.ResourceInfo;
import eu.h2020.symbiote.resources.query.Query;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
public class ResourceAccessHistoryMessage extends ResourceAccessMessage {
    
    @JsonProperty("resourceInfo")
    ResourceInfo resInfo;
    
    @JsonProperty("top")
    int top;
    
    @JsonProperty("filter")
    private final Query filter;
    /**
     * JSON Constructor
     * @param resInfo               the resource data information   
     * @param top                   the size of the requested collection
     * @param filter                the filter for query  
     */
    @JsonCreator
    public ResourceAccessHistoryMessage(@JsonProperty("resourceInfo") ResourceInfo resInfo,
            @JsonProperty("top") int top,
            @JsonProperty("filter") Query filter){
        this.accessType = AccessType.HISTORY;
        this.resInfo = resInfo;
        this.top = top;
        this.filter = filter;
    }
    
    @JsonProperty("resourceInfo")
    public ResourceInfo getResourceInfo() {
        return resInfo;
    }
    
    public int getTop() {
        return top;
    }
    
    public Query getFilter(){
        return this.filter;
    }
 
}
