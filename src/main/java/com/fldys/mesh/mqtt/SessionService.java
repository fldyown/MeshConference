package com.fldys.mesh.mqtt;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.security.Key;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
//@Service
public class SessionService {
    //hash//clientId->username
    public static final String USER_ONLINE_KEY = "user_online_key";
    //hash//ip->clientId->connection
    public static final String USER_ROUTER_KEY = "user_router_key";
//    //索引保存
//    public static final String NAME_TO_CLIENT_ID = "name_to_clientId";
//    public static final String CLIENT_ID_TO_NAME = "clientId_to_name";

    public static final String USER_JWT_KEY = "user_jwt_key";

    public static final String SERVICES = "services";

    public static final String CONFERENCES = "conferences";


    /**
     * 本地连接数据
     */
    private HashMap<String, Channel> connections = new HashMap<>();
    @Autowired
    RedisTemplate<String, Serializable> template;

    /**
     * 添加路由信息
     *
     * @param username
     * @param clientId
     * @param server(Ip:Port)
     */
    public void addRouter(String username, String clientId, String server) {
        template.opsForHash().put(username, clientId, server);
        template.opsForHash().put(USER_ONLINE_KEY, clientId, username);
    }

    /**
     * 删除路由信息
     *
     * @param clientId
     */
    public void removeRouter(String clientId) {
        template.opsForHash().delete((String) template.opsForHash().get(USER_ONLINE_KEY, clientId), clientId);
        template.opsForHash().delete(USER_ONLINE_KEY, clientId);
    }

    /**
     * 获取用户名
     *
     * @param clientId
     * @return
     */
    public String getUsernameByClientId(String clientId) {
        return (String) template.opsForHash().get(USER_ONLINE_KEY, clientId);
    }

    /**
     * 获取设备登录信息
     *
     * @param username
     * @return
     */
    public Map<String, String> getClientIdAndServerByUsername(String username) {
        return template.opsForHash().entries(username).entrySet().stream()
                .collect(Collectors.toMap(k -> (String) k.getValue(), v -> (String) v.getValue()));
    }

    /**
     * 获取客户端ID
     *
     * @param username
     * @return
     */
    public Set<String> getClientIdsByUsername(String username) {
        return template.opsForHash().entries(username).keySet().stream().map(e -> (String) e).collect(Collectors.toSet());
    }

    /**
     * 获取路由信息
     *
     * @param clientId
     * @return
     */
    public String getRouterByClientId(String clientId) {
        return (String) template.opsForHash().get((String) template.opsForHash().get(USER_ONLINE_KEY, clientId), clientId);
    }

    /**
     * 是否存在
     *
     * @param clientId
     * @return
     */
    public boolean existClientId(String clientId) {
        return template.opsForHash().hasKey(USER_ONLINE_KEY, clientId);
    }

    /**
     * 初始化用户密钥
     *
     * @param userId
     * @param key
     */
    public void setIfAbsentUserJwtKey(String userId, Key key) {
        template.opsForHash().putIfAbsent(USER_JWT_KEY, userId, key);
    }

    /**
     * 设置用户密钥
     *
     * @param userId
     * @param key
     */
    public void setUserJwtKey(String userId, Key key) {
        template.opsForHash().put(USER_JWT_KEY, userId, key);
    }

    /**
     * 获取用户密钥
     *
     * @param userId
     * @return
     */
    public Key getUserJwtKey(String userId) {
        return (Key) template.opsForHash().get(USER_JWT_KEY, userId);
    }

    /**
     * 添加连接数
     */
    public void addConnections(String clientId, Channel channel) {
        connections.put(clientId, channel);
    }

    /**
     * 删除连接数
     */
    public void removeConnections(String clientId) {
        connections.remove(clientId);
    }

    /**
     * 获取连接
     */
    public Channel getConnections(String clientId) {
        return connections.get(clientId);
    }

    /**
     * 服务注册
     *
     * @param service
     */
    public void registerService(String service, String server) {
        template.opsForHash().put(SERVICES, service, server);
    }

    /**
     * 服务是否存在
     *
     * @param service
     * @return
     */
    public boolean containService(String service) {
        return template.opsForHash().hasKey(SERVICES, service);
    }

    /**
     * 获取服务
     */
    public Set<String> getAllRegisterServiceKey() {
        return template.opsForHash().entries(SERVICES).keySet().stream().map(e -> (String) e).collect(Collectors.toSet());
    }

    /**
     * 服务注销
     *
     * @param service
     */
    public void unregisterService(String service) {
        template.opsForHash().delete(SERVICES, service);
    }

    /**
     * 获取服务地址
     *
     * @param service
     */
    public String getServerByService(String service) {
        return (String) template.opsForHash().get(SERVICES, service);
    }

//    /**
//     * 缓存会议数据
//     *
//     * @param conference
//     */
//    public void updateConference(Conference conference) {
//        if (conference.getId() != null) {
//            template.opsForHash().put(CONFERENCES, conference.getId(), conference);
//        } else {
//            log.info("updateConference:id is null");
//        }
//    }
//
//    /**
//     * 获取会议数据
//     *
//     * @param id
//     */
//    public Conference getConference(String id) {
//        return (Conference) template.opsForHash().get(CONFERENCES, id);
//    }
}