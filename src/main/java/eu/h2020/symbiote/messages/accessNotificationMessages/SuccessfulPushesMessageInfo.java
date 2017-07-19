/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.messages.accessNotificationMessages;

import java.sql.Timestamp;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
public class SuccessfulPushesMessageInfo extends MessageInfo{

    public SuccessfulPushesMessageInfo(String symbioTeId, Timestamp timestamp) {
        this.symbioTeId = symbioTeId;
        this.timestamp = timestamp;
    }
    
}
