package com.fldys.mesh.user.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class User {
    public String uid;          //用户ID
    public String name;         //名称
    public String avatar;       //头像
    public String phone;        //手机
    public String email;        //邮箱
    public long created;        //创建
    public long update;         //更新
}
