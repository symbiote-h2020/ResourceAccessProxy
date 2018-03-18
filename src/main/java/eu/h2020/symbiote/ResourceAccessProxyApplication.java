package eu.h2020.symbiote;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ResourceAccessProxyApplication {
    public static void main(String[] args) {
    		WaitForPort.waitForServices(WaitForPort.findProperty("SPRING_BOOT_WAIT_FOR_SERVICES"));
        SpringApplication.run(ResourceAccessProxyApplication.class, args);
    }   
}
