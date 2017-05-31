/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.service.notificationResource;

import java.util.List;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
public class WebSocketMessage {
    public static final String SUBSCRIBE = "SUBSCRIBE";
    public static final String UNSUBSCRIBE = "UNSUBSCRIBE";
    private String action;
    private List<String> ids;
    
    public WebSocketMessage(){
        
    }
    
    public WebSocketMessage(String action,List<String> ids){
        this.action = action;
        this.ids = ids;
    }
    
    public String getAction(){
        return this.action;
    }
    public void setAction(String action){
        this.action = action;
    }
    
    public List<String> getIds(){
        return this.ids;
    }
    public void setIds(List<String> ids){
        this.ids = ids;
    }
}