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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
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
    private HashOperations ho;
    @Autowired
    SocketService socketService;
    @Autowired
    DeviceService deviceService;

    private HashMap<String, Conference> conferences = new HashMap<>();

    @OnEvent(value = ConferenceApi.SERVICE)
    public void event(SocketIOClient client, String data, AckRequest ackSender) {
        log.info("conference data:" + data);
        Message message = gson.fromJson(data, Message.class);
        switch (message.api) {
            case ConferenceApi.CREATE:
                create(client, message, data, ackSender);
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
            case ConferenceApi.INVITE:
                invite(client, message, ackSender);
                break;
            case ConferenceApi.SDP:
            case ConferenceApi.CANDIDATE:
                route(client, message, ackSender);
                break;
            default:
                break;
        }
    }

    public void create(SocketIOClient client, Message message, String data, AckRequest ackSender) {
        Device device = deviceService.getDevice(client);
        JsonObject jo = JsonParser.parseString(data).getAsJsonObject();
//        Object obj = message.data.get("conference");
        Conference conference = init(gson.fromJson(jo.getAsJsonObject("data").getAsJsonObject("conference"), Conference.class), device);
        conferences.put(conference.id, conference);
        client.joinRoom(conference.id);
        Member from = init(device, conference);
        message.data.put("conference", conference);
        message.data.put("from", from);

        log.info("message:" + message.toString());

        ackSyncNotify(client, message, ackSender, device, conference);
    }

    public void destroy(SocketIOClient client, Message message, AckRequest ackSender) {
        Device device = deviceService.getDevice(client);
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
        Device device = deviceService.getDevice(client);
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
        Device device = deviceService.getDevice(client);
        String cid = (String) message.data.get("cid");
        Conference conference = conferences.get(cid);
        if (conference.members == null || !conference.members.containsKey(device.did)) {
            message.type = Type.Ack.code;
            message.code = 404;
            message.desc = "can't find user";
            ackSender.sendAckData(message);
            return;
        }

        Member quit = conference.members.remove(device.did);

        if (conference.histories == null) {
            conference.histories = new HashMap<>();
        }

        conference.histories.put(quit.did, quit);

        client.leaveRoom(conference.id);

        message.data.put("conference", conference);
        message.data.put("from", quit);

        log.info("message:" + message.toString());

        ackSyncNotify(client, message, ackSender, device, conference);
    }

    public void invite(SocketIOClient client, Message message, AckRequest ackSender) {
        Device device = deviceService.getDevice(client);
        String cid = (String) message.data.get("cid");
        String name = (String) message.data.get("name");
        Conference conference = conferences.get(cid);

        SocketIOClient sc = deviceService.getClient(name);

        if (conference == null || sc == null) {
            log.error("conference:" + conference + " sc:" + sc);
            message.type = Type.Ack.code;
            message.code = 404;
            message.desc = "can't find user";
            ackSender.sendAckData(message);
            return;
        }

        sc.joinRoom(conference.id);

        Device d = deviceService.getDevice(sc);
        Member invite = init(d, conference);

        Member from = conference.members.get(device.did);

        if (conference.invites == null) {
            conference.invites = new HashMap<String, Member>();
        }

        conference.invites.put(invite.did, invite);


        message.data.put("conference", conference);
        message.data.put("from", from);

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
            message.type = Type.Ack.code;
            message.code = 404;
            message.desc = "can't find user";
            ackSender.sendAckData(message);
            return;
        }
        message.type = Type.Notify.code;

        Device dnotify = deviceService.getDevice(to);
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
        log.info("conference:" + conference.toString());
        log.info("device:" + device.toString());
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
