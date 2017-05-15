/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote;

import eu.h2020.symbiote.security.SecurityHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import eu.h2020.symbiote.security.session.AAM;
import java.util.List;
import java.util.Iterator;
import eu.h2020.symbiote.security.exceptions.SecurityHandlerException;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
@Configuration
public class SecurityHandlerConfig {
    @Value("${rabbit.host}")
    String rabbitMQHostIP;

    @Value("${rabbit.username}")
    String rabbitMQUsername;  

    @Value("${rabbit.password}")
    String rabbitMQPassword;

    @Value("${symbiote.coreaam.url}") 
    private String coreAAMUrl;
  
    @Value("${platform.id}") 
    private String platformId;

    @Bean
    public SecurityHandler securityHandler() {
        return new SecurityHandler(coreAAMUrl, rabbitMQHostIP, rabbitMQUsername, rabbitMQPassword);
    }   

    @Bean
    public AAM platformAAM(SecurityHandler securityHandler) throws SecurityHandlerException{
        List<AAM> listOfAAMs = securityHandler.getAvailableAAMs();

        for(Iterator iter = listOfAAMs.iterator(); iter.hasNext();) {
            AAM aam = (AAM) iter.next();
            if (aam.getAamInstanceId() == platformId)
                return aam;
        }
        return new AAM();
    } 
}
