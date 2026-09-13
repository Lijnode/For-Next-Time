package com.fornexttime.user.res;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 校验个人资料中的加入日期不暴露具体时分秒。
 */
class UserProfileResSerializationTest {

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();

    /**
     * 资料响应应将账号创建时间序列化为日期。
     */
    @Test
    void serializesJoinTimeAsDateOnly() throws Exception {
        // 1. 构造包含具体时分秒的账号创建时间。
        UserProfileRes response = new UserProfileRes(1L, "admin", null, 0,
                LocalDateTime.of(2026, 9, 13, 21, 49, 24));

        // 2. 使用与 Web 响应一致的 Jackson 时间模块执行序列化。
        String json = objectMapper.writeValueAsString(response);

        // 3. 断言加入时间仅保留用户需要的日期部分。
        assertThat(json).contains("\"createTime\":\"2026-09-13\"")
                .doesNotContain("21:49:24");
    }
}
