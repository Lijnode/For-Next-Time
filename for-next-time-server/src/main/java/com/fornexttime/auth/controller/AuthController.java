package com.fornexttime.auth.controller;

import com.fornexttime.auth.req.LoginReq;
import com.fornexttime.auth.req.RegisterReq;
import com.fornexttime.auth.res.LoginRes;
import com.fornexttime.auth.service.AuthService;
import com.fornexttime.common.model.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * 注入认证业务服务。
     */
    public AuthController(AuthService authService) {
        // 1. 保存服务依赖，Controller 只负责请求编排。
        this.authService = authService;
    }

    /**
     * 接收注册请求并返回自动登录后的令牌信息。
     */
    @PostMapping("/register")
    public Result<LoginRes> register(@Valid @RequestBody RegisterReq request) {
        // 1. 参数校验通过后交由服务完成注册。
        return Result.success(authService.register(request));
    }

    /**
     * 接收登录请求并返回认证后的令牌信息。
     */
    @PostMapping("/login")
    public Result<LoginRes> login(@Valid @RequestBody LoginReq request) {
        // 1. 参数校验通过后交由服务完成身份认证。
        return Result.success(authService.login(request));
    }
}
