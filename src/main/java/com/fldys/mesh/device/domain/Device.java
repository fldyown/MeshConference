package com.fldys.mesh.device.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Device {
    public String did;          //设备ID
    public String sid;          //会话ID
    public String uid;          //用户ID
    public String name;         //设备名称
    public String plat;         //设备平台
    public String brand;        //品牌
    public String model;        //型号
    public String version;      //版本
    public long create;         //创建时间
}
