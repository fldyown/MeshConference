package com.fldys.mesh.conference.service;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.fldys.mesh.conference.domain.Conference;
import com.fldys.mesh.conference.domain.Member;
import com.fldys.mesh.device.domain.Device;
import com.fldys.mesh.device.service.DeviceService;
import com.fldys.mesh.protocol.Message;
import com.fldys.mesh.protocol.Type;
import com.fldys.mesh.socketio.SocketService;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.UUID;

@Slf4j
@Service
public class ConferenceService {
    @Autowired
    Gson gson;
    @Autowired
    SocketService socketService;
    @Autowired
    DeviceService deviceService;

    private HashMap<String, Conference> conferences = new HashMap<>();

    @OnEvent(value = ConferenceApi.SERVICE)
    public void event(SocketIOClient client, Message message, AckRequest ackSender) {
        log.info("conference data:" + message);
        switch (message.api) {
            case ConferenceApi.CREATE:
                create(client, message, ackSender);
                break;
            case ConferenceApi.DESTROY:
                destroy(client, message, ackSender);
                break;
            case ConferenceApi.JOIN:
                join(client, message, ackSender);
                break;
            case ConferenceApi.QUIT:
                quit(client, message, ackSender);
                break;
            case ConferenceApi.SDP:
            case ConferenceApi.CANDIDATE:
                route(client, message, ackSender);
                break;
            default:
                break;
        }
    }

    public void create(SocketIOClient client, Message message, AckRequest ackSender) {
        Device device = deviceService.sessions.get(client.getSessionId().toString());
        Object obj = message.data.get("conference");
        Conference conference = init(gson.fromJson(obj.toString(), Conference.class), device);
        conferences.put(conference.id, conference);
        client.joinRoom(conference.id);
        Member from = init(device, conference);
        message.data.put("conference", conference);
        message.data.put("from", from);

        log.info("message:" + message.toString());

        ackSyncNotify(client, message, ackSender, device, conference);
    }

    public void destroy(SocketIOClient client, Message message, AckRequest ackSender) {
        Device device = deviceService.sessions.get(client.getSessionId().toString());
        String cid = (String) message.data.get("cid");
        Conference conference = conferences.get(cid);
        conference.destroyed = System.currentTimeMillis();

        Member from = conference.getMembers().get(device.did);

        message.data.put("conference", conference);
        message.data.put("from", from);

        log.info("message:" + message.toString());

        ackSyncNotify(client, message, ackSender, device, conference);
    }

    public void join(SocketIOClient client, Message message, AckRequest ackSender) {
        Device device = deviceService.sessions.get(client.getSessionId().toString());
        String cid = (String) message.data.get("cid");
        Conference conference = conferences.get(cid);
        Member join = init(device, conference);
        join.join = System.currentTimeMillis();
        if (conference.members == null) {
            conference.members = new HashMap<>();
        }
        conference.members.put(join.did, join);

        client.joinRoom(conference.id);

        message.data.put("conference", conference);
        message.data.put("from", join);

        log.info("message:" + message.toString());

        ackSyncNotify(client, message, ackSender, device, conference);
    }

    public void quit(SocketIOClient client, Message message, AckRequest ackSender) {
        Device device = deviceService.sessions.get(client.getSessionId().toString());
        String cid = (String) message.data.get("cid");
        Conference conference = conferences.get(cid);
        if (conference.members == null || !conference.members.containsKey(device.did)) {
            message.code = 404;
            message.desc = "can't find user";
            ackSender.sendAckData(message);
            return;
        }

        Member quit = conference.members.remove(device.did);

        client.leaveRoom(conference.id);

        message.data.put("conference", conference);
        message.data.put("from", quit);

        log.info("message:" + message.toString());

        ackSyncNotify(client, message, ackSender, device, conference);
    }

    private void ackSyncNotify(SocketIOClient excluded, Message message, AckRequest ackSender, Device device, Conference conference) {
        //ack
        message.type = Type.Ack.code;
        ackSender.sendAckData(message);
        //sync
        if (!StringUtils.isEmpty(device.uid)) {
            message.type = Type.Sync.code;
            socketService.getServer().getRoomOperations(device.uid).sendEvent(ConferenceApi.SERVICE, excluded, message);
        }
        //notify
        message.type = Type.Notify.code;
        socketService.getServer().getRoomOperations(conference.id).sendEvent(ConferenceApi.SERVICE, excluded, message);
    }

    private void route(SocketIOClient client, Message message, AckRequest ackSender) {
        String cid = (String) message.data.get("cid");
        String to = (String) message.data.get("to");
        String from = (String) message.data.get("from");
        Conference conference = conferences.get(cid);

        log.info("cid:" + cid + " to:" + to + " from:" + from + "\n" + conference.toString());

        if (conference.members == null || !conference.members.containsKey(to) || !conference.members.containsKey(from)) {
            message.code = 404;
            message.desc = "can't find user";
            ackSender.sendAckData(message);
            return;
        }
        message.type = Type.Notify.code;

        Device dnotify = deviceService.devices.get(to);
        if (dnotify == null) {
            return;
        }

        SocketIOClient cnotify = socketService.getServer().getClient(UUID.fromString(dnotify.sid));
        if (cnotify == null) {
            return;
        }

        cnotify.sendEvent(ConferenceApi.SERVICE, message);
    }

    int i = 100;

    private Conference init(Conference conference, Device device) {
        Conference c = conference;
        c.id = "" + (i++);
        c.created = System.currentTimeMillis();
        c.maxNumber = 5;
        c.founder = device.did;
        c.owner = device.did;
        return c;
    }

    private Member init(Device device, Conference conference) {
        Member m = new Member();
        m.did = device.did;
        m.cid = conference.id;
        return m;
    }
}
