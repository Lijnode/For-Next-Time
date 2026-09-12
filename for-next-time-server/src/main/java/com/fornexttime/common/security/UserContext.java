package com.fornexttime.common.security;

import com.fornexttime.common.exception.BusinessException;
import com.fornexttime.common.exception.ErrorCode;

public final class UserContext {

    private static final ThreadLocal<JwtUser> CURRENT_USER = new ThreadLocal<>();

    private UserContext() {
    }

    /**
     * 绑定当前请求已认证的用户。
     */
    public static void set(JwtUser user) {
        // 1. 将身份保存到当前处理线程。
        CURRENT_USER.set(user);
    }

    /**
     * 获取当前用户 ID；未认证时返回空值。
     */
    public static Long getUserId() {
        // 1. 读取线程上下文中的用户身份。
        JwtUser user = CURRENT_USER.get();
        return user == null ? null : user.userId();
    }

    /**
     * 获取当前用户名；未认证时返回空值。
     */
    public static String getUsername() {
        // 1. 读取线程上下文中的用户身份。
        JwtUser user = CURRENT_USER.get();
        return user == null ? null : user.username();
    }

    /**
     * 获取当前用户 ID，缺失身份时中断业务处理。
     */
    public static long requireUserId() {
        // 1. 读取已认证的用户 ID。
        Long userId = getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    /**
     * 清理当前线程绑定的用户身份。
     */
    public static void clear() {
        // 1. 移除线程变量，防止容器线程复用污染下个请求。
        CURRENT_USER.remove();
    }
}
