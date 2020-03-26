package com.fldys.mesh.socketio;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Slf4j
@Component
@Service
public class SocketService {
    @Autowired
    private SocketIOServer server;
    @Autowired
    private Gson gson;

    @PostConstruct
    public void create() {
        server.start();
    }

    @PreDestroy
    public void destroy() {
        server.stop();
    }

    @OnConnect
    public void connect(SocketIOClient client) {
        System.out.println("connect:" + client.getSessionId());
        log.info("SocketHandlerService:" + client.toString());
        client.sendEvent("data", "连接成功!");
    }

    @OnDisconnect
    public void disconnect(SocketIOClient client) {
        System.out.println("disconnect:" + client.getSessionId());
    }

//    @OnEvent(value = "user")
//    public void event(SocketIOClient client, Object data, AckRequest ackSender) {
//        Message message = gson.fromJson(data.toString(), Message.class);
//        ackSender.sendAckData(message);
//        log.info("SocketHandlerService2222222:" + message.toString());
//    }

    @OnEvent(value = "broadcast")
    public void broadcast(SocketIOClient client, AckRequest ackRequest, Object data) {
        log.info("SocketHandlerService:" + client.toString());
    }


    public SocketIOServer getServer() {
        return server;
    }

}
