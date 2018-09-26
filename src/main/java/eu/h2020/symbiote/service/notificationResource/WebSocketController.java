/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.service.notificationResource;

import eu.h2020.symbiote.core.cci.accessNotificationMessages.SuccessfulAccessMessageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.resources.db.ResourcesRepository;
import eu.h2020.symbiote.cloud.model.rap.ResourceInfo;
import eu.h2020.symbiote.cloud.model.rap.access.ResourceAccessMessage;
import eu.h2020.symbiote.cloud.model.rap.access.ResourceAccessSubscribeMessage;
import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.exceptions.EntityNotFoundException;
import eu.h2020.symbiote.interfaces.conditions.NBInterfaceWebSocketCondition;
import eu.h2020.symbiote.cloud.model.rap.access.ResourceAccessUnSubscribeMessage;
import eu.h2020.symbiote.interfaces.ResourceAccessNotificationService;
import eu.h2020.symbiote.resources.RapDefinitions;
import eu.h2020.symbiote.resources.db.PlatformInfo;
import eu.h2020.symbiote.resources.db.PluginRepository;
import eu.h2020.symbiote.resources.db.DbResourceInfo;
import eu.h2020.symbiote.security.commons.exceptions.custom.ValidationException;
import eu.h2020.symbiote.security.communication.payloads.SecurityRequest;
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.managers.AuthorizationResult;
import eu.h2020.symbiote.managers.ServiceResponseResult;
import eu.h2020.symbiote.service.notificationResource.WebSocketMessage.Action;
import static eu.h2020.symbiote.security.commons.SecurityConstants.SECURITY_RESPONSE_HEADER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 *
 * @author Luca Tomaselli
 */
