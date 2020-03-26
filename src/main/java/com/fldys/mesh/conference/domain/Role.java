package com.fldys.mesh.conference.domain;

/**
 * 会议成员角色
 */
public enum Role {
    owner,                  //拥有者
    master,                 //管理员
    member,                 //成员
    guest;                  //陌生人
}