/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.messages.accessNotificationMessages;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.sql.Timestamp;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
public class MessageInfo {
    @JsonProperty("symbioTeId")
    protected String symbioTeId;
    @JsonProperty("timestamp")
    protected Timestamp timestamp;

    public String getSymbioTeId() {
        return symbioTeId;
    }

    public void setSymbioTeId(String symbioTeId) {
        this.symbioTeId = symbioTeId;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
