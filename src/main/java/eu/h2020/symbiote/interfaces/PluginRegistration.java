/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.interfaces;

import eu.h2020.symbiote.resources.db.PluginRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.messages.registration.PluginRegistrationMessage;
import eu.h2020.symbiote.messages.registration.RegisterPluginMessage;
import eu.h2020.symbiote.messages.registration.RegistrationMessage.RegistrationAction;
import eu.h2020.symbiote.resources.db.PlatformInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Matteo Pardi
 */
public class PluginRegistration {
    
    private static final Logger log = LoggerFactory.getLogger(PluginRegistration.class);

    @Autowired
    PluginRepository pluginRepository;

    /**
     * This method received the registration of a new plugin to the RAP
     *
     * @param messageByte received message
     */
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
                    boolean hasFilters = mess.getHasFilters();
                    boolean hasNotifications = mess.getHasNotifications();
                    log.debug("Registering plugin for platform with id " + platformId);
                    addPlugin(platformId, hasFilters, hasNotifications);
                    log.info("AddPlugin: Id=" + platformId);
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
    
    private void addPlugin(String platformId, boolean hasFilters, boolean hasNotifications) {
        PlatformInfo platformInfo = new PlatformInfo(platformId, hasFilters, hasNotifications);
        pluginRepository.save(platformInfo);
    }
    
    private void deletePlugin(String platformId) {
        pluginRepository.delete(platformId);
    }
}
