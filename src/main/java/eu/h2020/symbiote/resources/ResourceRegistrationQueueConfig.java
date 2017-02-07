/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.resources;

import eu.h2020.symbiote.interfaces.ResourceRegistration;
import eu.h2020.symbiote.resources.RapDefinitions;
import java.util.ArrayList;
import java.util.List;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
@Configuration
public class ResourceRegistrationQueueConfig {
    @Bean(name=RapDefinitions.RESOURCE_REGISTRATION_EXCHANGE_IN)
    TopicExchange resourceRegistrationExchangeIn() {
            return new TopicExchange(RapDefinitions.RESOURCE_REGISTRATION_EXCHANGE_IN, false, false);
    }
    
    @Bean(name=RapDefinitions.RESOURCE_REGISTRATION_QUEUE)
    Queue resourceRegistrationQueue() {
        return new Queue(RapDefinitions.RESOURCE_REGISTRATION_QUEUE, false);
    }

    @Bean(name=RapDefinitions.RESOURCE_REGISTRATION_QUEUE + "Bindings")
    List<Binding> resourceRegistrationBindings(@Qualifier(RapDefinitions.RESOURCE_REGISTRATION_QUEUE) Queue queue,
                             @Qualifier(RapDefinitions.RESOURCE_REGISTRATION_EXCHANGE_IN) TopicExchange exchange) {
        ArrayList bindings = new ArrayList();
        for(String key : RapDefinitions.RESOURCE_REGISTRATION_KEYS)
            bindings.add(BindingBuilder.bind(queue).to(exchange).with(key));

        return bindings;
    }

    @Bean(name=RapDefinitions.RESOURCE_REGISTRATION_QUEUE + "Container")
    SimpleMessageListenerContainer resourceContainer(ConnectionFactory connectionFactory,
                                             @Qualifier(RapDefinitions.RESOURCE_REGISTRATION_QUEUE + "Listener") MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(RapDefinitions.RESOURCE_REGISTRATION_QUEUE);
        container.setMessageListener(listenerAdapter);
        return container;
    }

    @Bean
    ResourceRegistration resourceReceiver() {
        return new ResourceRegistration();
    }

    @Bean(name=RapDefinitions.RESOURCE_REGISTRATION_QUEUE + "Listener")
    MessageListenerAdapter resourceRegistrationListenerAdapter(ResourceRegistration receiver) {
        return new MessageListenerAdapter(receiver, "receiveMessage");
    }
}
