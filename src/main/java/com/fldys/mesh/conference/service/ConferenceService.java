package com.fldys.mesh.conference.service;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fldys.mesh.conference.domain.Conference;
import com.fldys.mesh.conference.domain.Member;
import com.fldys.mesh.conference.domain.State;
import com.fldys.mesh.connection.ConnectService;
import com.fldys.mesh.protocol.Message;
import com.fldys.mesh.protocol.Type;
import com.fldys.mesh.socketio.SocketService;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

@Slf4j
@Service
public class ConferenceService {
    @Autowired
    Gson gson;
    @Autowired
    SocketService service;
    @Autowired
    ConnectService connectService;
//    @Autowired
//    ConferenceRepository repository;

    HashMap<String, Conference> conferences = new HashMap<>();

    @OnEvent(value = "conference")
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

            case ConferenceApi.SDP:

                sdp(client, message, ackSender);
                break;

            case ConferenceApi.CANDIDATE:

                candidate(client, message, ackSender);
                break;
            default:
                break;
        }
    }

    public void create(SocketIOClient client, Message message, AckRequest ackSender) {
        Object obj = message.data.get("conference");
        Conference conference = init(gson.fromJson(obj.toString(), Conference.class), client);
        log.info("conference:55555:" + conference);
        log.info("conference:" + conference.toString());
        client.joinRoom(conference.id);

        Member from = init(client);

        conferences.put(conference.id, conference);

        message.data.put("conference", conference);
        message.data.put("from", from);
        message.type = Type.Ack.code;

        log.info("message:" + message.toString());
        //ack
        ackSender.sendAckData(message);

        //sync
        if (!StringUtils.isEmpty(from.uid)) {
            message.type = Type.Sync.code;
            service.getServer().getRoomOperations(from.uid).sendEvent(ConferenceApi.BASE, message);
        }

        //notify
        message.type = Type.Notify.code;
        service.getServer().getRoomOperations(conference.id).sendEvent(ConferenceApi.BASE, message);
    }

    public void destroy(SocketIOClient client, Message message, AckRequest ackSender) {
        String cid = (String) message.data.get("cid");
        Conference conference = conferences.get(cid);
        conference.destroyed = System.currentTimeMillis();
        conference.state = State.destroy;
//        conference = repository.save(conference);

        Member from = new Member();
        from.did = client.getSessionId().toString();

        message.data.put("conference", conference);
        message.data.put("from", from);

        //ack
        ackSender.sendAckData(message);

        //sync
        if (!StringUtils.isEmpty(from.uid)) {
            message.type = Type.Sync.code;
            service.getServer().getRoomOperations(from.uid).sendEvent(ConferenceApi.BASE, message);
        }

        //notify
        message.type = Type.Notify.code;
        service.getServer().getRoomOperations(conference.id).sendEvent(ConferenceApi.BASE, message);
    }

    public void join(SocketIOClient client, Message message, AckRequest ackSender) {
        String cid = (String) message.data.get("cid");

        Conference conference = conferences.get(cid);

        Member join = init(client);

        if (conference.members == null) {
            conference.members = new ArrayList<>();
        }
        conference.members.add(join);

        client.joinRoom(conference.id);

        message.data.put("conference", conference);
        message.data.put("from", join);

        //ack
        ackSender.sendAckData(message);

        //sync
        if (!StringUtils.isEmpty(join.uid)) {
            message.type = Type.Sync.code;
            service.getServer().getRoomOperations(join.uid).sendEvent(ConferenceApi.BASE, message);
        }

        //notify
        message.type = Type.Notify.code;
        service.getServer().getRoomOperations(conference.id).sendEvent(ConferenceApi.BASE, message);
    }

    public void quit(SocketIOClient client, Message message, AckRequest ackSender) {

    }

    private void sdp(SocketIOClient client, Message message, AckRequest ackSende) {
        String cid = (String) message.data.get("cid");
        String to = (String) message.data.get("to");
        String from = (String) message.data.get("from");
        Conference conference = conferences.get(cid);
        if (!StringUtils.isEmpty(cid)) {//判断成员是否都在会议里面
            service.getServer().getClient(connectService.connections.get(to).getSessionId()).sendEvent(ConferenceApi.BASE,message);
        }
    }

    private void candidate(SocketIOClient client, Message message, AckRequest ackSende) {
        String cid = (String) message.data.get("cid");
        String to = (String) message.data.get("to");
        String from = (String) message.data.get("from");
        Conference conference = conferences.get(cid);
        if (!StringUtils.isEmpty(cid)) {//判断成员是否都在会议里面
            service.getServer().getClient(connectService.connections.get(to).getSessionId()).sendEvent(ConferenceApi.BASE,message);
        }
    }
    int i = 100;

    private Conference init(Conference conference, SocketIOClient client) {
        Conference c = conference;
        c.id = "" + (i++);
        c.state = State.create;
        c.created = System.currentTimeMillis();
        c.maxNumber = 5;
        c.founder = "founder";
        c.owner = "owner";
        c.type = com.fldys.mesh.conference.domain.Type.MESH;
        return c;
    }

    private Member init(SocketIOClient client) {
        Member m = new Member();
        m.id = UUID.randomUUID().toString();
        m.did = client.getSessionId().toString();
        m.uid = "uid";
        m.name = "name";
        return m;
    }
}
