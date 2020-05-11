package com.fldys.mesh.device.service;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.fldys.mesh.device.domain.Device;
import com.fldys.mesh.protocol.Message;
import com.fldys.mesh.protocol.Type;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URLDecoder;
import java.util.*;

@Slf4j
@Service
@EnableScheduling
public class DeviceService {
    @Autowired
    Gson gson;
    @Autowired
    private HashOperations ho;

    private String SECURITY_CONNECT = "security_connect";
    private String DEVICES = "devices";
    private String USERS = "users";
    private HashMap<String, SocketIOClient> sessions = new HashMap<>();

    public Device getDevice(String did) {
        return (Device) ho.get(DEVICES, did);
    }

    public Device getDevice(SocketIOClient client) {
        String did = did(client);
        return (Device) ho.get(DEVICES, did);
    }

    public SocketIOClient getClient(String name){
        if (ho.hasKey(USERS, name)) {
            log.error("99999999111" + ho.get(USERS, name));
            return sessions.get(ho.get(USERS, name));
        } else {
            return null;
        }
    }

    @OnConnect
    private void connect(SocketIOClient client) {
        String sessionId = client.getSessionId().toString();
        log.error("" + System.nanoTime());
        ho.put(SECURITY_CONNECT, sessionId, System.nanoTime());

        Device device = init(client);
        if (device != null) {
            log.error("99999999222" + sessionId + " :" +client);
            sessions.put(sessionId, client);
        } else {
            ho.delete(SECURITY_CONNECT, sessionId);
            client.disconnect();
        }
        log.info("connect" + sessionId);
    }

    @OnDisconnect
    private void disconnect(SocketIOClient client) {
        String sessionId = client.getSessionId().toString();
        sessions.remove(sessionId);
        ho.delete(SECURITY_CONNECT, sessionId);
        log.info("disconnect" + sessionId);
    }

    @OnEvent(value = DeviceApi.SERVICE)
    private void event(SocketIOClient client, String data, AckRequest ackSender) {
        log.info("conference data:" + data.toString());
        Message message = gson.fromJson(data, Message.class);
        switch (message.api) {
            case DeviceApi.REGISTER:
                register(client, message , data, ackSender);
                break;
            case DeviceApi.UNREGISTER:
                unregister(client, message, ackSender);
                break;
            case DeviceApi.NICKNAME:
                nickname(client, message, ackSender);
                break;
            default:
                break;
        }
    }

    private void register(SocketIOClient client, Message message, String data, AckRequest ackSender) {
        String sessionId = client.getSessionId().toString();
        JsonObject jo = JsonParser.parseString(data).getAsJsonObject();
        Device device = gson.fromJson(jo.getAsJsonObject("data").get("device"), Device.class);
        log.error("register11:" + device);
        Device update = init(client);
        update.name = device.name;
        update.plat = device.plat;
        update.brand = device.brand;
        update.model = device.model;
        update.version = device.version;
        if (!StringUtils.isEmpty(update.name)) {
            log.error("register99:" + device);
            ho.delete(SECURITY_CONNECT, update.sid);
            ho.put(DEVICES, update.did, update);
        }

        message.type = Type.Ack.code;
        message.data.put("device", update);
        ackSender.sendAckData(message);
    }

    private void unregister(SocketIOClient client, Message message, AckRequest ackSender) {
        String sessionId = client.getSessionId().toString();
        sessions.remove(sessionId);
        ho.delete(SECURITY_CONNECT, sessionId);
    }

    private void nickname(SocketIOClient client, Message message, AckRequest ackSender) {
        Device device = init(client);
        String nickname = message.data.get("nickname").toString();
        message.type = Type.Ack.code;
        if (ho.hasKey(USERS, nickname)) {
            message.code = 201;
            message.desc = nickname + "is exist!";
            ackSender.sendAckData(message);
        } else {
            ho.put(USERS, nickname, device.sid);
            ackSender.sendAckData(message);
        }
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

    @Scheduled(initialDelay = 2000, fixedRate = 10000)
    private void securityListener() {
        Map<String, Long> securities = (Map<String, Long>) ho.entries(SECURITY_CONNECT);
        Set<Map.Entry<String, Long>> entries = securities.entrySet();
        Iterator<Map.Entry<String, Long>> it = entries.iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> in = it.next();
            String key = in.getKey();
            long time = in.getValue();
            log.error("" + (System.nanoTime() - time));
            if (System.nanoTime() - time > 10000000000l) {
                SocketIOClient client = sessions.get(key);
                if (client != null) {
                    client.disconnect();
                }
                sessions.remove(key);
                ho.delete(SECURITY_CONNECT, key);
            }
        }
    }

    private String did(SocketIOClient client) {
        try {
            return URLDecoder.decode((client.getHandshakeData().getSingleUrlParam("did")), "UTF-8");
        } catch (Exception e) {
            log.error("did:" + e.getMessage());
        }
        return null;
    }
}
