package com.fldys.mesh.mqtt;

import com.fldys.mesh.device.service.DeviceApi;
import com.fldys.mesh.device.service.DeviceService;
import com.fldys.mesh.user.service.UserApi;
import com.fldys.mesh.user.service.UserService;
import com.google.gson.Gson;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.Charset;

@Slf4j
//@SERVICE
//@ChannelHandler.Sharable
public class HandlerService extends SimpleChannelInboundHandler<MqttMessage> {
    @Autowired
    Gson gson;
    @Autowired
    private UserService userService;
    @Autowired
    private DeviceService deviceService;
//    @Autowired
    private ConnectService connectService;


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) throws Exception {
        if (msg.decoderResult().isFailure()) {
            Throwable cause = msg.decoderResult().cause();
            log.error("Connection Failed:" + cause.toString());
            if (cause instanceof MqttUnacceptableProtocolVersionException) {
                ctx.writeAndFlush(MqttMessageFactory.newMessage(
                        new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                        new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION, false),
                        null));
            } else if (cause instanceof MqttIdentifierRejectedException) {
                ctx.writeAndFlush(MqttMessageFactory.newMessage(
                        new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                        new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED, false),
                        null));
            }
            ctx.close();
            log.error("Connection Failed:" + cause.toString());
            return;
        }

        switch (msg.fixedHeader().messageType()) {
            case CONNECT:
                log.info("HandlerService CONNECT:" + msg);
                connectService.connect(ctx.channel(), (MqttConnectMessage) msg);
                break;
            case CONNACK:
                log.info("HandlerService CONNACK");
                break;

            case PUBLISH:
                handler(ctx, (MqttPublishMessage) msg);   //核心处理消息模块
                break;
            case PUBACK:
                log.info("HandlerService PUBACK");
                break;
            case PUBREC:
                log.info("HandlerService PUBREC");
                break;
            case PUBREL:
                log.info("HandlerService PUBREL");
                break;
            case PUBCOMP:
                log.info("HandlerService PUBCOMP");
                break;
            case SUBSCRIBE:
                log.info("HandlerService SUBSCRIBE");
//                connectService.subscribe(ctx.channel(), (MqttSubscribeMessage) msg);
                break;
            case SUBACK:
                log.info("HandlerService SUBACK");
                break;
            case UNSUBSCRIBE:
                log.info("HandlerService UNSUBSCRIBE");
//                connectService.unsubscribe(ctx.channel(), (MqttUnsubscribeMessage) msg);
                break;
            case UNSUBACK:
                log.info("HandlerService UNSUBACK");
                break;
            case PINGREQ:
                log.info("HandlerService PINGREQ");
                connectService.ping(ctx.channel(), msg);
                break;
            case PINGRESP:
                log.info("HandlerService PINGRESP");
                break;
            case DISCONNECT:
                connectService.disconnect(ctx.channel(), msg);
                log.info("HandlerService DISCONNECT");
                break;
            default:
                break;
        }
    }

    /**
     * 分发消息到指定服务
     *
     * @param ctx
     * @param msg
     */
    private void handler(ChannelHandlerContext ctx, MqttPublishMessage msg) {
        String service = msg.variableHeader().topicName();
        if (StringUtils.isEmpty(service)) {
            log.error("HandlerService:" + "\n" + service + "is null\n" + msg);
            return;
        }
        MqttQoS qos = msg.fixedHeader().qosLevel();
        int packageId = msg.variableHeader().packetId();
        log.info("HandlerService:" + "\n" + service + "\n" + qos + "\n" + msg + "\n" + packageId);

        log.info("HandlerService:" + "data" + msg.payload().toString(Charset.forName("UTF-8")));
        switch (service) {
            case DeviceApi.REGISTER:
            case DeviceApi.UNREGISTER:
//                deviceService.handler(gson.fromJson(msg.payload().toString(Charset.forName("UTF-8")), new TypeToken<Message<Device>>() {
//                }.getType()));
                break;
            case UserApi.Code:
            case UserApi.Login:
//                userService.handler(gson.fromJson(msg.payload().toString(Charset.forName("UTF-8")), Message.class));
                break;
            default:
                log.info("HandlerService:Unknown service");
                break;
        }
    }

//    private void ack(Channel channel, int packageId) {
//        log.info("ack:" + packageId);
//        MqttPubAckMessage pubAckMessage = (MqttPubAckMessage) MqttMessageFactory.newMessage(
//                new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
//                MqttMessageIdVariableHeader.from(packageId),
//                null);
//        channel.writeAndFlush(pubAckMessage);
//    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("error:" + cause.getMessage());
        if (cause instanceof IOException) {
            String deviceId = (String) ctx.channel().attr(AttributeKey.valueOf("deviceId")).get();
            Channel c = connectService.removeChannel(deviceId);
            c.close();// 远程主机强迫关闭了一个现有的连接的异常
            log.error("exceptionCaught:" + deviceId + " =>" + cause.getMessage());
        } else {
            super.exceptionCaught(ctx, cause);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        log.error("error:" + evt.toString());
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            if (idleStateEvent.state() == IdleState.ALL_IDLE) {
                String deviceId = (String) ctx.channel().attr(AttributeKey.valueOf("deviceId")).get();
                Channel c = connectService.removeChannel(deviceId);
                c.close();// 远程主机强迫关闭了一个现有的连接的异常
                log.error("userEventTriggered:" + deviceId + " =>" + evt.toString());
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
