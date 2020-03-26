package com.fldys.mesh.mqtt;

import com.fldys.mesh.mqtt.util.ByteBufToWebSocketFrameEncoder;
import com.fldys.mesh.mqtt.util.WebSocketFrameToByteBufDecoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.util.ResourceLeakDetector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Slf4j
//@Service
public class ServiceService {
    public static String TCP_SERVER;
    public static String WS_SERVER;

    @Value("${mqtt.host}")
    private String host;
    @Value("${mqtt.port}")
    private Integer port;
    @Value("${mqtt.ws_port}")
    private Integer wsPort;
    @Value("${mqtt.leak_level}")
    private String leakLevel;
    @Value("${mqtt.boss_count}")
    private Integer bossCount;
    @Value("${mqtt.worker_count}")
    private Integer workerCount;
    @Value("${mqtt.max_payload_size}")
    private Integer maxPayloadSize;

    private static final String MQTT_CSV_LIST = "mqtt, mqttv3.1, mqttv3.1.1";

    @Autowired
    private HandlerService handlerService;
    @Autowired
    private SessionService sessionService;

    private Channel service;
    private EventLoopGroup boss;
    private EventLoopGroup worker;

//    @PostConstruct
    public void created() throws Exception {
        log.info("Setting resource leak detector level to {}", leakLevel);
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.valueOf(leakLevel.toUpperCase()));
        log.info("Starting ServiceService");
        boss = new NioEventLoopGroup(bossCount);
        worker = new NioEventLoopGroup(workerCount);

        initTcp();
        initWs();
//        initService();
////        initSelfKey(service);
//        log.info("Started ServiceService at " + SERVERSERVER);
    }

    private void initTcp() throws Exception {
        ServerBootstrap sb = new ServerBootstrap();
        sb.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        pipeline.addLast("decoder", new MqttDecoder(maxPayloadSize));
                        pipeline.addLast("encoder", MqttEncoder.INSTANCE);
                        pipeline.addLast("handler", handlerService);
                    }
                });
        ChannelFuture f = sb.bind(host, port);
        f.sync().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    final SocketAddress localAddress = future.channel().localAddress();
//                    if (localAddress instanceof InetSocketAddress) {
                    InetSocketAddress isa = (InetSocketAddress) localAddress;
                    TCP_SERVER = isa.getHostName() + ":" + isa.getPort();
                    log.info("ChannelFutureListener tcp:" + TCP_SERVER);
//                    }
                    initTcpService();
                } else {
                    log.error("TCP 服务启动失败");
                }
            }
        });
    }

    private void initWs() throws Exception {
        ServerBootstrap sb = new ServerBootstrap();
        sb.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
//                        //ws
                        pipeline.addLast("httpCode", new HttpServerCodec());
                        pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
                        pipeline.addLast("webSocketHandler",
                                new WebSocketServerProtocolHandler("/mqtt", MQTT_CSV_LIST));
                        pipeline.addLast("wsDecoder", new WebSocketFrameToByteBufDecoder());
                        pipeline.addLast("wsEncoder", new ByteBufToWebSocketFrameEncoder());
                        pipeline.addLast("decoder", new MqttDecoder(maxPayloadSize));
                        pipeline.addLast("encoder", MqttEncoder.INSTANCE);
                        //pipeline.addLast("idleStateHandler", new IdleStateHandler(10,2,12, TimeUnit.SECONDS));
//                        RouterService handler = new RouterService();
                        pipeline.addLast("handler", handlerService);
                    }
                });
        ChannelFuture f = sb.bind(host, wsPort);
        f.sync().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    final SocketAddress localAddress = future.channel().localAddress();
//                    if (localAddress instanceof InetSocketAddress) {
                    InetSocketAddress isa = (InetSocketAddress) localAddress;
                    WS_SERVER = isa.getHostName() + ":" + isa.getPort();
                    log.info("ChannelFutureListener ws:" + WS_SERVER);
//                    }
                    initWsService();
                } else {
                    log.error("WS 服务启动失败");
                }
            }
        });
    }

    private void initTcpService() {
        sessionService.registerService("conference", TCP_SERVER);
    }

    private void initWsService() {
        sessionService.registerService("conference", WS_SERVER);
    }

//    @PreDestroy
    public void destroyed() throws Exception {
        log.info("Stopping ServiceService");
        sessionService.unregisterService("conference");
        if (worker == null || boss == null) {
            log.error("Netty acceptor is not initialized");
            throw new IllegalStateException("Invoked close on an Acceptor that wasn't initialized");
        }
        Future<?> workerWaiter = worker.shutdownGracefully();
        Future<?> bossWaiter = boss.shutdownGracefully();

        try {
            workerWaiter.wait(1 * 1000);
            bossWaiter.wait(1 * 1000);
        } catch (InterruptedException iex) {
            log.warn("An InterruptedException was caught while waiting for event loops to terminate...");
        }

        if (!worker.isTerminated()) {
            log.warn("Forcing shutdown of worker event loop...");
            worker.shutdownGracefully(0L, 0L, TimeUnit.MILLISECONDS);
        }

        if (!boss.isTerminated()) {
            log.warn("Forcing shutdown of boss event loop...");
            boss.shutdownGracefully(0L, 0L, TimeUnit.MILLISECONDS);
        }
        log.info("Stopped ServiceService");
    }
}
