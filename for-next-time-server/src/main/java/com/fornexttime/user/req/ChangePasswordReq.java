package com.fornexttime.user.req;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
/** 修改当前用户密码的请求参数。 */
public record ChangePasswordReq(
        /** 当前登录密码，仅用于服务端匹配验证。 */
        @NotBlank(message = "旧密码不能为空")
        String oldPassword,

        /** 新密码，长度限制为 8 至 64 个字符。 */
        @NotBlank(message = "新密码不能为空")
        @Size(min = 8, max = 64, message = "新密码长度应为8至64个字符")
        String newPassword,

        /** 新密码确认值，必须与新密码一致。 */
        @NotBlank(message = "确认密码不能为空")
        String confirmPassword
) {}
