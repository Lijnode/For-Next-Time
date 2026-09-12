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

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<LoginRes> register(@Valid @RequestBody RegisterReq request) {
        return Result.success(authService.register(request));
    }

    @PostMapping("/login")
    public Result<LoginRes> login(@Valid @RequestBody LoginReq request) {
        return Result.success(authService.login(request));
    }
}
