package com.fornexttime.content.mapper;

import com.fornexttime.content.entity.Content;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ContentMapper {

    int insert(Content content);
}
