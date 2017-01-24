/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
public class RegisterResourceMessage extends ResourceRegistrationMessage {
    private final String platformResourceId;
    private final String platformId;
    
    @JsonCreator
    public RegisterResourceMessage(String resourceId, String platformResourceId, String platformId) {
        this.actionType = RegistrationAction.REGISTER_RESOURCE;
        this.resourceId = resourceId;
        this.platformResourceId = platformResourceId;
        this.platformId = platformId;
    }
    
    @JsonProperty("platformResourceId")
    public String getPlatformResourceId() {
        return platformResourceId;
    }
    
    @JsonProperty("platformId")
    public String getPlatformId() {
        return platformId;
    }
}
