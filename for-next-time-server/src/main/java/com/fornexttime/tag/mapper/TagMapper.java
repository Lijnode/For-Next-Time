package com.fornexttime.tag.mapper;

import com.fornexttime.tag.entity.Tag;
import com.fornexttime.content.res.ContentTagRes;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TagMapper {

    Tag findByUserIdAndName(Long userId, String name);

    int insert(Tag tag);

    List<ContentTagRes> findByContentIds(@Param("contentIds") List<Long> contentIds);
}
