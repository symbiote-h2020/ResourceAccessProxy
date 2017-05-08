/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote;

import eu.h2020.symbiote.commons.security.SecurityHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
@Configuration
public class SecurityHandlerConfig {
    @Value("${rabbit.host}") 
    private String rabbitHost;
    @Value("${symbiote.coreaam.url}") 
    private String aamUrl;
    @Value("${security.enabled}") 
    private boolean securityEnabled;
    
    public static String coreAAMUrl;
    
    @Bean
    public SecurityHandler securityHandler() {
        coreAAMUrl = aamUrl;
        return new SecurityHandler(aamUrl, rabbitHost, securityEnabled);
    }    
}
