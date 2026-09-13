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

    List<Content> findPage(@Param("userId") Long userId, @Param("categoryCode") String categoryCode,
                           @Param("pinned") Boolean pinned, @Param("offset") int offset, @Param("pageSize") int pageSize);

    long countPage(@Param("userId") Long userId, @Param("categoryCode") String categoryCode, @Param("pinned") Boolean pinned);
}
