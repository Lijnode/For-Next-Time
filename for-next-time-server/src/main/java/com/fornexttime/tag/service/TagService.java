package com.fornexttime.tag.service;

import com.fornexttime.common.security.UserContext;
import com.fornexttime.tag.mapper.TagMapper;
import com.fornexttime.tag.res.TagRes;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户标签查询业务服务。
 */
@Service
public class TagService {

    /** 用户标签数据访问组件。 */
    @Resource
    private TagMapper tagMapper;

    /**
     * 查询当前用户可复用的标签，并将空白关键词视为无筛选。
     *
     * @param keyword 可选标签关键词
     * @param limit   最大返回数量
     * @return 对外展示的标签列表
     */
    public List<TagRes> list(String keyword, int limit) {
        // 1. 清理关键词两端空白，空白时不附加名称筛选。
        String normalizedKeyword = keyword == null ? null : keyword.trim();
        if (normalizedKeyword != null && normalizedKeyword.isEmpty()) {
            normalizedKeyword = null;
        }
        // 2. 仅查询当前登录用户的标签，并移除内部 userId 字段。
        return tagMapper.findList(UserContext.requireUserId(), normalizedKeyword, limit).stream()
                .map(tag -> new TagRes(tag.getId(), tag.getName()))
                .toList();
    }
}
