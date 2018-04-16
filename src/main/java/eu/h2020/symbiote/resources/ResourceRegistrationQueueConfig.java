/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.resources;

import eu.h2020.symbiote.interfaces.ResourceRegistration;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author Matteo Pardi
 */
@Configuration
public class ResourceRegistrationQueueConfig {
    @Bean(name=RapDefinitions.RESOURCE_REGISTRATION_EXCHANGE_IN)
    DirectExchange resourceRegistrationExchangeIn() {
        return new DirectExchange(RapDefinitions.RESOURCE_REGISTRATION_EXCHANGE_IN, true, false);
    }
    
    @Bean(name=RapDefinitions.RESOURCE_REGISTRATION_QUEUE)
    Queue resourceRegistrationQueue() {
        return new Queue(RapDefinitions.RESOURCE_REGISTRATION_QUEUE, false);
    }
    
    @Bean(name=RapDefinitions.RESOURCE_UNREGISTRATION_QUEUE)
    Queue resourceUnregistrationQueue() {
        return new Queue(RapDefinitions.RESOURCE_UNREGISTRATION_QUEUE, false);
    }
    
    @Bean(name=RapDefinitions.RESOURCE_UPDATE_QUEUE)
    Queue resourceUpdatedQueue() {
        return new Queue(RapDefinitions.RESOURCE_UPDATE_QUEUE, false);
    }
    
    //4 QUEUES FOR L2 RESOURCES
    @Bean(name=RapDefinitions.RESOURCE_L2_UPDATE_QUEUE)
    Queue resourceL2UpdateQueue() {
        return new Queue(RapDefinitions.RESOURCE_L2_UPDATE_QUEUE, false);
    }
    
    @Bean(name=RapDefinitions.RESOURCE_L2_UNREGISTRATION_QUEUE)
    Queue resourceL2UnregistrationQueue() {
        return new Queue(RapDefinitions.RESOURCE_L2_UNREGISTRATION_QUEUE, false);
    }
    
    @Bean(name=RapDefinitions.RESOURCE_L2_SHARE_QUEUE)
    Queue resourceL2ShareQueue() {
        return new Queue(RapDefinitions.RESOURCE_L2_SHARE_QUEUE, false);
    }
    
    @Bean(name=RapDefinitions.RESOURCE_L2_UNSHARE_QUEUE)
    Queue resourceL2UnShareQueue() {
        return new Queue(RapDefinitions.RESOURCE_L2_UNSHARE_QUEUE, false);
    }
    //end L2

    @Bean(name=RapDefinitions.RESOURCE_REGISTRATION_QUEUE + "Bindings")
    Binding resourceRegistrationBindings(@Qualifier(RapDefinitions.RESOURCE_REGISTRATION_QUEUE) Queue queue,
                             @Qualifier(RapDefinitions.RESOURCE_REGISTRATION_EXCHANGE_IN) DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(RapDefinitions.RESOURCE_REGISTRATION_KEY);
    }
    
    
    @Bean(name=RapDefinitions.RESOURCE_UNREGISTRATION_QUEUE + "Bindings")
    Binding resourceUnregistrationBindings(@Qualifier(RapDefinitions.RESOURCE_UNREGISTRATION_QUEUE) Queue queue,
                             @Qualifier(RapDefinitions.RESOURCE_REGISTRATION_EXCHANGE_IN) DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(RapDefinitions.RESOURCE_UNREGISTRATION_KEY);
    }
    
    
    @Bean(name=RapDefinitions.RESOURCE_UPDATE_QUEUE + "Bindings")
    Binding resourceUpdatedBindings(@Qualifier(RapDefinitions.RESOURCE_UPDATE_QUEUE) Queue queue,
                             @Qualifier(RapDefinitions.RESOURCE_REGISTRATION_EXCHANGE_IN) DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(RapDefinitions.RESOURCE_UPDATE_KEY);
    }
    
