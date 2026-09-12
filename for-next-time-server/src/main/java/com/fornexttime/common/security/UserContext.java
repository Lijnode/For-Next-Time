package com.fornexttime.common.security;

import com.fornexttime.common.exception.BusinessException;
import com.fornexttime.common.exception.ErrorCode;

public final class UserContext {

    private static final ThreadLocal<JwtUser> CURRENT_USER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(JwtUser user) {
        CURRENT_USER.set(user);
    }

    public static Long getUserId() {
        JwtUser user = CURRENT_USER.get();
        return user == null ? null : user.userId();
    }

    public static String getUsername() {
        JwtUser user = CURRENT_USER.get();
        return user == null ? null : user.username();
    }

    public static long requireUserId() {
        Long userId = getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
