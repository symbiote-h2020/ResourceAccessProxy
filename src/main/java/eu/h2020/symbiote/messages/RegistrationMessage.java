/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.messages;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
public class RegistrationMessage {
    public enum RegistrationAction {
        REGISTER_RESOURCE, UNREGISTER_RESOURCE, REGISTER_PLUGIN, UNREGISTER_PLUGIN
    }
    
    RegistrationAction actionType;
    
    @JsonProperty("type")
    public RegistrationAction getActionType() {
        return actionType;
    }
}
