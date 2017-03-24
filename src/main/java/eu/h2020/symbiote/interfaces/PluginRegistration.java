/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.messages.PluginRegistrationMessage;
import eu.h2020.symbiote.messages.RegisterPluginMessage;
import eu.h2020.symbiote.messages.RegistrationMessage.RegistrationAction;
import eu.h2020.symbiote.resources.PlatformInfo;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
public class PluginRegistration {
    
    private static final Logger log = LoggerFactory.getLogger(PluginRegistration.class);

    @Autowired
    PluginRepository pluginRepository;
    
    
    public void receiveMessage(byte[] messageByte) {
        try {
            String message = new String(messageByte, "UTF-8");
            log.debug("Plugin Registration message received.\n" + message);

            ObjectMapper mapper = new ObjectMapper();
            PluginRegistrationMessage msg = mapper.readValue(message, PluginRegistrationMessage.class);            
            String platformId = msg.getPlatformId();
            RegistrationAction type = msg.getActionType();
            switch(type) {
                case REGISTER_PLUGIN: {
                    RegisterPluginMessage mess = (RegisterPluginMessage)msg;
                    String platformName = mess.getPlatformName();
                    log.debug("Registering plugin for platform " + platformId + " with name " + platformName);
                    addPlugin(platformId, platformName);
                    log.info("AddPlugin: platformId=" + platformId + " platformName="+platformName);
                    break;
                }
                case UNREGISTER_PLUGIN: {
                    log.debug("Unregistering plugin for platform with id " + platformId);
                    deletePlugin(platformId);                       
                    break;
                }
                default:
                    throw new Exception("Wrong message format");
            }
        } catch (Exception e) {
            log.info("Error during plugin registration process\n" + e.getMessage());
        }
    }
    
    private void addPlugin(String platformId, String platformName) {
        PlatformInfo platformInfo = new PlatformInfo(platformId, platformName);
        pluginRepository.save(platformInfo);
    }
    
    private void deletePlugin(String platformId) {
        pluginRepository.delete(platformId);
    }
}
