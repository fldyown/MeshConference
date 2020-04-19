package com.fldys.mesh.socketio;

import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.annotation.SpringAnnotationScanner;
import com.corundumstudio.socketio.listener.ExceptionListener;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.Base64;
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
        configuration.setTransports(Transport.WEBSOCKET, Transport.POLLING);
        configuration.setPort(port);
//        configuration.setOrigin("http://192.168.31.30:7777");
//        try {
//            InputStream stream = new FileInputStream("/Users/tinchy/workspase/mesh/mesh.jks");
//            configuration.setKeyStore(stream);
//        } catch (Exception e){}
//        configuration.setKeyStorePassword("mesh123");
//        configuration.setPingInterval(ping);//采用默认值ping 25
//        configuration.setUpgradeTimeout(upgrade);
        configuration.setPingTimeout(timeout);
        configuration.setAllowCustomRequests(custom);
        configuration.setBossThreads(boss);
        configuration.setWorkerThreads(work);
        configuration.setMaxHttpContentLength(length);
        configuration.setMaxFramePayloadLength(length);
        configuration.setAuthorizationListener(new AuthorizationListener() {
            @Override
            public boolean isAuthorized(HandshakeData data) {
                try {
                    String did = data.getSingleUrlParam("did");
                    log.info("did:" + did);
//                    String d = new String(Base64.getDecoder().decode(URLDecoder.decode(did)), "UTF-8");
                    String d = URLDecoder.decode(did);
                    if (d.length() < 36) {
                        return false;
                    }
                    return true;
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
                return false;
            }
        });
        return new SocketIOServer(configuration);
    }

    /**
     * 用于扫描nettysocketio的注解，比如 @OnConnect、@OnEvent
     */
    @Bean
    public SpringAnnotationScanner springAnnotationScanner() {
        return new SpringAnnotationScanner(socketIOServer());
    }
}
