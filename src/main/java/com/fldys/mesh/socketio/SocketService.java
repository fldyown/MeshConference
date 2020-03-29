package com.fldys.mesh.socketio;

import com.corundumstudio.socketio.SocketIOServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Slf4j
@Service
public class SocketService {
    @Autowired
    private SocketIOServer server;

    @PostConstruct
    public void create() {
        server.start();
    }

    @PreDestroy
    public void destroy() {
        server.stop();
    }

    public SocketIOServer getServer() {
        return server;
    }
}
