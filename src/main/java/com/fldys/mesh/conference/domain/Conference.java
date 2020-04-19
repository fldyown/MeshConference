package com.fldys.mesh.conference.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;

/**
 * 会议
 */
@Data
@NoArgsConstructor
public class Conference implements Serializable, Cloneable {
    public String id;                                       //唯一ID
    public String subject;                                  //主题
    public String content;                                  //内容
    public boolean security;                                //加密
    public String password;                                 //密码
    //    public Type type;                                 //类型
//    public State state;                                   //会议状态
    public String avatar;                                   //头像
    public String founder;                                  //创建者
    public String owner;                                    //拥有者
    public long created;                                    //创建时间
    public long started;                                    //开始时间
    public long duration;                                   //时长
    public long destroyed;                                  //销毁时间
    public long maxNumber = 5;                              //最大人数
    public HashMap<String, Member> inits;                   //初始成员
    public HashMap<String, Member> members;                 //会议成员
    public HashMap<String, Member> invites;                 //邀请人员
    public HashMap<String, Member> histories;               //历史数据
    public String extend;                                   //扩展字段

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
