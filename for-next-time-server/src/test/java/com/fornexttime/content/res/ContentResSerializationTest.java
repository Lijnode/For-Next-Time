package com.fornexttime.content.res;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 校验内容相关响应的时间字段遵循统一 API 时间协议。
 */
class ContentResSerializationTest {

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();

    /**
     * 内容列表、详情和复制统计均应序列化为以空格分隔的本地时间。
     */
    @Test
    void serializesContentTimesWithoutIsoSeparator() throws Exception {
        // 1. 准备所有内容响应共用的固定时间样例。
        LocalDateTime time = LocalDateTime.of(2026, 9, 13, 21, 49, 24);

        // 2. 序列化三类会暴露内容时间的响应 DTO。
        String listJson = objectMapper.writeValueAsString(new ContentListRes(1L, "下次一定", null,
                null, false, 0, time, time, time, List.of()));
        String detailJson = objectMapper.writeValueAsString(new ContentDetailRes(1L, "下次一定", null,
                null, List.of(), null, false, 0, time, time, time));
        String copyJson = objectMapper.writeValueAsString(new ContentCopyRes(1, time));

        // 3. 断言统一时间协议使用空格而不是 ISO 8601 的 T 分隔符。
        assertThat(listJson).contains("2026-09-13 21:49:24").doesNotContain("T21:49:24");
        assertThat(detailJson).contains("2026-09-13 21:49:24").doesNotContain("T21:49:24");
        assertThat(copyJson).contains("2026-09-13 21:49:24").doesNotContain("T21:49:24");
    }
}
