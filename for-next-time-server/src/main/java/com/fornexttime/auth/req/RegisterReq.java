package com.fornexttime.auth.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户注册请求参数。
 */
public record RegisterReq(

    /** 注册用户名。 */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度应为3至50个字符")
    String username,

    /** 原始密码，仅用于生成密码摘要。 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度应为8至64个字符")
    String password,

    /** 用于校验用户两次输入是否一致的确认密码。 */
    @NotBlank(message = "确认密码不能为空")
    String confirmPassword

) {
}
