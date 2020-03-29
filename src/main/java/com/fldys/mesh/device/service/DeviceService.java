package com.fldys.mesh.device.service;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.fldys.mesh.device.domain.Device;
import com.fldys.mesh.protocol.Message;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.net.URLDecoder;
import java.util.*;

@Slf4j
@Service
public class DeviceService {
    @Autowired
    Gson gson;
    @Autowired
    private MongoTemplate mongo;
    @Autowired
    private RedisTemplate<String, Serializable> template;

    public HashMap<String, Device> devices = new HashMap<>();
    public HashMap<String, SocketIOClient> clients = new HashMap<>();
    public HashMap<String, Device> sessions = new HashMap<>();

    @OnConnect
    public void connect(SocketIOClient client) {
        log.info("connect" + client.getSessionId());
        Device device = init(client);
        if (device != null) {
            clients.put(client.getSessionId().toString(), client);
            sessions.put(client.getSessionId().toString(), device);
            devices.put(device.did, device);
        } else {
            client.disconnect();
        }
    }

    @OnDisconnect
    public void disconnect(SocketIOClient client) {
        clients.remove(client.getSessionId().toString());
        sessions.remove(client.getSessionId().toString());
        log.info("disconnect" + client.getSessionId());
    }

    @OnEvent(value = DeviceApi.SERVICE)
    public void event(SocketIOClient client, Message message, AckRequest ackSender) {
        log.debug("conference data:" + message);

        switch (message.api) {
            case DeviceApi.REGISTER:
                register(client, message, ackSender);
                break;
            case DeviceApi.UNREGISTER:
                unregister(client, message, ackSender);
                break;
            default:
                break;
        }
    }

    public void register(SocketIOClient client, Message message, AckRequest ackSender) {
        Object obj = message.data.get("device");
        Device device = gson.fromJson(obj.toString(), Device.class);
        Device old = sessions.get(client.getSessionId().toString());
        old.name = device.name;
        old.plat = device.plat;
        old.brand = device.brand;
        old.model = device.model;
        old.version = device.version;
    }

    public void unregister(SocketIOClient client, Message message, AckRequest ackSender) {
        devices.remove(client.getSessionId().toString());
        sessions.remove(client.getSessionId().toString());
    }

    private Device init(SocketIOClient client) {
        try {
            Device device = new Device();
            device.did = URLDecoder.decode((client.getHandshakeData().getSingleUrlParam("did")), "UTF-8");
            device.sid = client.getSessionId().toString();
            device.create = System.currentTimeMillis();
            return device;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }
}
