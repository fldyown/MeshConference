package com.fldys.mesh.protocol;

/**
 * 消息类型
 */
public enum Type {
    Req(0), Ack(1), Sync(2), Notify(3);
    public int code;
    Type(int code) {
        this.code = code;
    }
}
