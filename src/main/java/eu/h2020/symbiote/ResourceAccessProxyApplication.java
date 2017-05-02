package eu.h2020.symbiote;

import eu.h2020.symbiote.commons.security.SecurityHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ResourceAccessProxyApplication {
        
    @Value("${rabbit.host}") 
    private String rabbitHost;

    @Value("${symbiote.coreaam.url}") 
    private String coreAAMUrl;

    @Value("${security.enabled}") 
    private boolean securityEnabled;
    
    public static void main(String[] args) {
        SpringApplication.run(ResourceAccessProxyApplication.class, args);
    }   
    
    @Bean
    public SecurityHandler securityHandler() {
        return new SecurityHandler(coreAAMUrl, rabbitHost, securityEnabled);
    }
}
