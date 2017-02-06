/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.resources;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
public class RapDefinitions {
    
    public static final String      RESOURCE_REGISTRATION_EXCHANGE_IN = "symbIoTe.platformExchange";
    public static final String[]    RESOURCE_REGISTRATION_KEYS  = { "symbIoTe.platformexchange.rh.rap.register_resources", 
                                                                    "symbIoTe.platformexchange.rh.rap.unregister_resources",
                                                                    "symbIoTe.platformexchange.rh.rap.update_resources"};
    public static final String      RESOURCE_REGISTRATION_QUEUE = "symbiote-rap-queue";
    
    public static final String      RESOURCE_ACCESS_EXCHANGE_IN = "symbIoTe.rap";
    public static final String[]    RESOURCE_READ_KEYS  = {"symbIoTe.rap.readResource.*"};
    public static final String      RESOURCE_READ_QUEUE = "symbiote-rap-readResource";   
    public static final String[]    RESOURCE_WRITE_KEYS  = {"symbIoTe.rap.writeResource.*"};
    public static final String      RESOURCE_WRITE_QUEUE = "symbiote-rap-writeResource";
    
    public static final String      PLUGIN_REGISTRATION_EXCHANGE_IN = "symbIoTe.rapPluginExchange";
    public static final String[]    PLUGIN_REGISTRATION_KEYS = {"symbIoTe.rapPluginExchange.add-plugin"};
    public static final String      PLUGIN_REGISTRATION_QUEUE = "symbIoTe.platform-queue";

    public static final String      PLUGIN_EXCHANGE_OUT = "plugin-exchange";

}
