/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.resources;

/**
 *
 * @author Matteo Pardi
 */
public class RapDefinitions {
    
    public static final String      RESOURCE_REGISTRATION_EXCHANGE_IN = "symbIoTe.registrationHandler";
    public static final String      RESOURCE_REGISTRATION_KEY = "symbIoTe.rh.resource.core.register";
    public static final String      RESOURCE_REGISTRATION_QUEUE = "symbIoTe.rap.registrationHandler.register_resources";
    public static final String      RESOURCE_UNREGISTRATION_KEY = "symbIoTe.rh.resource.core.delete";
    public static final String      RESOURCE_UNREGISTRATION_QUEUE = "symbIoTe.rap.registrationHandler.unregister_resources";
    public static final String      RESOURCE_UPDATE_KEY = "symbIoTe.rh.resource.core.update";
    public static final String      RESOURCE_UPDATE_QUEUE = "symbIoTe.rap.registrationHandler.update_resources";

    /*
    public static final String      RESOURCE_ACCESS_EXCHANGE_IN = "symbIoTe.rap.accessResource";
    public static final String[]    RESOURCE_READ_KEYS  = {"symbIoTe.rap.accessResource.readResource.*"};
    public static final String      RESOURCE_READ_QUEUE = "symbiote-rap-accessResource-readResource";   
    public static final String[]    RESOURCE_WRITE_KEYS  = {"symbIoTe.rap.accessResource.writeResource.*"};
    public static final String      RESOURCE_WRITE_QUEUE = "symbIoTe-rap-accessResource-writeResource";
    */
    
    public static final String      PLUGIN_REGISTRATION_EXCHANGE_IN = "symbIoTe.rapPluginExchange";
    public static final String      PLUGIN_REGISTRATION_KEY = "symbIoTe.rapPluginExchange.add-plugin";
    public static final String      PLUGIN_REGISTRATION_QUEUE = "symbIoTe.platform-queue";

    public static final String      PLUGIN_EXCHANGE_OUT = "plugin-exchange";
    
    public static final String      PLUGIN_NOTIFICATION_QUEUE = "symbIoTe.platform-queue-notification";
    public static final String      PLUGIN_NOTIFICATION_EXCHANGE_IN = "symbIoTe.rapPluginExchange-notification";
    public static final String      PLUGIN_NOTIFICATION_KEY = "symbIoTe.rapPluginExchange.plugin-notification";

    //for sending access data to Monitoring and B&T Manager
    public static final String 		RAP_ACCESS_EXCHANGE = "symbIoTe.resourceAccessProxy";
	public static final String 		RAP_ACCESS_ROUTING_KEY = "symbIoTe.rap.resource.access";
	public static final String 		RAP_BARTERING_ROUTING_KEY = "symbIoTe.rap.btm.access";

	
	//for receiving L2 registration&share messages
	//queues
    public static final String      RESOURCE_L2_UPDATE_QUEUE = "symbIoTe.rap.registrationHandler.l2.update_resources";
    public static final String      RESOURCE_L2_UNREGISTRATION_QUEUE = "symbIoTe.rap.registrationHandler.l2.unregister_resources";
    public static final String      RESOURCE_L2_SHARE_QUEUE = "symbIoTe.rap.registrationHandler.l2.share_resources";
    public static final String      RESOURCE_L2_UNSHARE_QUEUE = "symbIoTe.rap.registrationHandler.l2.unshare_resources";

    //keys
	public static final String 		ROUTING_KEY_RH_UPDATED = "symbIoTe.rh.resource.updated";
	public static final String 		ROUTING_KEY_RH_DELETED = "symbIoTe.rh.resource.deleted";
	public static final String 		ROUTING_KEY_RH_SHARED = "symbIoTe.rh.resource.shared";
	public static final String 		ROUTING_KEY_RH_UNSHARED = "symbIoTe.rh.resource.unshared";
	
    public static final String      JSON_OBJECT_TYPE_FIELD_NAME = "@type";
    
    //exchange and queues for federation info
    public static final String 		FEDERATION_EXCHANGE = "symbIoTe.federation";
    public static final String 		FEDERATION_KEY_CREATED = "symbIoTe.federation.created";
    public static final String 		FEDERATION_QUEUE_CREATED = "symbIoTe.federation.queue.created";
    public static final String 		FEDERATION_KEY_CHANGED = "symbIoTe.federation.changed";
    public static final String 		FEDERATION_QUEUE_CHANGED = "symbIoTe.federation.queue.changed";
    public static final String 		FEDERATION_KEY_DELETED = "symbIoTe.federation.deleted";
    public static final String 		FEDERATION_QUEUE_DELETED = "symbIoTe.federation.queue.deleted";
}
