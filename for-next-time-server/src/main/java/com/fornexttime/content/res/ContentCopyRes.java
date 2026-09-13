package com.fornexttime.content.res;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 成功记录复制后返回的最新统计数据。
 */
public record ContentCopyRes(
        /** 更新后的成功复制次数。 */
        int copyCount,

        /** 本次复制后记录的最近成功复制时间，按统一时间协议序列化。 */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime lastCopyTime
) {
}
