package com.fornexttime.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    /**
     * 提供全局复用的 BCrypt 密码编码器。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // 返回 Spring Security 标准 BCrypt 实现。
        return new BCryptPasswordEncoder();
    }
}
