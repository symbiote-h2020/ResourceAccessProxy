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
    public static final String      RAP_EXCHANGE_IN = "rap-exchange";

    public static final String[]    NORTHBOUND_KEYS = {"symbiote.rap.register", "symbiote.rap.unregister"};
    public static final String      NORTHBOUND_QUEUE = "symbiote-rap-queue";

    public static final String[]    PLATFORM_KEYS = {"symbiote.rap.add-plugin"};
    public static final String      PLATFORM_QUEUE = "platform-queue";

    public static final String      PLUGINS_EXCHANGE_OUT = "plugins-exchange";

}
