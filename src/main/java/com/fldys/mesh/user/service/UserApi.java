package com.fldys.mesh.user.service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class UserApi {
    public static final String Login = "user/login";                      //登录
    public static final String Code = "user/code";                        //登出

    private static List<String> events = new ArrayList<>();

    public static boolean isExists(String api) {
        if (null == api) {
            return false;
        }
        if (events.size() <= 0) {
            try {
                for (Field f : UserApi.class.getDeclaredFields()) {
                    f.setAccessible(true);
                    String e = (String) f.get(UserApi.class);
                    if (!events.contains(e)) {
                        events.add(e);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return events.contains(api);
    }
}
