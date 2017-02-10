/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.resources;

import eu.h2020.symbiote.interfaces.ResourceRead;
import eu.h2020.symbiote.interfaces.ResourceWrite;
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
 * 
 * 
 */
/******************************************************************************
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * ATTENTION: This class is NOT USED anymore!!!!
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 */
@Configuration
public class ResourceAccessQueueConfig {
    @Bean(name=RapDefinitions.RESOURCE_ACCESS_EXCHANGE_IN)
    TopicExchange resourceAccessExchangeIn() {
            return new TopicExchange(RapDefinitions.RESOURCE_ACCESS_EXCHANGE_IN, false, false);
    }
    
    @Bean(name=RapDefinitions.RESOURCE_READ_QUEUE)
    Queue resourceReadQueue() {
        return new Queue(RapDefinitions.RESOURCE_READ_QUEUE, false);
    }

    @Bean(name=RapDefinitions.RESOURCE_READ_QUEUE + "Bindings")
    List<Binding> resourceReadBindings(@Qualifier(RapDefinitions.RESOURCE_READ_QUEUE) Queue queue,
                             @Qualifier(RapDefinitions.RESOURCE_ACCESS_EXCHANGE_IN) TopicExchange exchange) {
        ArrayList bindings = new ArrayList();
        for(String key : RapDefinitions.RESOURCE_READ_KEYS)
            bindings.add(BindingBuilder.bind(queue).to(exchange).with(key));

        return bindings;
    }

    @Bean(name=RapDefinitions.RESOURCE_READ_QUEUE + "Container")
    SimpleMessageListenerContainer resourceReadContainer(ConnectionFactory connectionFactory,
                                             @Qualifier(RapDefinitions.RESOURCE_READ_QUEUE + "Listener") MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(RapDefinitions.RESOURCE_READ_QUEUE);
        container.setMessageListener(listenerAdapter);
        return container;
    }

    @Bean
    ResourceRead resourceReadReceiver() {
        return new ResourceRead();
    }

    @Bean(name=RapDefinitions.RESOURCE_READ_QUEUE + "Listener")
    MessageListenerAdapter resourceReadListenerAdapter(ResourceRead receiver) {
        return new MessageListenerAdapter(receiver, "receiveMessage");
    }
    
    @Bean(name=RapDefinitions.RESOURCE_WRITE_QUEUE)
    Queue resourceWriteQueue() {
        return new Queue(RapDefinitions.RESOURCE_WRITE_QUEUE, false);
    }   

    @Bean(name=RapDefinitions.RESOURCE_WRITE_QUEUE + "Bindings")
    List<Binding> resourceWriteBindings(@Qualifier(RapDefinitions.RESOURCE_WRITE_QUEUE) Queue queue,
                             @Qualifier(RapDefinitions.RESOURCE_ACCESS_EXCHANGE_IN) TopicExchange exchange) {
        ArrayList bindings = new ArrayList();
        for(String key : RapDefinitions.RESOURCE_WRITE_KEYS)
            bindings.add(BindingBuilder.bind(queue).to(exchange).with(key));

        return bindings;
    }

    @Bean(name=RapDefinitions.RESOURCE_WRITE_QUEUE + "Container")
    SimpleMessageListenerContainer resourceWriteContainer(ConnectionFactory connectionFactory,
                                             @Qualifier(RapDefinitions.RESOURCE_WRITE_QUEUE + "Listener") MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(RapDefinitions.RESOURCE_WRITE_QUEUE);
        container.setMessageListener(listenerAdapter);
        return container;
    }

    @Bean
    ResourceWrite resourceWriteReceiver() {
        return new ResourceWrite();
    }

    @Bean(name=RapDefinitions.RESOURCE_WRITE_QUEUE + "Listener")
    MessageListenerAdapter resourceWriteListenerAdapter(ResourceWrite receiver) {
        return new MessageListenerAdapter(receiver, "receiveMessage");
    }
}
