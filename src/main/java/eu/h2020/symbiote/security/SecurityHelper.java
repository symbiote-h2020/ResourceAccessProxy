/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.h2020.symbiote.security.commons.Token;
import eu.h2020.symbiote.security.commons.credentials.AuthorizationCredentials;
import eu.h2020.symbiote.security.commons.credentials.HomeCredentials;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import eu.h2020.symbiote.security.commons.exceptions.custom.ValidationException;
import eu.h2020.symbiote.security.communication.payloads.AAM;
import eu.h2020.symbiote.security.communication.payloads.SecurityRequest;
import eu.h2020.symbiote.security.handler.ISecurityHandler;
import eu.h2020.symbiote.security.helpers.MutualAuthenticationHelper;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
public class SecurityHelper {
    
    //@Value("${symbIoTe.coreaam.url}")
    private String coreAAMUrl;
    
    //@Value("${platform.id}") 
    private String platformId;
    
    private String keystorePath;
    private String keystorePassword;
        //Put here whatever. It is todo for R4. Does not make any difference now
    private String userId;
    private String username;
    private String password;
    private String clientId;
    
    private static final Logger log = LoggerFactory.getLogger(SecurityHelper.class);
    
        
    
    public SecurityHelper(){
    }
    
    
    @Value("${symbIoTe.coreaam.url}")
    public void setCoreAAMUrl(String coreaamUrl) {
        this.coreAAMUrl = coreaamUrl;
    }
    @Value("${platform.id}") 
    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }
    @Value("${symbIoTe.component.keystore.path}")
    public void setKeystorePath(String keystorePath) {
        this.keystorePath = keystorePath;
    }
    @Value("${symbIoTe.component.keystore.password}")
    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }
    @Value("${userId}") 
    public void setUserId(String userId) {
        this.userId = userId;
    }
    @Value("${demoApp.username}") 
    public void setUsername(String username) {
        this.username = username;
    }
    @Value("${demoApp.password}") 
    public void setPassword(String password) {
        this.password = password;
    }
    @Value("${clientId}") 
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    
    public HttpHeaders getHeader() throws Exception{
        Map<String, String> securityRequestHeaders = null;
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        try {
            ISecurityHandler securityHandler = ClientSecurityHandlerFactory.getSecurityHandler(coreAAMUrl, keystorePath,
                keystorePassword, userId);
            
            
            Set<AuthorizationCredentials> authorizationCredentialsSet = new HashSet<>();
            Map<String, AAM> availableAAMs = securityHandler.getAvailableAAMs();

            AAM platformAAM = availableAAMs.get(platformId);
            if(platformAAM == null)
                throw new Exception("Not found availableAAMs for platform "+platformId);
            log.info("Getting certificate for " + platformAAM.getAamInstanceId());
            securityHandler.getCertificate(platformAAM, username, password, clientId);

            log.info("Getting token from " + platformAAM.getAamInstanceId());
            Token homeToken = securityHandler.login(platformAAM);

            HomeCredentials homeCredentials = securityHandler.getAcquiredCredentials().get(platformId).homeCredentials;
            authorizationCredentialsSet.add(new AuthorizationCredentials(homeToken, homeCredentials.homeAAM, homeCredentials));

            SecurityRequest securityRequest = MutualAuthenticationHelper.getSecurityRequest(authorizationCredentialsSet, false);
            securityRequestHeaders = securityRequest.getSecurityRequestHeaderParams();
            
            for (Map.Entry<String, String> entry : securityRequestHeaders.entrySet()) {
                httpHeaders.add(entry.getKey(), entry.getValue());
            }
            log.info("request headers: " + httpHeaders);

        } catch (SecurityHandlerException | ValidationException | JsonProcessingException | NoSuchAlgorithmException e) {
            log.error("Fail to take header",e);
        }
        return httpHeaders;
    }
}
