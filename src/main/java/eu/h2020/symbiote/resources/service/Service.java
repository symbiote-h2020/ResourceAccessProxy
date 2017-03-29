/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.resources.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 *
 * @author luca-
 */
@Document(collection="services")
public class Service {
    
    @Id
    @JsonProperty("serviceId")
    private final String ID;
    
    
    @JsonCreator
    public Service(@JsonProperty("serviceId") String ID) {
        this.ID = ID;
    }
    
    @JsonProperty("serviceId")
    public String getserviceId() {
        return ID;
    }
}

