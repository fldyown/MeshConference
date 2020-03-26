package com.fldys.mesh.device.service;

import com.fldys.mesh.connection.ConnectService;
import com.fldys.mesh.device.domain.Device;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.*;

@Slf4j
@Service
public class DeviceService {
    public final static String NAME = "device";
    @Autowired
    private MongoTemplate mongo;
    @Autowired
    private ConnectService connectService;
    @Autowired
    private RedisTemplate<String, Serializable> template;

    public Device register(Device device) {
        log.info("DeviceService:" + "register\n" + device.toString());
        return mongo.save(device);
    }

    public boolean exists(String deviceId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("deviceId").is(deviceId));
        return mongo.exists(query, Device.class);
    }

    public void unregister(String deviceId) {

    }

    public void login(Device device) {

    }

    public void logout() {
    }

    public Device find(String deviceId) {
        return mongo.findById(deviceId, Device.class);
    }

//    public void handler(Message message) {
//        log.info("handler:" + message.toString());
//        switch (message.event) {
//            case DeviceApi.Register:
//                Device device = register((Device) message.data);
//                message.data = device;
//                message.type = Type.Ack;
//
//                connectService.router(Arrays.asList(device.deviceId), message);
//                log.info("handler:=>:" + device);
//                break;
//            case DeviceApi.Unregister:
//                break;
//            default:
//                break;
//        }
//    }
}
