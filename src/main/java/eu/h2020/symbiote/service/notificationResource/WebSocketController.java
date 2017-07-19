/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.service.notificationResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.exceptions.GenericException;
import eu.h2020.symbiote.resources.db.ResourcesRepository;
import eu.h2020.symbiote.messages.access.ResourceAccessMessage;
import eu.h2020.symbiote.messages.access.ResourceAccessSubscribeMessage;
import eu.h2020.symbiote.cloud.model.data.observation.Observation;
import eu.h2020.symbiote.exceptions.EntityNotFoundException;
import eu.h2020.symbiote.interfaces.conditions.NBInterfaceWebSocketCondition;
import eu.h2020.symbiote.messages.access.ResourceAccessUnSubscribeMessage;
import eu.h2020.symbiote.messages.accessNotificationMessages.NotificationMessage;
import eu.h2020.symbiote.messages.accessNotificationMessages.SuccessfulAccessMessageInfo;
import eu.h2020.symbiote.resources.RapDefinitions;
import eu.h2020.symbiote.resources.db.ResourceInfo;
import eu.h2020.symbiote.service.notificationResource.WebSocketMessage.Action;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
@Conditional(NBInterfaceWebSocketCondition.class)
@Component
@CrossOrigin 
public class WebSocketController extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(WebSocketController.class);

    @Autowired
    ResourcesRepository resourcesRepo;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    @Qualifier(RapDefinitions.PLUGIN_EXCHANGE_OUT)
    TopicExchange exchange;

    private final HashMap<String, WebSocketSession> idSession = new HashMap();

    @Override
    public void handleTransportError(WebSocketSession session, Throwable throwable) throws Exception {
        log.error("error occured at sender " + session, throwable);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("Session " + session.getId() + " closed with status " + status.getCode());
        idSession.remove(session.getId());

        //update DB
        List<ResourceInfo> resInfoList = resourcesRepo.findAll();
        if (resInfoList != null) {
            for (ResourceInfo resInfo : resInfoList) {
                List<String> sessionsIdOfRes = resInfo.getSessionId();
                if (sessionsIdOfRes != null) {
                    sessionsIdOfRes.remove(session.getId());
                    resInfo.setSessionId(sessionsIdOfRes);
                    resourcesRepo.save(resInfo);
                }
            }
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("Connected ... " + session.getId());
        idSession.put(session.getId(), session);
    }

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
            WebSocketMessage webSocketMessage = mapper.readValue(message, WebSocketMessage.class);

            List<String> resourcesId = webSocketMessage.getIds();
            log.debug("Ids: " + resourcesId);
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
        List<ResourceInfo> resInfoList = new ArrayList();
        for (String resId : resourcesId) {
            ResourceInfo resInfo = getResourceInfo(resId);
            resInfoList.add(resInfo);

            //update DB
            List<String> sessionsIdOfRes = resInfo.getSessionId();
            if (sessionsIdOfRes == null) {
                sessionsIdOfRes = new ArrayList();
            }
            sessionsIdOfRes.add(session.getId());
            resInfo.setSessionId(sessionsIdOfRes);
            resourcesRepo.save(resInfo);
        }
        ResourceAccessMessage msg = new ResourceAccessSubscribeMessage(resInfoList);
        String routingKey = ResourceAccessMessage.AccessType.SUBSCRIBE.toString().toLowerCase();

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);        
        String json = mapper.writeValueAsString(msg);
        
        Object o = rabbitTemplate.convertSendAndReceive(exchange.getName(), routingKey, json);
        if(o == null)
            throw new Exception("No response from plugin");
        sendSuccessfulAccessMessage(resourcesId, SuccessfulAccessMessageInfo.AccessType.SUBSCRIPTION_START.name());
    }
    
    private void Unsubscribe(WebSocketSession session, List<String> resourcesId) throws Exception {
        List<ResourceInfo> resInfoList = new ArrayList();
        for (String resId : resourcesId) {
            ResourceInfo resInfo = getResourceInfo(resId);
            resInfoList.add(resInfo);
            List<String> sessionsIdOfRes = resInfo.getSessionId();
            if (sessionsIdOfRes != null) {
                sessionsIdOfRes.remove(session.getId());
                resInfo.setSessionId(sessionsIdOfRes);
                resourcesRepo.save(resInfo);            
            }
        }
        ResourceAccessMessage msg = new ResourceAccessUnSubscribeMessage(resInfoList);
        String routingKey = ResourceAccessMessage.AccessType.UNSUBSCRIBE.toString().toLowerCase();

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);        
        String json = mapper.writeValueAsString(msg);
        
        rabbitTemplate.convertSendAndReceive(exchange.getName(), routingKey, json);
        sendSuccessfulAccessMessage(resourcesId, SuccessfulAccessMessageInfo.AccessType.SUBSCRIPTION_END.name());
    }

    public void SendMessage(Observation obs) {
        String internalId = obs.getResourceId();
        ResourceInfo resInfo = getResourceByInternalId(internalId);
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
                mess = map.writeValueAsString(obs);
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

    private ResourceInfo getResourceInfo(String resId) {
        ResourceInfo resInfo = null;
        Optional<ResourceInfo> resInfoOptional = resourcesRepo.findById(resId);
        if(!resInfoOptional.isPresent())
            throw new EntityNotFoundException(resId);
        
        resInfo = resInfoOptional.get();
        return resInfo;
    }
    
    private ResourceInfo getResourceByInternalId(String internalId) {
        ResourceInfo resInfo = null;
        try {
            List<ResourceInfo> resInfoList = resourcesRepo.findByInternalId(internalId);
            if (resInfoList != null && !resInfoList.isEmpty()) {
                for(ResourceInfo ri: resInfoList){
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
    
    
    public static void sendSuccessfulAccessMessage(List<String> symbioteIdList, String accessType){
        String jsonNotificationMessage = null;
        ObjectMapper map = new ObjectMapper();
        map.configure(SerializationFeature.INDENT_OUTPUT, true);
        map.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        NotificationMessage notificationMessage = new NotificationMessage();
        
        try{
            notificationMessage.SetSuccessfulAttemptsList(symbioteIdList, timestamp, accessType);
            jsonNotificationMessage = map.writeValueAsString(notificationMessage);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
        NotificationMessage.SendSuccessfulAttemptsMessage(jsonNotificationMessage);
    }
    
    private void sendFailMessage(String path, Exception e) {
        String jsonNotificationMessage = null;
        String appId = "";String issuer = ""; String validationStatus = "";
        String symbioteId = "";
        ObjectMapper mapper = new ObjectMapper();
        
        String code = Integer.toString(HttpStatus.INTERNAL_SERVER_ERROR.value());
        String message = e.getMessage();
        if(message == null)
            message = e.toString();
        
        if(e.getClass().equals(EntityNotFoundException.class)){
            code = Integer.toString(HttpStatus.NOT_FOUND.value());
            symbioteId = ((EntityNotFoundException) e).getSymbioteId();
        }
            
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        NotificationMessage notificationMessage = new NotificationMessage();
        try {
            notificationMessage.SetFailedAttempts(symbioteId, timestamp, 
            code, message, appId, issuer, validationStatus, path); 
            jsonNotificationMessage = mapper.writeValueAsString(notificationMessage);
        } catch (JsonProcessingException jsonEx) {
            log.error(jsonEx.getMessage());
        }
        NotificationMessage.SendFailAccessMessage(jsonNotificationMessage);
    }
}
