package com.fornexttime.common.config;

import com.fornexttime.common.security.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    /**
     * 注入统一鉴权拦截器。
     */
    public WebMvcConfig(AuthInterceptor authInterceptor) {
        // 1. 保存拦截器以配置 MVC 调用链。
        this.authInterceptor = authInterceptor;
    }

    /**
     * 为业务 API 注册鉴权规则，并放行认证接口。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. 保护业务 API，注册和登录保持匿名可访问。
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns("/api/v1/auth/register", "/api/v1/auth/login");
    }
}
