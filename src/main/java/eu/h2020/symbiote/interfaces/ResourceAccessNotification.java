/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.interfaces;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import eu.h2020.symbiote.messages.resourceAccessNotification.FailedAccessMessageInfo;
import eu.h2020.symbiote.messages.resourceAccessNotification.SuccessfulAccessMessageInfo;
import eu.h2020.symbiote.messages.resourceAccessNotification.SuccessfulPushesMessageInfo;

/**
 *
 * @author Luca Tomaselli, Pavle Skocir
 */

public class ResourceAccessNotification {
	
    
    @JsonProperty("successfulAttempts")
    private List<SuccessfulAccessMessageInfo> successfulAttempts;

	@JsonProperty("successfulPushes")
    private List<SuccessfulPushesMessageInfo> successfulPushes;
    
    @JsonProperty("failedAttempts")
    private List<FailedAccessMessageInfo> failedAttempts;

    public ResourceAccessNotification() {
    	successfulAttempts= new ArrayList<>();
    	successfulPushes= new ArrayList<>();
    	failedAttempts= new ArrayList<>();
    }
            
    public void addSuccessfulAttempts (String symbioTeId, List<Date> timestamp, String accessType){
        SuccessfulAccessMessageInfo succAccMess = new SuccessfulAccessMessageInfo(symbioTeId, timestamp, accessType);
        this.successfulAttempts.add(succAccMess);
    }
    
    public void addSuccessfulAttemptsList (List<String> symbioTeIdList, List<Date> timestamp, String accessType){
        for(String symbioteId: symbioTeIdList){
            SuccessfulAccessMessageInfo succAccMess = new SuccessfulAccessMessageInfo(symbioteId, timestamp, accessType);
            this.successfulAttempts.add(succAccMess);
        }
    }
    
    public void addSuccessfulPushes (String symbioTeId, List<Date> timestamp){
        SuccessfulPushesMessageInfo succPushMess = new SuccessfulPushesMessageInfo(symbioTeId, timestamp);
        this.successfulPushes.add(succPushMess);
    }
    
    public void addFailedAttempts (String symbioTeId, List<Date> timestamp, 
            String code, String message, String appId, String issuer, 
            String validationStatus, String requestParams) {
        FailedAccessMessageInfo failMess= new FailedAccessMessageInfo(symbioTeId, timestamp, 
                code, message, appId, issuer, validationStatus, requestParams);
        this.failedAttempts.add(failMess);
    }

	public void clear() {
		successfulAttempts.clear();
		successfulPushes.clear();
		failedAttempts.clear();
	}
    
}
