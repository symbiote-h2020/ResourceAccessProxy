package eu.h2020.symbiote;

import eu.h2020.symbiote.service.notificationResurce.NotificationWebSocket;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ResourceAccessProxyApplication {
        
    public static void main(String[] args) {
        SpringApplication.run(ResourceAccessProxyApplication.class, args);
    }   
    
    @Bean
    public NotificationWebSocket createWebsocket() {
        return new NotificationWebSocket();
    }
}
