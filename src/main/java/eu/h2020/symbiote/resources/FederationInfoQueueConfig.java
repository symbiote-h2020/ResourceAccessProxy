package eu.h2020.symbiote.resources;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import eu.h2020.symbiote.interfaces.FederationInfoRegistration;

/**
 * 
 * @author Pavle Skocir
 *
 */
@Configuration
public class FederationInfoQueueConfig {
    
    @Value("${rabbit.replyTimeout}")
    private int rabbitReplyTimeout;

	//exchange
	@Bean(name=RapDefinitions.FEDERATION_EXCHANGE)
    TopicExchange federationExchange() {
        return new TopicExchange(RapDefinitions.FEDERATION_EXCHANGE, false, false);   		
    }
	
	//queues
	@Bean(name=RapDefinitions.FEDERATION_QUEUE_CREATED)
    Queue federationCreatedQueue() {
	    Map<String, Object> arguments = new HashMap<>();
	    arguments.put("x-message-ttl", rabbitReplyTimeout);
        return new Queue(RapDefinitions.FEDERATION_QUEUE_CREATED, false, false, true, arguments);
    }
	
	@Bean(name=RapDefinitions.FEDERATION_QUEUE_CHANGED)
    Queue federationChangedQueue() {
       Map<String, Object> arguments = new HashMap<>();
       arguments.put("x-message-ttl", rabbitReplyTimeout);
       return new Queue(RapDefinitions.FEDERATION_QUEUE_CHANGED, false, false, true, arguments);
    }
	
	@Bean(name=RapDefinitions.FEDERATION_QUEUE_DELETED)
    Queue federationDeletedQueue() {
       Map<String, Object> arguments = new HashMap<>();
       arguments.put("x-message-ttl", rabbitReplyTimeout);
       return new Queue(RapDefinitions.FEDERATION_QUEUE_DELETED, false, false, true, arguments);
    }
	
	//bindings
	@Bean(name=RapDefinitions.FEDERATION_QUEUE_CREATED + "Bindings")
    Binding federationCreatedBindings(@Qualifier(RapDefinitions.FEDERATION_QUEUE_CREATED) Queue queue,
                             @Qualifier(RapDefinitions.FEDERATION_EXCHANGE) TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(RapDefinitions.FEDERATION_KEY_CREATED);
    }
	
	@Bean(name=RapDefinitions.FEDERATION_QUEUE_CHANGED + "Bindings")
    Binding federationChangedBindings(@Qualifier(RapDefinitions.FEDERATION_QUEUE_CHANGED) Queue queue,
                             @Qualifier(RapDefinitions.FEDERATION_EXCHANGE) TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(RapDefinitions.FEDERATION_KEY_CHANGED);
    }
	
	@Bean(name=RapDefinitions.FEDERATION_QUEUE_DELETED + "Bindings")
    Binding federationDeletedBindings(@Qualifier(RapDefinitions.FEDERATION_QUEUE_DELETED) Queue queue,
                             @Qualifier(RapDefinitions.FEDERATION_EXCHANGE) TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(RapDefinitions.FEDERATION_KEY_DELETED);
    }
		
	//containers
	@Bean(name=RapDefinitions.FEDERATION_QUEUE_CREATED + "Container")
    SimpleMessageListenerContainer federationCreatedContainer(ConnectionFactory connectionFactory,
                                             @Qualifier(RapDefinitions.FEDERATION_QUEUE_CREATED + "Listener") MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(RapDefinitions.FEDERATION_QUEUE_CREATED);
        container.setMessageListener(listenerAdapter);
        container.setDefaultRequeueRejected(false);
        return container;
    }
	
	@Bean(name=RapDefinitions.FEDERATION_QUEUE_CHANGED + "Container")
    SimpleMessageListenerContainer federationChangedContainer(ConnectionFactory connectionFactory,
                                             @Qualifier(RapDefinitions.FEDERATION_QUEUE_CHANGED + "Listener") MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(RapDefinitions.FEDERATION_QUEUE_CHANGED);
        container.setMessageListener(listenerAdapter);
        container.setDefaultRequeueRejected(false);
        return container;
    }
	
	@Bean(name=RapDefinitions.FEDERATION_QUEUE_DELETED + "Container")
    SimpleMessageListenerContainer federationDeletedContainer(ConnectionFactory connectionFactory,
                                             @Qualifier(RapDefinitions.FEDERATION_QUEUE_DELETED + "Listener") MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(RapDefinitions.FEDERATION_QUEUE_DELETED);
        container.setMessageListener(listenerAdapter);
        container.setDefaultRequeueRejected(false);
        return container;
    }
	
	//listeners
	@Bean
    FederationInfoRegistration federationInfoReceiver() {
        return new FederationInfoRegistration();
    }

    @Bean(name=RapDefinitions.FEDERATION_QUEUE_CREATED + "Listener")
    MessageListenerAdapter federationCreatedListenerAdapter(FederationInfoRegistration receiver) {
        return new MessageListenerAdapter(receiver, "receiveFederationCreatedMessage");
    }
    
    @Bean(name=RapDefinitions.FEDERATION_QUEUE_CHANGED + "Listener")
    MessageListenerAdapter federationChangedListenerAdapter(FederationInfoRegistration receiver) {
        return new MessageListenerAdapter(receiver, "receiveFederationChangedMessage");
    }
    
    @Bean(name=RapDefinitions.FEDERATION_QUEUE_DELETED + "Listener")
    MessageListenerAdapter federationDeletedListenerAdapter(FederationInfoRegistration receiver) {
        return new MessageListenerAdapter(receiver, "receiveFederationDeletedMessage");
    }
}
