package com.fornexttime.common.config;

import com.fornexttime.common.security.AuthInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /** 业务 API 鉴权拦截器。 */
    @Resource
    private AuthInterceptor authInterceptor;

    /** 文件存储根目录，开发环境指向项目根目录下的 files。 */
    @Value("${app.file.storage-dir:files}")
    private String fileStorageDir;

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

    /**
     * 将头像等公开文件映射为同源的 files 访问路径。
     *
     * @param registry Spring 静态资源映射注册器
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. 将配置目录规范化为绝对文件系统路径。
        String location = Path.of(fileStorageDir).toAbsolutePath().normalize().toUri().toString();
        // 2. 补齐目录 URI 尾部斜杠，保证资源名称正确拼接。
        if (!location.endsWith("/")) {
            location += "/";
        }
        // 3. 仅通过公开 files 路径提供已验证的头像文件。
        registry.addResourceHandler("/files/**").addResourceLocations(location);
    }
}
