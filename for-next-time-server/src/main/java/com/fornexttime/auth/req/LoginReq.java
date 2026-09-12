package com.fornexttime.auth.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户登录请求参数。
 */
public record LoginReq(
        /** 登录用户名。 */
        @NotBlank(message = "用户名不能为空")
        @Size(max = 50, message = "用户名长度不能超过50个字符")
        String username,

        /** 登录密码，仅用于认证，不会写入日志。 */
        @NotBlank(message = "密码不能为空")
        String password
) {
}
