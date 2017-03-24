/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.resources;

import eu.h2020.symbiote.interfaces.PluginRegistration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Class responsible for the instantiation 
 * and configuration of the message queues.
 *
 */
@Configuration
public class PluginQueueConfig {
    
    @Bean(name=RapDefinitions.PLUGIN_EXCHANGE_OUT)
    TopicExchange exchangeOut() {
        return new TopicExchange(RapDefinitions.PLUGIN_EXCHANGE_OUT, false, false);
    }

    @Bean(name=RapDefinitions.PLUGIN_REGISTRATION_EXCHANGE_IN)
    TopicExchange pluginRegistrationExchangeIn() {
        return new TopicExchange(RapDefinitions.PLUGIN_REGISTRATION_EXCHANGE_IN, false, false);
    }
    
    @Bean(name=RapDefinitions.PLUGIN_REGISTRATION_QUEUE)
    Queue pluginQueue() {
        return new Queue(RapDefinitions.PLUGIN_REGISTRATION_QUEUE, false);
    }
    
    @Bean(name=RapDefinitions.PLUGIN_REGISTRATION_QUEUE + "Container")
    SimpleMessageListenerContainer platformPluginContainer(ConnectionFactory connectionFactory,
                                                           @Qualifier(RapDefinitions.PLUGIN_REGISTRATION_QUEUE + "Listener") MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(RapDefinitions.PLUGIN_REGISTRATION_QUEUE);
        container.setMessageListener(listenerAdapter);
        return container;
    }
    
    @Bean(name=RapDefinitions.PLUGIN_REGISTRATION_QUEUE + "Bindings")
    List<Binding> pluginBindings(@Qualifier(RapDefinitions.PLUGIN_REGISTRATION_QUEUE) Queue queue,
                                 @Qualifier(RapDefinitions.PLUGIN_REGISTRATION_EXCHANGE_IN) TopicExchange exchange) {
        ArrayList bindings = new ArrayList();
        bindings.add(BindingBuilder.bind(queue).to(exchange).with(RapDefinitions.PLUGIN_REGISTRATION_KEY));

        return bindings;
    }

    @Bean
    PluginRegistration platformPluginReceiver() {
        return new PluginRegistration();
    }

    @Bean(name=RapDefinitions.PLUGIN_REGISTRATION_QUEUE + "Listener")
    MessageListenerAdapter platformPluginListenerAdapter(PluginRegistration receiver) {
        return new MessageListenerAdapter(receiver, "receiveMessage");
    }
}
