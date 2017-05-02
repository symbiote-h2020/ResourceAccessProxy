package eu.h2020.symbiote;

import eu.h2020.symbiote.commons.security.SecurityHandler;
import eu.h2020.symbiote.resources.RapDefinitions;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ResourceAccessProxyApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResourceAccessProxyApplication.class, args);
    }   
    
    @Bean
    public SecurityHandler securityHandler() {
        return new SecurityHandler(RapDefinitions.coreAAMUrl, RapDefinitions.rabbitHost, RapDefinitions.securityEnabled);
    }
}