@Conditional(NBInterfaceWebSocketCondition.class)
@Component
@CrossOrigin 
public class WebSocketController extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(WebSocketController.class);

    @Autowired
    ResourceAccessNotificationService notificationService;
    
    @Autowired
    ResourcesRepository resourcesRepo;
    
    @Autowired
    PluginRepository pluginRepo;
        
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    @Qualifier(RapDefinitions.PLUGIN_EXCHANGE_OUT)
    TopicExchange exchange;
    
    @Value("${symbiote.rap.cram.url}") 
    private String notificationUrl;
    
    @Autowired
    private AuthorizationManager authManager;

    private final HashMap<String, WebSocketSession> idSession = new HashMap<>();

    @Override
    public void handleTransportError(WebSocketSession session, Throwable throwable) throws Exception {
        log.error("error occured at sender " + session, throwable);
    }

    /**
     * This method handles the connection closed procedure.
     * @param session WebSocket session
     * @param status close status
     * @throws Exception exception in handling closing WebSocket
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("Session " + session.getId() + " closed with status " + status.getCode());
        idSession.remove(session.getId());

        //update DB
        List<DbResourceInfo> resInfoList = resourcesRepo.findAll();
        if (resInfoList != null) {
            for (DbResourceInfo resInfo : resInfoList) {
                List<String> sessionsIdOfRes = resInfo.getSessionId();
                if (sessionsIdOfRes != null) {
                    sessionsIdOfRes.remove(session.getId());
                    resInfo.setSessionId(sessionsIdOfRes);
                    resourcesRepo.save(resInfo);
                }
            }
        }
    }

    /**
     * This method is called right after a client connects to the server
     *
     * @param session WebSocket session
     * @throws Exception exception in handling connection
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("Connected ... " + session.getId());
        idSession.put(session.getId(), session);
    }

    /**
     * This method is called whenever a message is received from the server
     *
     * @param session WebSocket session
     * @param jsonTextMessage received message
     * @throws Exception exception in handling message
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage jsonTextMessage) throws Exception  {
        Exception e = null;
        HttpStatusCode code = HttpStatusCode.INTERNAL_SERVER_ERROR;
        String message = "";
        try 
        {
            message = jsonTextMessage.getPayload();
            log.info("message received: " + message);

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);        
            WebSocketMessageSecurityRequest webSocketMessageSecurity = mapper.readValue(message, WebSocketMessageSecurityRequest.class);
            
            Map<String,String> securityRequest = webSocketMessageSecurity.getSecRequest();
            if(securityRequest == null) {
                log.error("Security Request cannot be empty");
                throw new Exception("Security Request cannot be empty");
            }
            
            WebSocketMessage webSocketMessage = webSocketMessageSecurity.getPayload();
            List<String> resourcesId = webSocketMessage.getIds();
            log.debug("Ids: " + resourcesId);
            
            checkAccessPolicies(securityRequest,resourcesId);
            
            Action act = webSocketMessage.getAction();
            switch(act) {
                case SUBSCRIBE:
                    log.debug("Subscribing resources..");
                    Subscribe(session, resourcesId);
                    break;
                case UNSUBSCRIBE:
                    log.debug("Unsubscribing resources..");
                    Unsubscribe(session, resourcesId);
                    break;
            }
        }catch (JsonParseException jsonEx){
            code = HttpStatusCode.BAD_REQUEST;
            e = jsonEx;
            log.error(e.getMessage());
        } catch (IOException ioEx) {
            code = HttpStatusCode.BAD_REQUEST;
            e = ioEx;
            log.error(e.getMessage());
        } catch (EntityNotFoundException entityEx){
            code = HttpStatusCode.NOT_FOUND;
            e = entityEx;
            log.error(e.getMessage());
        } catch (ValidationException vex) {
        	log.error(vex.getMessage());        	
        } catch (Exception ex) {
            e = ex;
            log.error("Generic IO Exception: " + e.getMessage());
        }
        
        if(e != null){
            session.sendMessage(new TextMessage(code.name()+ " "+
                    e.getMessage()));
            sendFailMessage(message,e);
        }
    }


    private void Subscribe(WebSocketSession session, List<String> resourcesId) throws Exception {
        HashMap<String, List<DbResourceInfo>> subscribeList = new HashMap<>();
        for (String resId : resourcesId) {
            // adding new resource info to subscribe map, with pluginId as key
            DbResourceInfo resInfo = getResourceInfo(resId);            
            String pluginId = resInfo.getPluginId();
            // if no plugin id specified, we assume there's only one plugin attached
            if(pluginId == null) {
                List<PlatformInfo> lst = pluginRepo.findAll();
                if(lst == null || lst.isEmpty())
                    throw new Exception("No plugin found");                
                pluginId = lst.get(0).getPlatformId();
            } 
            List<DbResourceInfo> rl;
            if(subscribeList.containsKey(pluginId)) {
                rl = subscribeList.get(pluginId);
            } else {
                rl = new ArrayList<>();
            }            
            rl.add(resInfo);
            subscribeList.put(pluginId, rl);
            //update DB
            List<String> sessionsIdOfRes = resInfo.getSessionId();
            if (sessionsIdOfRes == null) {
                sessionsIdOfRes = new ArrayList<>();
            }
            sessionsIdOfRes.add(session.getId());
            resInfo.setSessionId(sessionsIdOfRes);
            resourcesRepo.save(resInfo);
        }
        
        for(String plugin : subscribeList.keySet() ) {
            List<ResourceInfo> resList = subscribeList.get(plugin).stream().map(ri -> ri.toResourceInfo()).collect(Collectors.toList());
            ResourceAccessMessage msg = new ResourceAccessSubscribeMessage(resList);
            String routingKey = plugin + "." + ResourceAccessMessage.AccessType.SUBSCRIBE.toString().toLowerCase();
            
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);        
            String json = mapper.writeValueAsString(msg);

            rabbitTemplate.convertSendAndReceive(exchange.getName(), routingKey, json);            
            sendSuccessfulAccessMessage(resourcesId, SuccessfulAccessMessageInfo.AccessType.SUBSCRIPTION_START.name());
        }
        
    }
    
    private void Unsubscribe(WebSocketSession session, List<String> resourcesId) throws Exception {
        HashMap<String, List<DbResourceInfo>> unsubscribeList = new HashMap<>();
        for (String resId : resourcesId) {
            // adding new resource info to subscribe map, with pluginId as key
            DbResourceInfo resInfo = getResourceInfo(resId);
            String pluginId = resInfo.getPluginId();
            // if no plugin id specified, we assume there's only one plugin attached
            if(pluginId == null) {
                List<PlatformInfo> lst = pluginRepo.findAll();
                if(lst == null || lst.isEmpty())
                    throw new Exception("No plugin found");                
                pluginId = lst.get(0).getPlatformId();
            } 
            List<DbResourceInfo> rl;
            if(unsubscribeList.containsKey(pluginId)) {
                rl = unsubscribeList.get(pluginId);
            } else {
                rl = new ArrayList<>();
            }            
            rl.add(resInfo);
            unsubscribeList.put(pluginId, rl);
            //update DB
            List<String> sessionsIdOfRes = resInfo.getSessionId();
            if (sessionsIdOfRes != null) {
                sessionsIdOfRes.remove(session.getId());
                resInfo.setSessionId(sessionsIdOfRes);
                resourcesRepo.save(resInfo);            
            }
        }
        for(String plugin : unsubscribeList.keySet() ) {
            List<ResourceInfo> resList = unsubscribeList.get(plugin).stream().map(ri -> ri.toResourceInfo()).collect(Collectors.toList());
            ResourceAccessMessage msg = new ResourceAccessUnSubscribeMessage(resList);
            String routingKey = plugin + "." + ResourceAccessMessage.AccessType.UNSUBSCRIBE.toString().toLowerCase();

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);        
            String json = mapper.writeValueAsString(msg);

            rabbitTemplate.convertSendAndReceive(exchange.getName(), routingKey, json);
            sendSuccessfulAccessMessage(resourcesId, SuccessfulAccessMessageInfo.AccessType.SUBSCRIPTION_END.name());
        }
    }


    /**
     * This method is used to send a push notification message to all the clients connected
     * and subscribed to the resource that emits the notification itself
     *
     * @param obs observation
     */
    public void SendMessage(Observation obs) {
        Map<String,String> secResponse = new HashMap<>();
        ServiceResponseResult serResponse = authManager.generateServiceResponse();
        if(serResponse.isCreatedSuccessfully()) {
            secResponse.put(SECURITY_RESPONSE_HEADER, serResponse.getServiceResponse());
        }        
        WebSocketMessageSecurityResponse messageSecurityResp = new WebSocketMessageSecurityResponse(secResponse, obs);
        
        String internalId = obs.getResourceId();
        DbResourceInfo resInfo = getResourceByInternalId(internalId);
        List<String> sessionIdList = resInfo.getSessionId();
        HashSet<WebSocketSession> sessionList = new HashSet<>();
        if (sessionIdList != null && !sessionIdList.isEmpty()) {
            for (String sessionId : sessionIdList) {
                WebSocketSession session = idSession.get(sessionId);
                if(session != null)
                    sessionList.add(session);
            }

            String mess = "";
            try {
                ObjectMapper map = new ObjectMapper();
                map.configure(SerializationFeature.INDENT_OUTPUT, true);
                map.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                mess = map.writeValueAsString(messageSecurityResp);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            sendAll(sessionList, mess);
        }
    }

    private static void sendAll(Set<WebSocketSession> sessionList, String msg) {
        for (WebSocketSession session : sessionList) {
            try {
                session.sendMessage(new TextMessage(msg)); //.getBasicRemote().sendText(msg);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private DbResourceInfo getResourceInfo(String resId) {
        DbResourceInfo resInfo = null;
        Optional<DbResourceInfo> resInfoOptional = resourcesRepo.findById(resId);
        if(!resInfoOptional.isPresent())
            throw new EntityNotFoundException(resId);
        
        resInfo = resInfoOptional.get();
        return resInfo;
    }
    
    private DbResourceInfo getResourceByInternalId(String internalId) {
        DbResourceInfo resInfo = null;
        try {
            List<DbResourceInfo> resInfoList = resourcesRepo.findByInternalId(internalId);
            if (resInfoList != null && !resInfoList.isEmpty()) {
                for(DbResourceInfo ri: resInfoList){
                    resInfo = ri;
                    List<String> sessionsId = ri.getSessionId();
                    if(sessionsId != null && !sessionsId.isEmpty())
                        break;
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return resInfo;
    }

    /**
     * This method is used to send a successful access message to CRAM
     *
     * @param symbioteIdList list of symbiote IDs
     * @param accessType type of access from {@link eu.h2020.symbiote.core.cci.accessNotificationMessages.SuccessfulAccessMessageInfo.AccessType} name
     */
    public void sendSuccessfulAccessMessage(List<String> symbioteIdList, String accessType){
        List<Date> dateList = new ArrayList<Date>();
        dateList.add(new Date());
        
        notificationService.addSuccessfulAttemptsList(symbioteIdList, dateList, accessType);
        notificationService.sendAccessData();
    }
    
    private void sendFailMessage(String path, Exception e) {
        String appId = "";String issuer = ""; String validationStatus = "";
        String symbioteId = "";
        
        String code = Integer.toString(HttpStatus.INTERNAL_SERVER_ERROR.value());
        String message = e.getMessage();
        if(message == null)
            message = e.toString();
        
        if(e.getClass().equals(EntityNotFoundException.class)){
            code = Integer.toString(HttpStatus.NOT_FOUND.value());
            symbioteId = ((EntityNotFoundException) e).getSymbioteId();
        }
            
        List<Date> dateList = new ArrayList<Date>();
        dateList.add(new Date());
        notificationService.addFailedAttempts(symbioteId, dateList, 
            code, message, appId, issuer, validationStatus, path);
        notificationService.sendAccessData();
    }

    /**
     * This method is used to check access policies towards Authentication Manager
     *
     * @param secHdrs map of security headers
     * @param resourceIdList list of resource IDs
     * @throws Exception security exception
     */
    public void checkAccessPolicies(Map<String, String> secHdrs, List<String> resourceIdList) throws Exception {
        
        log.debug("secHeaders: " + secHdrs);
        SecurityRequest securityReq = new SecurityRequest(secHdrs);

        for(String resourceId: resourceIdList){        
            AuthorizationResult result = authManager.checkResourceUrlRequest(resourceId, securityReq);
            log.info(result.getMessage());
            if(!result.isValidated()) {
                log.error("Resource " + resourceId + "access has been denied with message: " + result.getMessage());
                throw new ValidationException("The access policies were not satisfied for resource " + resourceId + ". MSG: " + result.getMessage());
            }
        }        
    }
}
