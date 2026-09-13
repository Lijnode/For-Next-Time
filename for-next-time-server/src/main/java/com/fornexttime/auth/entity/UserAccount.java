package com.fornexttime.auth.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户账户数据库实体。
 */
@Data
public class UserAccount {

    /** 用户主键。 */
    private Long id;

    /** 用户登录名。 */
    private String username;

    /** BCrypt 加密后的密码摘要，不可对外返回。 */
    private String passwordHash;

    /** 头像在文件存储根目录下的相对路径，可为空。 */
    private String avatarPath;

    /** 账号创建时间。 */
    private LocalDateTime createTime;
}
