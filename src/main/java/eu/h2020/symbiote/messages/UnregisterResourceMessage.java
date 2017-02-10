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
public class UnregisterResourceMessage extends ResourceRegistrationMessage {
    
    @JsonCreator
    public UnregisterResourceMessage(@JsonProperty("id") String resourceId) {
        this.actionType = RegistrationAction.UNREGISTER_RESOURCE;
        this.resourceId = resourceId;
    }
}
