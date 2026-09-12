package com.fornexttime.content.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ContentTagMapper {

    int insert(@Param("contentId") Long contentId, @Param("tagId") Long tagId);
}