    //4 BINDINGS FOR L2 RESOURCES
    @Bean(name=RapDefinitions.RESOURCE_L2_UPDATE_QUEUE + "Bindings")
    Binding resourceL2UpdatedBindings(@Qualifier(RapDefinitions.RESOURCE_L2_UPDATE_QUEUE) Queue queue,
                             @Qualifier(RapDefinitions.RESOURCE_REGISTRATION_EXCHANGE_IN) DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(RapDefinitions.ROUTING_KEY_RH_UPDATED);
    }
    
    
    @Bean(name=RapDefinitions.RESOURCE_L2_UNREGISTRATION_QUEUE + "Bindings")
    Binding resourceL2UnregistrationBindings(@Qualifier(RapDefinitions.RESOURCE_L2_UNREGISTRATION_QUEUE) Queue queue,
                             @Qualifier(RapDefinitions.RESOURCE_REGISTRATION_EXCHANGE_IN) DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(RapDefinitions.ROUTING_KEY_RH_DELETED);
    }
    
    
    @Bean(name=RapDefinitions.RESOURCE_L2_SHARE_QUEUE + "Bindings")
    Binding resourceL2SharedBindings(@Qualifier(RapDefinitions.RESOURCE_L2_SHARE_QUEUE) Queue queue,
                             @Qualifier(RapDefinitions.RESOURCE_REGISTRATION_EXCHANGE_IN) DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(RapDefinitions.ROUTING_KEY_RH_SHARED);
    }
        
    @Bean(name=RapDefinitions.RESOURCE_L2_UNSHARE_QUEUE + "Bindings")
    Binding resourceL2UnsharedBindings(@Qualifier(RapDefinitions.RESOURCE_L2_UNSHARE_QUEUE) Queue queue,
                             @Qualifier(RapDefinitions.RESOURCE_REGISTRATION_EXCHANGE_IN) DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(RapDefinitions.ROUTING_KEY_RH_UNSHARED);
    }
    //end L2

    @Bean(name=RapDefinitions.RESOURCE_REGISTRATION_QUEUE + "Container")
    SimpleMessageListenerContainer resourceRegContainer(ConnectionFactory connectionFactory,
                                             @Qualifier(RapDefinitions.RESOURCE_REGISTRATION_QUEUE + "Listener") MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(RapDefinitions.RESOURCE_REGISTRATION_QUEUE);
        container.setMessageListener(listenerAdapter);
        return container;
    }
    
    @Bean(name=RapDefinitions.RESOURCE_UNREGISTRATION_QUEUE + "Container")
    SimpleMessageListenerContainer resourceUnregContainer(ConnectionFactory connectionFactory,
                                             @Qualifier(RapDefinitions.RESOURCE_UNREGISTRATION_QUEUE + "Listener") MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(RapDefinitions.RESOURCE_UNREGISTRATION_QUEUE);
        container.setMessageListener(listenerAdapter);
        return container;
    }

    @Bean(name=RapDefinitions.RESOURCE_UPDATE_QUEUE + "Container")
    SimpleMessageListenerContainer resourceUpdContainer(ConnectionFactory connectionFactory,
                                             @Qualifier(RapDefinitions.RESOURCE_UPDATE_QUEUE + "Listener") MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(RapDefinitions.RESOURCE_UPDATE_QUEUE);
        container.setMessageListener(listenerAdapter);
        return container;
    }
    
    //4 CONTAINERS FOR L2 RESOURCES
    @Bean(name=RapDefinitions.RESOURCE_L2_UPDATE_QUEUE + "Container")
    SimpleMessageListenerContainer resourceL2UpdContainer(ConnectionFactory connectionFactory,
                                             @Qualifier(RapDefinitions.RESOURCE_L2_UPDATE_QUEUE + "Listener") MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(RapDefinitions.RESOURCE_L2_UPDATE_QUEUE);
        container.setMessageListener(listenerAdapter);
        return container;
    }
    
