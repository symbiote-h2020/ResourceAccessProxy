/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.messages.accessNotificationMessages;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
public class NotificationMessage {
    @JsonProperty("successfulAttempts")
    private List<SuccessfulAccessMessageInfo> successfulAttempts;
    
    @JsonProperty("successfulPushes")
    private List<SuccessfulPushesMessageInfo> successfulPushes;
    
    @JsonProperty("failedAttempts")
    private List<FailedAccessMessageInfo> failedAttempts;
            
    public void SetSuccessfulAttempts (String symbioTeId, Timestamp timestamp, String accessType){
        SuccessfulAccessMessageInfo succAccMess = new SuccessfulAccessMessageInfo(symbioTeId, timestamp, accessType);
        this.successfulAttempts = new ArrayList<SuccessfulAccessMessageInfo>();
        this.successfulAttempts.add(succAccMess);
    }
    
    public void SetSuccessfulAttemptsList (List<String> symbioTeIdList, Timestamp timestamp, String accessType){
        this.successfulAttempts = new ArrayList<SuccessfulAccessMessageInfo>();
        for(String symbioteId: symbioTeIdList){
            SuccessfulAccessMessageInfo succAccMess = new SuccessfulAccessMessageInfo(symbioteId, timestamp, accessType);
            this.successfulAttempts.add(succAccMess);
        }
    }
    
    public void SetSuccessfulPushes (String symbioTeId, Timestamp timestamp){
        SuccessfulPushesMessageInfo succPushMess = new SuccessfulPushesMessageInfo(symbioTeId, timestamp);
        this.successfulPushes = new ArrayList<SuccessfulPushesMessageInfo>();
        this.successfulPushes.add(succPushMess);
    }
    
    public void SetFailedAttempts (String symbioTeId, Timestamp timestamp, 
            String code, String message, String appId, String issuer, 
            String validationStatus, String requestParams) {
        FailedAccessMessageInfo failMess= new FailedAccessMessageInfo(symbioTeId, timestamp, 
                code, message, appId, issuer, validationStatus, requestParams);
        this.failedAttempts = new ArrayList<FailedAccessMessageInfo>();
        this.failedAttempts.add(failMess);
    }
    
    
    public static void SendSuccessfulAttemptsMessage(String message){
        
    }
    
    public static void SendFailAccessMessage(String message){
        
    }
    
    public static void SendSuccessfulPushMessage(String message){
        
    }
}
