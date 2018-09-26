/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.resources;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.context.annotation.Bean;
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
