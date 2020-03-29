package com.fldys.mesh.mqtt;

import com.fldys.mesh.device.service.DeviceService;
import com.google.gson.Gson;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;


@Slf4j
//@SERVICE
public class ConnectService {
    private final int MAX_IDLE_TIME = 200;//SECOND
    private final int DEFAULT_IDLE_TIME = 120;//SECOND
    @Autowired
    private Gson gson;
    @Autowired
    private SessionService sessionService;
    @Autowired
    private DeviceService deviceService;
    private HashMap<String, Channel> connections = new HashMap<>();

    public synchronized void connect(Channel channel, MqttConnectMessage msg) {
        String deviceId = msg.payload().clientIdentifier();
        log.info("connect:" + deviceId);
        if (StringUtils.isEmpty(deviceId)) {
            MqttConnAckMessage connAckMessage = (MqttConnAckMessage) MqttMessageFactory.newMessage(
                    new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                    new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED, false), null);
            channel.writeAndFlush(connAckMessage);
            channel.close();
            log.error("ConnectService null deviceId : " + deviceId);
            return;
        }
        int time = Integer.valueOf(msg.payload().userName());
        log.error("ConnectService time : " + time + " deviceId:" + deviceId);
        if (time <= 0) {
            MqttConnAckMessage connAckMessage = (MqttConnAckMessage) MqttMessageFactory.newMessage(
                    new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                    new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED, false), null);
            channel.writeAndFlush(connAckMessage);
            channel.close();
            log.error("ConnectService no deviceId : exist ");
            return;
        }

        if (connections.keySet().contains(deviceId)) {
            Channel c = connections.remove(deviceId);
            if (c != null) {
                c.close();
            }
        }

        if (msg.variableHeader().keepAliveTimeSeconds() > 0) {
            if (channel.pipeline().names().contains("idle")) {
                channel.pipeline().remove("idle");
            }
            channel.pipeline().addFirst("idle", new IdleStateHandler(0, 0, Math.round((msg.variableHeader().keepAliveTimeSeconds() > MAX_IDLE_TIME ? MAX_IDLE_TIME : msg.variableHeader().keepAliveTimeSeconds()) * 1.5f)));
        } else {
            channel.pipeline().addFirst("idle", new IdleStateHandler(0, 0, DEFAULT_IDLE_TIME));
        }

        connections.put(deviceId, channel);

        channel.attr(AttributeKey.valueOf("deviceId")).set(deviceId);
        Boolean sessionPresent = !msg.variableHeader().isCleanSession();
        MqttConnAckMessage ack = (MqttConnAckMessage) MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_ACCEPTED, sessionPresent),
                null
        );
        channel.writeAndFlush(ack);
    }

    public void disconnect(Channel channel, MqttMessage msg) {
        String deviceId = (String) channel.attr(AttributeKey.valueOf("deviceId")).get();
        sessionService.removeConnections(deviceId);
        sessionService.removeRouter(deviceId);
        channel.close();
        log.info("disconnect:" + deviceId);
    }

    public void ping(Channel channel, MqttMessage msg) {
        MqttMessage pingRespMessage = MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0),
                null,
                null);
        log.info("PINGREQ - clientId: {}", (String) channel.attr(AttributeKey.valueOf("deviceId")).get());
        channel.writeAndFlush(pingRespMessage);
    }

    public Channel getChannel(String deviceId) {
        return connections.get(deviceId);
    }

    public Channel removeChannel(String deviceId) {
        return connections.remove(deviceId);
    }

//    public void router(List<String> deviceIds, Message message) {
//        MqttPublishMessage mpm = MqttMessageBuilders.publish()
//                .topicName(message.event)
//                .retained(false)
//                .qos(MqttQoS.AT_MOST_ONCE)
//                .payload(Unpooled.copiedBuffer(gson.toJson(message).getBytes(Charset.forName("UTF-8")))).build();
//
//        for (String id : deviceIds) {
//            connections.get(id).writeAndFlush(mpm);
//        }
//    }

//
//    public void subscribe(Channel channel, MqttSubscribeMessage msg) {
//        List<MqttTopicSubscription> topicSubscriptions = msg.payload().topicSubscriptions();
//        if (this.validTopicFilter(topicSubscriptions)) {
//            String clientId = (String) channel.attr(AttributeKey.valueOf("clientId")).get();
//            List<Integer> mqttQoSList = new ArrayList<Integer>();
//            for (MqttTopicSubscription mqttTopicSubscription : topicSubscriptions) {
//                String topicFilter = mqttTopicSubscription.topicName();
//                MqttQoS mqttQoS = mqttTopicSubscription.qualityOfService();
//                MqttSubAckMessage subAckMessage = (MqttSubAckMessage) MqttMessageFactory.newMessage(
//                        new MqttFixedHeader(MqttMessageType.SUBACK, false, mqttQoS, false, 0),
//                        MqttMessageIdVariableHeader.from(msg.variableHeader().messageId()),
//                        new MqttSubAckPayload(mqttQoSList));
//                channel.writeAndFlush(subAckMessage);
//                this.sendRetainMessage(channel, topicFilter, mqttQoS);
//                mqttQoSList.add(mqttQoS.value());
//                log.info("SUBSCRIBE - clientId: {}, topFilter: {}, QoS: {}", clientId, topicFilter, mqttQoS.value());
//            }
//        } else {
//            channel.close();
//        }
//    }
//
//
//    public void unsubscribe(Channel channel, MqttUnsubscribeMessage msg) {
//        List<String> topicFilters = msg.payload().topics();
//        String clientId = (String) channel.attr(AttributeKey.valueOf("clientId")).get();
//        topicFilters.forEach(topicFilter -> {
//            log.info("UNSUBSCRIBE - clientId: {}, topicFilter: {}", clientId, topicFilter);
//        });
//        MqttUnsubAckMessage unsubAckMessage = (MqttUnsubAckMessage) MqttMessageFactory.newMessage(
//                new MqttFixedHeader(MqttMessageType.UNSUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
//                MqttMessageIdVariableHeader.from(msg.variableHeader().messageId()),
//                null);
//        channel.writeAndFlush(unsubAckMessage);
//    }
}
