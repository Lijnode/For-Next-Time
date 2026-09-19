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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * 提供当前登录用户的资料读取和密码修改能力。
 */
@Service
public class UserService {

    /** 单个头像最大文件大小：2MB。 */
    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024;

    /** 用户账号数据访问组件。 */
    @Resource
    private UserAccountMapper userAccountMapper;

    /** 内容数据访问组件。 */
    @Resource
    private ContentMapper contentMapper;

    /** BCrypt 密码编码组件。 */
    @Resource
    private PasswordEncoder passwordEncoder;

    /** 文件存储根目录，头像统一保存在其中的 avatars 子目录。 */
    @Value("${app.file.storage-dir:files}")
    private String fileStorageDir;

    /** 前端访问头像文件的同源路径前缀，不暴露服务器磁盘目录。 */
    @Value("${app.file.public-url-prefix:/files}")
    private String filePublicUrlPrefix;

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
        return new UserProfileRes(userAccount.getId(), userAccount.getUsername(),
                toAvatarUrl(userAccount.getAvatarPath()), contentCount,
                userAccount.getCreateTime());
    }

    /**
     * 校验并保存当前登录用户上传的头像，同时更新账号中的相对路径。
     *
     * @param file 用户选择的 JPEG 或 PNG 图片文件
     * @return 保存后的最新用户资料
     */
    public UserProfileRes uploadAvatar(MultipartFile file) {
        // 1. 确认当前用户存在，并校验上传文件的基础约束。
        long userId = UserContext.requireUserId();
        requireUserAccount(userId);
        validateAvatar(file);
        // 2. 生成不依赖原始文件名的存储名称并写入头像目录。
        String avatarPath = saveAvatar(file);
        // 3. 仅更新当前用户的头像路径，再返回最新资料。
        userAccountMapper.updateAvatarPath(userId, avatarPath);
        return getProfile();
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

    /**
     * 校验上传文件的类型、大小和真实图片内容，避免将任意文件保存为头像。
     *
     * @param file 待校验的上传文件
     */
    private void validateAvatar(MultipartFile file) {
        // 1. 过滤空文件和超过大小上限的请求。
        if (file == null || file.isEmpty() || file.getSize() > MAX_AVATAR_SIZE) {
            throw new BusinessException(ErrorCode.AVATAR_INVALID);
        }
        // 2. 仅接受明确的 JPEG、PNG MIME 类型。
        String contentType = file.getContentType();
        if (!"image/jpeg".equals(contentType) && !"image/png".equals(contentType)) {
            throw new BusinessException(ErrorCode.AVATAR_INVALID);
        }
        // 3. 使用 JDK 图像解码器确认上传内容确实是可读图片。
        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new BusinessException(ErrorCode.AVATAR_INVALID);
            }
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.AVATAR_INVALID);
        }
    }

    /**
     * 将已校验的头像保存到本地文件目录，并返回数据库存储的相对路径。
     *
     * @param file 已验证的头像文件
     * @return 文件存储根目录下的头像相对路径
     */
    private String saveAvatar(MultipartFile file) {
        // 1. 创建头像目录，保证首次上传也能成功保存。
        Path avatarDirectory = Path.of(fileStorageDir, "avatars").toAbsolutePath().normalize();
        try {
            Files.createDirectories(avatarDirectory);
            // 2. 根据经过校验的 MIME 类型确定安全扩展名。
            String extension = "image/png".equals(file.getContentType()) ? ".png" : ".jpg";
            String fileName = UUID.randomUUID() + extension;
            // 3. 将文件写入随机文件名，避免用户文件名影响存储路径。
            file.transferTo(avatarDirectory.resolve(fileName));
            return "avatars/" + fileName;
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像保存失败，请稍后重试");
        }
    }

    /**
     * 将数据库中的头像相对路径转换为前端可访问的同源地址。
     *
     * @param avatarPath 数据库存储的相对路径
     * @return 头像访问地址；未上传时为 null
     */
    private String toAvatarUrl(String avatarPath) {
        // 1. 未上传头像时保留空值，让前端展示用户名首字。
        if (avatarPath == null || avatarPath.isBlank()) {
            return null;
        }
        // 2. 使用部署环境提供的同源路径，避免将服务器磁盘目录暴露给前端。
        return filePublicUrlPrefix + "/" + avatarPath;
    }
}
