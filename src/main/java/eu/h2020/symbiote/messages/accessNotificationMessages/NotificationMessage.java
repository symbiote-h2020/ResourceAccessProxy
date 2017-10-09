/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.messages.accessNotificationMessages;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.h2020.symbiote.security.SecurityHelper;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
public class NotificationMessage {
    private String notificationUrl;
    
    private SecurityHelper securityHelper;
    
    private static final Logger log = LoggerFactory.getLogger(NotificationMessage.class);
    
    @JsonProperty("successfulAttempts")
    private List<SuccessfulAccessMessageInfo> successfulAttempts;
    
    @JsonProperty("successfulPushes")
    private List<SuccessfulPushesMessageInfo> successfulPushes;
    
    @JsonProperty("failedAttempts")
    private List<FailedAccessMessageInfo> failedAttempts;
    
    public NotificationMessage(SecurityHelper securityHelper, String notificationUrl) {
        this.securityHelper = securityHelper;
        this.notificationUrl = notificationUrl;
    }
            
    public void SetSuccessfulAttempts (String symbioTeId, List<Date> timestamp, String accessType){
        SuccessfulAccessMessageInfo succAccMess = new SuccessfulAccessMessageInfo(symbioTeId, timestamp, accessType);
        this.successfulAttempts = new ArrayList<>();
        this.successfulAttempts.add(succAccMess);
    }
    
    public void SetSuccessfulAttemptsList (List<String> symbioTeIdList, List<Date> timestamp, String accessType){
        this.successfulAttempts = new ArrayList<>();
        for(String symbioteId: symbioTeIdList){
            SuccessfulAccessMessageInfo succAccMess = new SuccessfulAccessMessageInfo(symbioteId, timestamp, accessType);
            this.successfulAttempts.add(succAccMess);
        }
    }
    
    public void SetSuccessfulPushes (String symbioTeId, List<Date> timestamp){
        SuccessfulPushesMessageInfo succPushMess = new SuccessfulPushesMessageInfo(symbioTeId, timestamp);
        this.successfulPushes = new ArrayList<>();
        this.successfulPushes.add(succPushMess);
    }
    
    public void SetFailedAttempts (String symbioTeId, List<Date> timestamp, 
            String code, String message, String appId, String issuer, 
            String validationStatus, String requestParams) {
        FailedAccessMessageInfo failMess= new FailedAccessMessageInfo(symbioTeId, timestamp, 
                code, message, appId, issuer, validationStatus, requestParams);
        this.failedAttempts = new ArrayList<>();
        this.failedAttempts.add(failMess);
    }
    
    
    public void SendSuccessfulAttemptsMessage(String message){
        sendMessage(message);
    }
    
    public void SendFailAccessMessage(String message){
        sendMessage(message);
    }
    
    public void SendSuccessfulPushMessage(String message){
        sendMessage(message);
    }
    
    private void sendMessage(String message){
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
        
        HttpHeaders httpHeaders = securityHelper.getHeader();
        HttpEntity<String> httpEntity = new HttpEntity<String>(message,httpHeaders);
        
        Object response = restTemplate.postForObject(notificationUrl, httpEntity, Object.class);
        log.info("Response notification message: "+ (String)response);
    }
}
