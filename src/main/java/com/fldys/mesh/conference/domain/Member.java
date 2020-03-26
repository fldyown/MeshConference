package com.fldys.mesh.conference.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * 会议成员
 */
@Data
@NoArgsConstructor
public class Member implements Serializable, Cloneable {
    public String id;                              //成员ID
    public String uid;                             //用户ID
    public String name;                            //名字
    public String avatar;                          //头像
    public String did;                             //设备ID
    public String phone;                           //手机
    public String email;                           //邮箱
    public String extend;                          //扩展字段
    public Role role = Role.member;                //角色

    public long invite;                            //邀请时间
    public long join;                              //加入时间
    public long quit;                              //退出时间

//    private List<Stream> streams;                //推流数据

    public boolean speak = true;                   //是否可以说
    public boolean hear = true;                    //是否可以听
    public boolean watch = true;                   //是否可以看
    public boolean video = false;                  //是否开启视频
    public boolean audio = true;                   //是否开启音频

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Member member = (Member) o;
        return Objects.equals(did, member.did);
    }

    @Override
    public int hashCode() {
        return Objects.hash(did);
    }
}