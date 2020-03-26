package com.fldys.mesh.socketio;

import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.annotation.SpringAnnotationScanner;
import com.corundumstudio.socketio.listener.ExceptionListener;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


@Slf4j
@org.springframework.context.annotation.Configuration
public class SocketConfig {
    @Value("${socket.host}")
    private String host;
    @Value("${socket.port}")
    private int port;
    @Value("${socket.length}")
    private int length;
    @Value("${socket.boss}")
    private int boss;
    @Value("${socket.work}")
    private int work;
    @Value("${socket.custom}")
    private boolean custom;
    @Value("${socket.upgrade}")
    private int upgrade;
    @Value("${socket.timeout}")
    private int timeout;
    @Value("${socket.ping}")
    private int ping;

    @Bean
    public SocketIOServer socketIOServer() {
        Logger.getGlobal().setLevel(Level.ALL);
        Configuration configuration = new Configuration();
        com.corundumstudio.socketio.SocketConfig socketConfig = new com.corundumstudio.socketio.SocketConfig();
        socketConfig.setReuseAddress(true);
        socketConfig.setTcpNoDelay(true);
        socketConfig.setSoLinger(0);
        configuration.setSocketConfig(socketConfig);
        configuration.setTransports(Transport.WEBSOCKET);

//        configuration.setKeyStorePassword("test1234");
//        InputStream stream = new FileInputStream(new File("/home/soft/keystore.jks"));
        log.info("host:" + host);
//        configuration.setHostname(host);
        configuration.setPort(port);
        configuration.setOrigin("http://192.168.31.30:7777");
        configuration.setExceptionListener(new ExceptionListener() {
            @Override
            public void onEventException(Exception e, List<Object> args, SocketIOClient client) {
                log.info("onEventException1:" + e.getMessage());
            }

            @Override
            public void onDisconnectException(Exception e, SocketIOClient client) {
                log.info("onEventException2:" + e.getMessage());
            }

            @Override
            public void onConnectException(Exception e, SocketIOClient client) {
                log.info("onEventException3:" + e.getMessage());
            }

            @Override
            public void onPingException(Exception e, SocketIOClient client) {
                log.info("onEventException4:" + e.getMessage());
            }

            @Override
            public boolean exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
                log.info("onEventException5:" + e.getMessage());
                return true;
            }
        });

        return new SocketIOServer(configuration);
    }

    /**
     * 用于扫描netty-socketio的注解，比如 @OnConnect、@OnEvent
     */
    @Bean
    public SpringAnnotationScanner springAnnotationScanner() {
        return new SpringAnnotationScanner(socketIOServer());
    }
}
