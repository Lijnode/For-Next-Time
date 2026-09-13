package com.fornexttime.content.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ContentTagMapper {

    int insert(@Param("contentId") Long contentId, @Param("tagId") Long tagId);

    /**
     * 删除一条内容已有的全部标签关系。
     *
     * @param contentId 内容主键
     * @return 删除的关系数量
     */
    int deleteByContentId(@Param("contentId") Long contentId);
}
