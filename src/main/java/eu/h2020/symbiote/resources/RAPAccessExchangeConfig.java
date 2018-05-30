/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.resources;

import eu.h2020.symbiote.interfaces.PluginNotification;
import eu.h2020.symbiote.interfaces.conditions.NBInterfaceWebSocketCondition;
import java.util.ArrayList;
import java.util.List;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author Pavle Skocir
 */
@Configuration
public class RAPAccessExchangeConfig {
    
    @Bean(name=RapDefinitions.RAP_ACCESS_EXCHANGE)
    DirectExchange rapAccessExchange() {
        return new DirectExchange(RapDefinitions.RAP_ACCESS_EXCHANGE, true, false);
    }
}
