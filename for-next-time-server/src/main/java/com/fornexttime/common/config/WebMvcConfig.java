package com.fornexttime.common.config;

import com.fornexttime.common.security.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import jakarta.annotation.Resource;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /** 业务 API 鉴权拦截器。 */
    @Resource
    private AuthInterceptor authInterceptor;

    /**
     * 为业务 API 注册鉴权规则，并放行认证接口。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 保护业务 API，注册和登录保持匿名可访问。
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns("/api/v1/auth/register", "/api/v1/auth/login");
    }
}
