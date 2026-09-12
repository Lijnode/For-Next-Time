package com.fornexttime.auth.res;

/**
 * 对外暴露的用户最小信息。
 */
public record UserSimpleRes(
        /** 用户主键。 */
        Long id,

        /** 用户登录名。 */
        String username
) {
}
