/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.messages.registration;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 *
 * @author Matteo Pardi
 */
public class RegistrationMessage {
    public enum RegistrationAction {
        REGISTER_RESOURCE, UNREGISTER_RESOURCE, REGISTER_PLUGIN, UNREGISTER_PLUGIN
    }
    
    @JsonIgnore
    RegistrationAction actionType;
    
    public RegistrationAction getActionType() {
        return actionType;
    }
}
