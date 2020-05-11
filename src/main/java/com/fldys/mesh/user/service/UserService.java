package com.fldys.mesh.user.service;

import com.fldys.mesh.user.domain.User;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserService {
    @Autowired
    private MongoTemplate mongo;
    @Autowired
    private RedisTemplate<String, Object> template;
    @Value("${spring.mail.username}")
    private String username;
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private Gson gson;

//    @OnEvent(value = "user")
//    public void event(SocketIOClient client, Object data, AckRequest ackSender) {
//        com.fldys.mesh.socketio.Message message = gson.fromJson(data.toString(), com.fldys.mesh.socketio.Message.class);
//        ackSender.sendAckData(message);
//        log.info("UserService55555:" + message.toString());
//    }

    public User login(String mail, int code) {
        int c = Integer.valueOf(String.valueOf(template.opsForValue().get(mail)));
        if (c == code) {
            Query query = new Query();
            query.addCriteria(Criteria.where("mail").is(mail));
            User user = mongo.findOne(query, User.class);
            if (user != null) {
                return user;
            } else {
                user = new User();
                user.setEmail(mail);
                return mongo.save(user);
            }
        }
        return null;
    }

    public void update(User user) {
        mongo.save(user);
    }

    public void logout(String userId) {

    }

    public int code(String mail) {
        int code = 10000;
        ++code;
        template.opsForValue().set(mail, code);
        SimpleMailMessage message = new SimpleMailMessage();
        // 发件人邮箱
        message.setFrom(username);
        // 收信人邮箱
        message.setTo(mail);
        // 邮件主题
        message.setSubject("验证码");
        // 邮件内容
        message.setText(String.valueOf(code));
        mailSender.send(message);
        return code;
    }

//    public void handler(Message message) {
//        log.info("handler:" + message.toString());
//        switch (message.event) {
//            case UserApi.Code:
//                HashMap<String, Object> header = message.header;
//                int code = code((String) header.get("code"));
////                Device device = code((Device) message.data);
////                message.data = device;
////                message.type = Type.Ack;
//
////                connectService.router(Arrays.asList(device.deviceId), message);
//                log.info("handler:=>:send:" + code);
//                break;
//            case UserApi.Login:
//
////                connectService.router(Arrays.asList(device.deviceId), message);
//                break;
//            default:
//                break;
//        }
//    }


}
