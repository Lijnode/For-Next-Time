package com.fornexttime.auth.res;

/**
 * 登录成功后返回的认证信息。
 */
public record LoginRes(

    /** 用于后续 Bearer 认证的 JWT。 */
    String token,

    /** Token 类型，固定为 Bearer。 */
    String tokenType,

    /** Token 剩余有效秒数。 */
    long expiresIn,

    /** 当前登录用户的公开基础信息。 */
    UserSimpleRes user

) {
}
