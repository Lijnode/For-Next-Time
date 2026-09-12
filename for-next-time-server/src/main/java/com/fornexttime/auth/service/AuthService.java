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

    /**
     * 组装认证服务依赖，集中处理账号、密码和令牌逻辑。
     */
    public AuthService(UserAccountMapper userAccountMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                       JwtProperties jwtProperties) {
        // 1. 保存数据访问与安全组件，供注册和登录流程复用。
        this.userAccountMapper = userAccountMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 注册新账号并直接签发登录令牌，保证注册成功后无需再次登录。
     */
    public LoginRes register(RegisterReq request) {
        // 1. 清洗用户名并校验清洗后的长度。
        String username = normalizeUsername(request.username());
        if (username.length() < 3 || username.length() > 50) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名长度应为3至50个字符");
        }
        // 2. 校验两次输入的密码一致。
        if (!request.password().equals(request.confirmPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }
        // 3. 预先检查用户名，给出明确的重复提示。
        if (userAccountMapper.findByUsername(username) != null) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        // 4. 使用 BCrypt 哈希密码并写入用户记录。
        UserAccount userAccount = new UserAccount();
        userAccount.setUsername(username);
        userAccount.setPasswordHash(passwordEncoder.encode(request.password()));
        try {
            userAccountMapper.insert(userAccount);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        // 5. 基于新生成的用户 ID 签发令牌。
        return createLoginResponse(userAccount);
    }

    /**
     * 校验账号密码并返回登录信息，避免暴露账号是否存在。
     */
    public LoginRes login(LoginReq request) {
        // 1. 使用清洗后的用户名查询账号。
        UserAccount userAccount = userAccountMapper.findByUsername(normalizeUsername(request.username()));
        // 2. 将账号不存在和密码错误统一视为认证失败。
        if (userAccount == null || !passwordEncoder.matches(request.password(), userAccount.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        // 3. 校验通过后签发令牌。
        return createLoginResponse(userAccount);
    }

    /**
     * 将用户实体转换为不含密码哈希的登录响应。
     */
    private LoginRes createLoginResponse(UserAccount userAccount) {
        // 1. 令牌只携带当前用户的身份信息。
        String token = jwtUtil.createToken(userAccount.getId(), userAccount.getUsername());
        // 2. 返回固定 Bearer 类型和配置的有效期。
        return new LoginRes(token, TOKEN_TYPE, jwtProperties.expirationSeconds(),
                new UserSimpleRes(userAccount.getId(), userAccount.getUsername()));
    }

    /**
     * 清理用户名两端空白，避免相同账号以不同空格形式重复注册。
     */
    private String normalizeUsername(String username) {
        // 1. 将空值转换为空字符串，使后续校验保持确定性。
        return username == null ? "" : username.trim();
    }
}
