/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.messages.access;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
//@JsonInclude(Include.NON_NULL)
public class RequestInfo {
    
    @JsonProperty("obj")
    private String obj;
    @JsonProperty("symbioteId")
    private String id;
    @JsonProperty("internalId")
    private String internalId;

    /**
     * JSON Constructor
     * @param obj  
     * @param id     
     * @param internalId  
     */
    @JsonCreator
    public RequestInfo(@JsonProperty("obj") String obj, 
            @JsonProperty("symbioteId") String id, 
            @JsonProperty("internalId") String internalId) {
        this.obj = obj;
        this.id = id;
        this.internalId = internalId;
    }   
    
    public String getObj() {
        return obj;
    }

    public String getId() {
        return id;
    }

    public String getInternalId() {
        return internalId;
    }
}
