/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.service.notificationResource;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.websocket.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author luca-
 */
//This class is used only for testing web socket
@ClientEndpoint
public class WSClient {

    private static final Logger log = LoggerFactory.getLogger(WSClient.class);

    private static Object waitLock = new Object();

    final static CountDownLatch messageLatch = new CountDownLatch(1);

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("Connected to endpoint: " + session.getBasicRemote());
        try {
            System.out.println("Connected with sessionId: " + session.getId());
        } catch (Exception ex) {
            log.error("error occured at sender " + session, ex);
        }
    }

    @OnMessage
    public void onMessage(String message) {
        //receive message
        System.out.println("Received msg: " + message);
    }

    private static void wait4TerminateSignal() {
        synchronized (waitLock) {
            try {
                waitLock.wait();
            } catch (InterruptedException e) {
            }
        }
    }

    public static void main(String[] args) {
        WebSocketContainer container = null;//
        Session session = null;
        try {
            container = ContainerProvider.getWebSocketContainer();
            session = container.connectToServer(WSClient.class, URI.create("ws://localhost:8080/notification"));

            session.getBasicRemote().sendText("[2]");

            messageLatch.await(60, TimeUnit.SECONDS);

            //wait4TerminateSignal();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
