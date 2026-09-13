package com.fornexttime.content.mapper;

import com.fornexttime.content.entity.Content;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ContentMapper {

    int insert(Content content);

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
