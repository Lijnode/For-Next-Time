package com.fornexttime.auth.mapper;

import com.fornexttime.auth.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserAccountMapper {

    /**
     * 按用户名查询账号，用于认证与重复用户名校验。
     *
     * @param username 清洗后的用户名
     * @return 匹配账号；不存在时返回 null
     */
    UserAccount findByUsername(String username);

    /**
     * 按主键查询账号，用于读取当前用户资料。
     *
     * @param id 用户主键
     * @return 匹配账号；不存在时返回 null
     */
    UserAccount findById(@Param("id") Long id);

    /**
     * 更新指定账号的 BCrypt 密码摘要。
     *
     * @param id           用户主键
     * @param passwordHash 新的 BCrypt 密码摘要
     * @return 实际更新行数
     */
    int updatePasswordHash(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    /**
     * 更新指定账号的头像相对存储路径。
     *
     * @param id         用户主键
     * @param avatarPath 文件存储根目录下的相对路径
     * @return 实际更新行数
     */
    int updateAvatarPath(@Param("id") Long id, @Param("avatarPath") String avatarPath);

    /**
     * 新增账号并回填数据库生成的主键。
     *
     * @param userAccount 待保存的账号
     * @return 实际插入行数
     */
    int insert(UserAccount userAccount);
}
