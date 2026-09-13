package com.fornexttime.user.res;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
/**
 * 当前已登录用户的个人资料响应。
 */
public record UserProfileRes(

        /** 当前用户主键。 */
        Long id,

        /** 当前用户登录名。 */
        String username,

        /** 已上传头像的公开访问地址；尚未上传时为 null。 */
        String avatarUrl,

        /** 当前用户尚未删除的内容数量。 */
        long contentCount,

        /** 账号创建时间，按统一时间协议序列化。 */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime createTime
) {}
