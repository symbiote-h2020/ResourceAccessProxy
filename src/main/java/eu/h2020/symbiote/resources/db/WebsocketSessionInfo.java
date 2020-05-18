/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.resources.db;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="sessions")
public class WebsocketSessionInfo {

    @Id
    private String session;
    private List<String> resourceIds;

    public WebsocketSessionInfo() {
        this.session = "";
        this.resourceIds = null;
    }

    public WebsocketSessionInfo(String session, List<String> resourceIds) {
        this.session = session;
        this.resourceIds = resourceIds;
    }

    public String getSession() {
      return session;
    }

    public void setSession(String session) {
      this.session = session;
    }

    public List<String> getResourceIds() {
      return resourceIds;
    }

    public void setResourceIds(List<String> resourceIds) {
      this.resourceIds = resourceIds;
    }
}
