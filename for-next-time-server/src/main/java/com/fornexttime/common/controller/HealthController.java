package com.fornexttime.common.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    /**
     * 返回服务存活状态，供部署环境健康探测使用。
     */
    public Map<String, String> health() {
        // 1. 健康检查不使用业务响应包装，返回固定状态。
        return Map.of("status", "UP");
    }
}
