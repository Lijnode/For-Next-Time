package com.fornexttime.auth.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterReq(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 50, message = "用户名长度应为3至50个字符") String username,
        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度应为8至64个字符") String password,
        @NotBlank(message = "确认密码不能为空") String confirmPassword
) {
}
