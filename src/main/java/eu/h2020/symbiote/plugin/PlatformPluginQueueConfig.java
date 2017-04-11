/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.plugin;

import eu.h2020.symbiote.resources.RapDefinitions;
import java.util.ArrayList;
import java.util.List;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
@Configuration
public class PlatformPluginQueueConfig {   
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    @Qualifier(RapDefinitions.PLUGIN_REGISTRATION_EXCHANGE_IN)    
    TopicExchange exchange;
    
    
    @Bean(name=PlatformSpecificPlugin.PLUGIN_RES_ACCESS_QUEUE)
    Queue dummyPluginQueue() {
        return new Queue(PlatformSpecificPlugin.PLUGIN_RES_ACCESS_QUEUE, false);
    }

    @Bean(name=PlatformSpecificPlugin.PLUGIN_RES_ACCESS_QUEUE + "Bindings")
    List<Binding> dummyPluginBindings(@Qualifier(PlatformSpecificPlugin.PLUGIN_RES_ACCESS_QUEUE) Queue queue,
                             @Qualifier(RapDefinitions.PLUGIN_EXCHANGE_OUT) TopicExchange exchange) {
        ArrayList bindings = new ArrayList();
        for(String key : PlatformSpecificPlugin.PLUGIN_RES_ACCESS_KEYS)
            bindings.add(BindingBuilder.bind(queue).to(exchange).with(key));

        return bindings;
    }

    @Bean(name=PlatformSpecificPlugin.PLUGIN_RES_ACCESS_QUEUE + "Container")
    SimpleMessageListenerContainer dummyPluginContainer(ConnectionFactory connectionFactory,
                                             @Qualifier(PlatformSpecificPlugin.PLUGIN_RES_ACCESS_QUEUE + "Listener") MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(PlatformSpecificPlugin.PLUGIN_RES_ACCESS_QUEUE);
        container.setMessageListener(listenerAdapter);
        return container;
    }

    @DependsOn(RapDefinitions.PLUGIN_REGISTRATION_EXCHANGE_IN)
    @Bean
    PlatformSpecificPlugin dummyPluginReceiver() {
        return new PlatformSpecificPlugin(rabbitTemplate, exchange);
    }

    @Bean(name=PlatformSpecificPlugin.PLUGIN_RES_ACCESS_QUEUE + "Listener")
    MessageListenerAdapter dummyPluginListenerAdapter(PlatformSpecificPlugin receiver) {
        return new MessageListenerAdapter(receiver, "receiveMessage");
    }
}
