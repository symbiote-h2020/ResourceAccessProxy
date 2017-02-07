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
public class RegisterPluginMessage extends PluginRegistrationMessage {
    @JsonProperty("platformName") 
    private final String platformName;
    
    @JsonCreator
    public RegisterPluginMessage(@JsonProperty("platformId") String platformId, 
                                 @JsonProperty("platformName") String platformName) {
        this.actionType = RegistrationMessage.RegistrationAction.REGISTER_PLUGIN;
        this.platformId = platformId;
        this.platformName = platformName;
    }
    
    @JsonProperty("platformName")
    public String getPlatformName() {
        return platformName;
    }
}
