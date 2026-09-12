package com.fornexttime.auth.service;

import com.fornexttime.auth.entity.UserAccount;
import com.fornexttime.auth.mapper.UserAccountMapper;
import com.fornexttime.auth.req.LoginReq;
import com.fornexttime.auth.req.RegisterReq;
import com.fornexttime.auth.res.LoginRes;
import com.fornexttime.auth.res.UserSimpleRes;
import com.fornexttime.common.config.JwtProperties;
import com.fornexttime.common.exception.BusinessException;
import com.fornexttime.common.exception.ErrorCode;
import com.fornexttime.common.security.JwtUtil;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    public AuthService(UserAccountMapper userAccountMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                       JwtProperties jwtProperties) {
        this.userAccountMapper = userAccountMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    public LoginRes register(RegisterReq request) {
        String username = normalizeUsername(request.username());
        if (username.length() < 3 || username.length() > 50) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名长度应为3至50个字符");
        }
        if (!request.password().equals(request.confirmPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }
        if (userAccountMapper.findByUsername(username) != null) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        UserAccount userAccount = new UserAccount();
        userAccount.setUsername(username);
        userAccount.setPasswordHash(passwordEncoder.encode(request.password()));
        try {
            userAccountMapper.insert(userAccount);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        return createLoginResponse(userAccount);
    }

    public LoginRes login(LoginReq request) {
        UserAccount userAccount = userAccountMapper.findByUsername(normalizeUsername(request.username()));
        if (userAccount == null || !passwordEncoder.matches(request.password(), userAccount.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        return createLoginResponse(userAccount);
    }

    private LoginRes createLoginResponse(UserAccount userAccount) {
        String token = jwtUtil.createToken(userAccount.getId(), userAccount.getUsername());
        return new LoginRes(token, TOKEN_TYPE, jwtProperties.expirationSeconds(),
                new UserSimpleRes(userAccount.getId(), userAccount.getUsername()));
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }
}
