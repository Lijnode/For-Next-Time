package com.fornexttime.common.security;

import com.fornexttime.common.exception.BusinessException;
import com.fornexttime.common.exception.ErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    /**
     * 注入 JWT 工具，供受保护接口解析身份。
     */
    public AuthInterceptor(JwtUtil jwtUtil) {
        // 1. 保存令牌解析工具。
        this.jwtUtil = jwtUtil;
    }

    /**
     * 在业务接口执行前校验 Bearer Token 并建立当前用户上下文。
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 读取并检查标准 Authorization 请求头。
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 2. 解析有效令牌并写入当前请求线程上下文。
        try {
            UserContext.set(jwtUtil.parseToken(authorization.substring(BEARER_PREFIX.length())));
            return true;
        } catch (ExpiredJwtException exception) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
    }

    /**
     * 在请求完成后清理线程变量，避免线程复用导致身份串扰。
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) {
        // 1. 无论请求成功或失败都释放当前用户上下文。
        UserContext.clear();
    }
}