    @Bean(name=RapDefinitions.RESOURCE_L2_UNREGISTRATION_QUEUE + "Container")
    SimpleMessageListenerContainer resourceL2UnregContainer(ConnectionFactory connectionFactory,
                                             @Qualifier(RapDefinitions.RESOURCE_L2_UNREGISTRATION_QUEUE + "Listener") MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(RapDefinitions.RESOURCE_L2_UNREGISTRATION_QUEUE);
        container.setMessageListener(listenerAdapter);
        return container;
    }

    @Bean(name=RapDefinitions.RESOURCE_L2_SHARE_QUEUE + "Container")
    SimpleMessageListenerContainer resourceL2ShareContainer(ConnectionFactory connectionFactory,
                                             @Qualifier(RapDefinitions.RESOURCE_L2_SHARE_QUEUE + "Listener") MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(RapDefinitions.RESOURCE_L2_SHARE_QUEUE);
        container.setMessageListener(listenerAdapter);
        return container;
    }
    
    @Bean(name=RapDefinitions.RESOURCE_L2_UNSHARE_QUEUE + "Container")
    SimpleMessageListenerContainer resourceL2UnShareContainer(ConnectionFactory connectionFactory,
                                             @Qualifier(RapDefinitions.RESOURCE_L2_UNSHARE_QUEUE + "Listener") MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(RapDefinitions.RESOURCE_L2_UNSHARE_QUEUE);
        container.setMessageListener(listenerAdapter);
        return container;
    }
    //end L2
    
    @Bean
    ResourceRegistration resourceReceiver() {
        return new ResourceRegistration();
    }

    @Bean(name=RapDefinitions.RESOURCE_REGISTRATION_QUEUE + "Listener")
    MessageListenerAdapter resourceRegistrationListenerAdapter(ResourceRegistration receiver) {
        return new MessageListenerAdapter(receiver, "receiveRegistrationMessage");
    }
    
    @Bean(name=RapDefinitions.RESOURCE_UNREGISTRATION_QUEUE + "Listener")
    MessageListenerAdapter resourceUnRegistrationListenerAdapter(ResourceRegistration receiver) {
        return new MessageListenerAdapter(receiver, "receiveUnregistrationMessage");
    }
    
    @Bean(name=RapDefinitions.RESOURCE_UPDATE_QUEUE + "Listener")
    MessageListenerAdapter resourceUpdatedListenerAdapter(ResourceRegistration receiver) {
        return new MessageListenerAdapter(receiver, "receiveUpdateMessage");
    }
    
    //4 LISTENERS TO THE SAME METHODS AS MENTIONED ABOVE
    @Bean(name=RapDefinitions.RESOURCE_L2_UPDATE_QUEUE + "Listener")
    MessageListenerAdapter resourceL2UpdateListenerAdapter(ResourceRegistration receiver) {
        return new MessageListenerAdapter(receiver, "receiveUpdateMessage");
    }
    
    @Bean(name=RapDefinitions.RESOURCE_L2_UNREGISTRATION_QUEUE + "Listener")
    MessageListenerAdapter resourceL2UnRegistrationListenerAdapter(ResourceRegistration receiver) {
        return new MessageListenerAdapter(receiver, "receiveUnregistrationMessage");
    }
    
    @Bean(name=RapDefinitions.RESOURCE_L2_SHARE_QUEUE + "Listener")
    MessageListenerAdapter resourceL2ShareListenerAdapter(ResourceRegistration receiver) {
        return new MessageListenerAdapter(receiver, "receiveUpdateMessage");
    }
    
    @Bean(name=RapDefinitions.RESOURCE_L2_UNSHARE_QUEUE + "Listener")
    MessageListenerAdapter resourceL2UnShareListenerAdapter(ResourceRegistration receiver) {
        return new MessageListenerAdapter(receiver, "receiveUpdateMessage");
    }
    //end L2
}
