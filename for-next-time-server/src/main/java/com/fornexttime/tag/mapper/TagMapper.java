package com.fornexttime.tag.mapper;

import com.fornexttime.tag.entity.Tag;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TagMapper {

    Tag findByUserIdAndName(Long userId, String name);

    int insert(Tag tag);
}
