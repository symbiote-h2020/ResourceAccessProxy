/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.messages.registration;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 *
 * @author Matteo Pardi
 */
public class UnregisterPluginMessage extends PluginRegistrationMessage {
    
    @JsonCreator
    public UnregisterPluginMessage(String platformId) {
        this.actionType = RegistrationAction.UNREGISTER_PLUGIN;
        this.platformId = platformId;
    }
}
