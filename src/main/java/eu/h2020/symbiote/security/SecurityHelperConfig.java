/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
@Configuration
public class SecurityHelperConfig {
    
    @Bean
    public SecurityHelper generateSecurityHelper(){
        return new SecurityHelper();
    }
}
