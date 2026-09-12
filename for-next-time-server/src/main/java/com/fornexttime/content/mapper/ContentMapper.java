package com.fornexttime.content.mapper;

import com.fornexttime.content.entity.Content;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ContentMapper {

    int insert(Content content);

    List<Content> findPage(@Param("userId") Long userId, @Param("categoryCode") String categoryCode,
                           @Param("pinned") Boolean pinned, @Param("offset") int offset, @Param("pageSize") int pageSize);

    long countPage(@Param("userId") Long userId, @Param("categoryCode") String categoryCode, @Param("pinned") Boolean pinned);
}
