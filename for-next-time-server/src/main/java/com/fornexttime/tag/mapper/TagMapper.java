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

    /**
     * 查询当前用户最近创建的可复用标签。
     *
     * @param userId  当前登录用户主键
     * @param keyword 可选标签关键词
     * @param limit   最大返回数量
     * @return 匹配的用户标签
     */
    List<Tag> findList(@Param("userId") Long userId, @Param("keyword") String keyword, @Param("limit") int limit);

    List<ContentTagRes> findByContentIds(@Param("contentIds") List<Long> contentIds);
}
