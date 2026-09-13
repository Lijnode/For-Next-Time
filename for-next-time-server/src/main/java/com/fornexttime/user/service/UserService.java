package com.fornexttime.user.service;

import com.fornexttime.auth.entity.UserAccount;
import com.fornexttime.auth.mapper.UserAccountMapper;
import com.fornexttime.common.exception.BusinessException;
import com.fornexttime.common.exception.ErrorCode;
import com.fornexttime.common.security.UserContext;
import com.fornexttime.content.mapper.ContentMapper;
import com.fornexttime.user.req.ChangePasswordReq;
import com.fornexttime.user.res.UserProfileRes;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 提供当前登录用户的资料读取和密码修改能力。
 */
@Service
public class UserService {

    /** 用户账号数据访问组件。 */
    @Resource
    private UserAccountMapper userAccountMapper;

    /** 内容数据访问组件。 */
    @Resource
    private ContentMapper contentMapper;

    /** BCrypt 密码编码组件。 */
    @Resource
    private PasswordEncoder passwordEncoder;

    /**
     * 查询当前登录用户的公开资料与有效内容数量。
     *
     * @return 不含密码摘要的当前用户资料
     */
    public UserProfileRes getProfile() {
        // 1. 从 JWT 线程上下文获取当前用户身份。
        long userId = UserContext.requireUserId();
        // 2. 查询账号主体，不存在时按未登录处理。
        UserAccount userAccount = requireUserAccount(userId);
        // 3. 统计未逻辑删除内容并组装安全响应。
        long contentCount = contentMapper.countActiveByUserId(userId);
        return new UserProfileRes(userAccount.getId(), userAccount.getUsername(), contentCount,
                userAccount.getCreateTime());
    }

    /**
     * 校验当前密码后更新当前登录用户的密码摘要。
     *
     * @param request 已通过格式校验的密码修改请求
     */
    public void changePassword(ChangePasswordReq request) {
        // 1. 从 JWT 线程上下文获取当前用户账号。
        UserAccount userAccount = requireUserAccount(UserContext.requireUserId());
        // 2. 验证旧密码，避免仅持有令牌即可改写密码。
        if (!passwordEncoder.matches(request.oldPassword(), userAccount.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "旧密码错误");
        }
        // 3. 校验两次新密码一致，并避免无意义地重复设置旧密码。
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }
        if (passwordEncoder.matches(request.newPassword(), userAccount.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新密码不能与旧密码相同");
        }
        // 4. 生成新的 BCrypt 摘要并限定为当前用户更新。
        userAccountMapper.updatePasswordHash(userAccount.getId(), passwordEncoder.encode(request.newPassword()));
    }

    /**
     * 查询指定账号；令牌对应账号已不存在时终止当前请求。
     *
     * @param userId 当前登录用户主键
     * @return 已存在的用户账号
     */
    private UserAccount requireUserAccount(long userId) {
        // 1. 用当前用户主键查询账号记录。
        UserAccount userAccount = userAccountMapper.findById(userId);
        // 2. 账号已不存在时按认证失效处理。
        if (userAccount == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        // 3. 返回仅供服务内部使用的完整账号实体。
        return userAccount;
    }
}
