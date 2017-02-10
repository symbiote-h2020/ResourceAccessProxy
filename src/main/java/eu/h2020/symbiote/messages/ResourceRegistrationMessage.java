/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
	@JsonSubTypes.Type(value = RegisterResourceMessage.class,   name = "REGISTER_RESOURCE"),
        @JsonSubTypes.Type(value = UnregisterResourceMessage.class, name = "UNREGISTER_RESOURCE"),
        @JsonSubTypes.Type(value = UpdateResourceMessage.class, name = "UPDATE_RESOURCE")
})
abstract public class ResourceRegistrationMessage extends RegistrationMessage {
    
    String resourceId;
    
    @JsonProperty("symbiote_id")
    public String getResourceId() {
        return resourceId;
    }
}
