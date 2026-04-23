package com.anbuz.trigger.auth;

/**
 * 用户上下文，负责在一次请求链路中保存和读取当前登录用户 ID。
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    public static void set(Long userId) {
        USER_ID.set(userId);
    }

    public static Long currentUserId() {
        Long id = USER_ID.get();
        if (id == null) throw new IllegalStateException("未登录");
        return id;
    }

    public static void clear() {
        USER_ID.remove();
    }

}
