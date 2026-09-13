package com.fornexttime.content.res;

import java.time.LocalDateTime;

/**
 * 成功记录复制后返回的最新统计数据。
 */
public record ContentCopyRes(
        /** 更新后的成功复制次数。 */
        int copyCount,

        /** 本次复制后记录的最近成功复制时间。 */
        LocalDateTime lastCopyTime
) {
}
