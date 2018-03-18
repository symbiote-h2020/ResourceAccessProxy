/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.messages.resourceAccessNotification;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Luca Tomaselli
 */
public class SuccessfulAccessMessageInfo extends MessageInfo {
    public enum AccessType {
        NORMAL,SUBSCRIPTION_START,SUBSCRIPTION_END
    }
    
    @JsonProperty("accessType")
    private String accessType;

    public SuccessfulAccessMessageInfo(String symbioTeId, List<Date> timestamp, String accessType) {
        this.symbioTeId = symbioTeId;
        this.timestamp = timestamp;
        this.accessType = accessType;
    }
    
    

    public String getAccessType() {
        return accessType;
    }

    public void setAccessType(String accessType) {
        this.accessType = accessType;
    }
    
    
}
