/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.service.notificationResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.exceptions.GenericException;
import eu.h2020.symbiote.interfaces.ResourcesRepository;
import eu.h2020.symbiote.messages.ResourceAccessMessage;
import eu.h2020.symbiote.messages.ResourceAccessSubscribeMessage;
import eu.h2020.symbiote.core.model.Observation;
import eu.h2020.symbiote.resources.RapDefinitions;
import eu.h2020.symbiote.resources.ResourceInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 *
 * @author luca-
 */
@Component
public class WebSocketController extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(WebSocketController.class);

    @Autowired
    ResourcesRepository resourcesRepo;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    @Qualifier(RapDefinitions.PLUGIN_EXCHANGE_OUT)
    TopicExchange exchange;

    private HashMap<String, WebSocketSession> id_session = new HashMap<String, WebSocketSession>();

    @Override
    public void handleTransportError(WebSocketSession session, Throwable throwable) throws Exception {
        log.error("error occured at sender " + session, throwable);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info(String.format("Session %s closed because of %s", session.getId(), status.getReason()));
        id_session.remove(session.getId());

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
        id_session.put(session.getId(), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage jsonTextMessage) throws Exception {
        String message = jsonTextMessage.getPayload();
        log.info("message received: " + message);

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

            //update DB
            List<String> sessionsIdOfRes = resInfo.getSessionId();
            if (sessionsIdOfRes == null) {
                sessionsIdOfRes = new ArrayList<String>();
            }
            sessionsIdOfRes.add(session.getId());
            resInfo.setSessionId(sessionsIdOfRes);
            resourcesRepo.save(resInfo);
        }

        ResourceAccessMessage msg;
        String routingKey;

        msg = new ResourceAccessSubscribeMessage(resInfoList);
        routingKey = ResourceAccessMessage.AccessType.SUBSCRIBE.toString().toLowerCase();

        String json = mapper.writeValueAsString(msg);

        Object obj = rabbitTemplate.convertSendAndReceive(exchange.getName(), routingKey, json);
        String response = new String((byte[]) obj, "UTF-8");

        log.info("Response: " + response);
    }

    public void SendMessage(Observation obs) {
        String resourceId = obs.getResourceId();
        ResourceInfo resInfo = getResourceInfo(resourceId);
        List<String> sessionIdList = resInfo.getSessionId();
        List<WebSocketSession> sessionList = new ArrayList<>();
        if (sessionIdList != null && !sessionIdList.isEmpty()) {
            for (String sessionId : sessionIdList) {
                WebSocketSession session = id_session.get(sessionId);
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

    private static void sendAll(List<WebSocketSession> sessionList, String msg) {
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
        try {
            Optional<ResourceInfo> resInfoOptional = resourcesRepo.findById(resId);
            if (resInfoOptional.isPresent()) {
                resInfo = resInfoOptional.get();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        //SOLO MOMENTANEO
        /*if (resInfo == null) {
            List<ResourceInfo> resInfo2 = resourcesRepo.findAll();
            resInfo = resInfo2.get(0);
        }*/

        return resInfo;
    }
}
