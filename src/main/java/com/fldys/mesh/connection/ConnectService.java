package com.fldys.mesh.connection;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.fldys.mesh.device.service.DeviceService;
import com.google.gson.Gson;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.UUID;

@Log4j2
@Service
public class ConnectService {
    @Autowired
    private Gson gson;
    @Autowired
    private DeviceService deviceService;
    public HashMap<String, SocketIOClient> connections = new HashMap<>();

    @OnConnect
    public void connect(SocketIOClient client) {
        connections.put(client.getSessionId().toString(), client);
        log.info("ConnectService:connect:" + client.getSessionId());
        client.sendEvent("data", "连接成功!");
    }

    @OnDisconnect
    public void disconnect(SocketIOClient client) {
        connections.remove(client.getSessionId().toString());
        log.info("ConnectService:disconnect" + client.getSessionId());
    }
}
