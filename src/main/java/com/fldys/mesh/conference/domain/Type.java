package com.fldys.mesh.conference.domain;

/**
 * 会议类型
 */
public enum Type {
    MESH(0),                   //点对点
    SFU(1),                    //中心转发
    MCU(2),                    //中心合流
    LIVE(3);                   //直播模式

    public int code;

    Type(int code) {
        this.code = code;
    }
}
