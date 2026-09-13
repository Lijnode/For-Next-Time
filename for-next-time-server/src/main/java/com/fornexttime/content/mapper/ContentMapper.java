package com.fornexttime.content.mapper;

import com.fornexttime.content.entity.Content;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ContentMapper {

    int insert(Content content);

    /**
     * 在当前用户范围内更新内容正文、分类和备注。
     *
     * @param id           内容主键
     * @param userId       当前登录用户主键
     * @param content      清洗后的正文
     * @param categoryCode 校验后的分类编码
     * @param remark       清洗后的备注
     * @return 实际更新行数
     */
    int update(@Param("id") Long id, @Param("userId") Long userId, @Param("content") String content,
               @Param("categoryCode") String categoryCode, @Param("remark") String remark);

    /**
     * 在当前用户范围内逻辑删除一条内容。
     *
     * @param id     内容主键
     * @param userId 当前登录用户主键
     * @return 实际更新行数
     */
    int softDelete(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 在指定用户的数据范围内查询一条未删除内容的完整详情。
     *
     * @param userId 当前登录用户主键
     * @param id      内容主键
     * @return 匹配的内容，不存在或越权时返回 null
     */
    Content findDetail(@Param("userId") Long userId, @Param("id") Long id);

    /**
     * 在当前用户范围内分页搜索内容、备注、标签和分类。
     *
     * @param userId        当前登录用户主键
     * @param keyword       清洗后的搜索关键词
     * @param categoryCode  可选分类二次筛选
     * @param categoryCodes 按中文分类名匹配得到的分类编码
     * @param offset        分页偏移量
     * @param pageSize      每页数量
     * @return 匹配的当前页内容
     */
    List<Content> searchPage(@Param("userId") Long userId, @Param("keyword") String keyword,
                             @Param("categoryCode") String categoryCode, @Param("categoryCodes") List<String> categoryCodes,
                             @Param("offset") int offset, @Param("pageSize") int pageSize);

    /**
     * 统计当前用户范围内符合搜索条件的内容总数。
     *
     * @param userId        当前登录用户主键
     * @param keyword       清洗后的搜索关键词
     * @param categoryCode  可选分类二次筛选
     * @param categoryCodes 按中文分类名匹配得到的分类编码
     * @return 匹配内容总数
     */
    long countSearch(@Param("userId") Long userId, @Param("keyword") String keyword,
                     @Param("categoryCode") String categoryCode, @Param("categoryCodes") List<String> categoryCodes);

    /**
     * 原子增加当前用户一条内容的复制次数并刷新最近复制时间。
     *
     * @param id     内容主键
     * @param userId 当前登录用户主键
     * @return 实际更新行数
     */
    int incrementCopyCount(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 在当前用户范围内设置内容置顶状态，不更新内容修改时间。
     */
    int updatePinned(@Param("id") Long id, @Param("userId") Long userId, @Param("pinned") boolean pinned);

    /**
     * 查询当前用户最近成功复制的内容。
     *
     * @param userId 当前登录用户主键
     * @param limit  最大返回数量
     * @return 按最近复制时间倒序的内容
     */
    List<Content> findRecent(@Param("userId") Long userId, @Param("limit") int limit);

    List<Content> findPage(@Param("userId") Long userId, @Param("categoryCode") String categoryCode,
                           @Param("pinned") Boolean pinned, @Param("offset") int offset, @Param("pageSize") int pageSize);

    long countPage(@Param("userId") Long userId, @Param("categoryCode") String categoryCode, @Param("pinned") Boolean pinned);
}
