package com.fldys.mesh.protocol;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
public class Message implements Serializable {
    public String api;
    public int type = Type.Req.code;
    public Map<String, Object> data = new HashMap<>();
    public int code = 200;
    public String desc = "OK";

    @Override
    public String toString() {
        return "Message{" +
                "api='" + api + '\'' +
                ", type=" + type +
                ", data=" + data +
                ", code=" + code +
                ", desc='" + desc + '\'' +
                '}';
    }
}