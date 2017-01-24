/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote;

import eu.h2020.symbiote.interfaces.PluginRegistration;
import eu.h2020.symbiote.interfaces.ResourceRegistration;
import eu.h2020.symbiote.resources.RapDefinitions;
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
public class QueueConfig {

	@Bean(name=RapDefinitions.NORTHBOUND_QUEUE)
	Queue northboundQueue() {
		return new Queue(RapDefinitions.NORTHBOUND_QUEUE, true);
	}
        
        @Bean(name=RapDefinitions.PLATFORM_QUEUE)
	Queue platformQueue() {
		return new Queue(RapDefinitions.PLATFORM_QUEUE, true);
	}
	
	@Bean(name=RapDefinitions.RAP_EXCHANGE_IN)
	TopicExchange exchangeIn() {
		return new TopicExchange(RapDefinitions.RAP_EXCHANGE_IN, true, false);
	}
        
        @Bean(name=RapDefinitions.PLUGINS_EXCHANGE_OUT)
	TopicExchange exchangeOut() {
		return new TopicExchange(RapDefinitions.PLUGINS_EXCHANGE_OUT, true, false);
	}
	
	@Bean(name="northboundBindings")
	List<Binding> northboundBindings(@Qualifier(RapDefinitions.NORTHBOUND_QUEUE) Queue queue,
                                 @Qualifier(RapDefinitions.RAP_EXCHANGE_IN) TopicExchange exchange) {
            ArrayList bindings = new ArrayList();
            for(String key : RapDefinitions.NORTHBOUND_KEYS)
                bindings.add(BindingBuilder.bind(queue).to(exchange).with(key));
            
            return bindings;
	}
        
        @Bean(name="platformBindings")
        List<Binding> platformBindings(@Qualifier(RapDefinitions.PLATFORM_QUEUE) Queue queue,
                                 @Qualifier(RapDefinitions.RAP_EXCHANGE_IN) TopicExchange exchange) {
            ArrayList bindings = new ArrayList();
            for(String key : RapDefinitions.PLATFORM_KEYS)
                bindings.add(BindingBuilder.bind(queue).to(exchange).with(key));
            
            return bindings;
	}
	
	@Bean(name="resourceRegistrationContainer")
	SimpleMessageListenerContainer resourceContainer(ConnectionFactory connectionFactory,
                                                 @Qualifier("resourceRegistrationListener")MessageListenerAdapter listenerAdapter) {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.setQueueNames(RapDefinitions.NORTHBOUND_QUEUE);
		container.setMessageListener(listenerAdapter);
		return container;
	}
	
	@Bean
	ResourceRegistration resourceReceiver() {
            return new ResourceRegistration();
	}
	
	@Bean(name="resourceRegistrationListener")
	MessageListenerAdapter resourceListenerAdapter(ResourceRegistration receiver) {
		return new MessageListenerAdapter(receiver, "receiveMessage");
	}
        
        @Bean(name="platformPluginRegistrationContainer")
	SimpleMessageListenerContainer platformPluginContainer(ConnectionFactory connectionFactory,
                                                               @Qualifier("platformPluginRegistrationListener")MessageListenerAdapter listenerAdapter) {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.setQueueNames(RapDefinitions.PLATFORM_QUEUE);
		container.setMessageListener(listenerAdapter);
		return container;
	}
	
	@Bean
	PluginRegistration platformPluginreceiver() {
            return new PluginRegistration();
	}
	
	@Bean(name="platformPluginRegistrationListener")
	MessageListenerAdapter platformPluginListenerAdapter(PluginRegistration receiver) {
		return new MessageListenerAdapter(receiver, "receiveMessage");
	}
    
}
