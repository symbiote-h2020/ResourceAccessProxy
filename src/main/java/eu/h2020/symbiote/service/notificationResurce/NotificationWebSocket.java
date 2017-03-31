/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.service.notificationResurce;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.exceptions.GenericException;
import eu.h2020.symbiote.interfaces.ResourcesRepository;
import eu.h2020.symbiote.messages.ResourceAccessGetMessage;
import eu.h2020.symbiote.messages.ResourceAccessHistoryMessage;
import eu.h2020.symbiote.messages.ResourceAccessMessage;
import eu.h2020.symbiote.messages.ResourceAccessSubscribeMessage;
import eu.h2020.symbiote.model.data.Observation;
import eu.h2020.symbiote.resources.RapDefinitions;
import eu.h2020.symbiote.resources.ResourceInfo;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.uri.UriParameter;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 *
 * @author luca-
 */
@ServerEndpoint("/notification")
public class NotificationWebSocket {
    
    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocket.class);

    @Autowired
    ResourcesRepository resourcesRepo;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    @Qualifier(RapDefinitions.PLUGIN_EXCHANGE_OUT)
    TopicExchange exchange;
    
    private HashMap<String,Session> id_session = new HashMap<String,Session>();

    @OnOpen
    public void onOpen(Session session) {
        // Method performed at the opening of the connection
        id_session.put(session.getId(), session);
    }

    @OnMessage
    public String onMessage(String message, Session session) throws GenericException, JsonProcessingException, UnsupportedEncodingException {
        // Method executed upon receipt of a message
        List<String> resourcesId = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        try {
            resourcesId = mapper.readValue(message, new TypeReference<List<String>>() {
            });
        } catch (IOException ex) {
            throw new GenericException(HttpStatusCode.BAD_REQUEST.getInfo());
        }

        List<ResourceInfo> resInfoList = new ArrayList<ResourceInfo>();
        
        for (String resId : resourcesId) {
            ResourceInfo resInfo = getResourceInfo(resId);
            resInfoList.add(resInfo);
        }

        ResourceAccessMessage msg;
        String routingKey;

        msg = new ResourceAccessSubscribeMessage(resInfoList);
        routingKey = ResourceAccessMessage.AccessType.SUBSCRIBE.toString().toLowerCase();

        String json = mapper.writeValueAsString(msg);

        Object obj = rabbitTemplate.convertSendAndReceive(exchange.getName(), routingKey, json);
        String response = new String((byte[]) obj, "UTF-8");

        return "Response: " + response;
    }

    @OnClose
    public void onClose(Session session) {
        // Method performed on closing the connection
        
        //RIMUOVERE DA RESOURCEINFO
        id_session.remove(session.getId());
    }

    @OnError
    public void onError(Throwable exception, Session session) {
        // A method performed on error
    }
    
    
    public void SendMessage(Observation obs){
        String resourceId = obs.getResourceId();
        ResourceInfo resInfo = getResourceInfo(resourceId);
        List<String> sessionIdList = resInfo.getSessionId();
        List<Session> sessionList = new ArrayList<>();
        for(String sessionId : sessionIdList){
            Session session = id_session.get(sessionId);
            sessionList.add(session);
        }
        
        String mess = "";
        try{
                ObjectMapper map = new ObjectMapper();
                map.configure(SerializationFeature.INDENT_OUTPUT, true);
                map.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                mess = map.writeValueAsString(obs);
            }
            catch(Exception e){
                log.error(e.getMessage());
            }
        sendAll(sessionList,mess);
    }

    public static void sendAll(List<Session> sessionList, String msg) {
        for (Session session : sessionList) {
            try {
                session.getBasicRemote().sendText(msg);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }
    
    private ResourceInfo getResourceInfo(String resId){
        ResourceInfo resInfo = null;
        try {
                Optional<ResourceInfo> resInfoOptional = resourcesRepo.findByResourceId(resId);
                if (resInfoOptional.isPresent()) {
                    resInfo = resInfoOptional.get();
                }
        } catch (Exception e) {
        }

            //SOLO MOMENTANEO
            if (resInfo == null) {
                List<ResourceInfo> resInfo2 = resourcesRepo.findAll();
                resInfo = resInfo2.get(0);
            }
            
            return resInfo;
    }
}
