package com.fornexttime.tag.controller;

import com.fornexttime.common.model.Result;
import com.fornexttime.tag.res.TagRes;
import com.fornexttime.tag.service.TagService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 当前用户标签查询接口。
 */
@RestController
@RequestMapping("/api/v1/tags")
public class TagController {

    /** 标签查询业务服务。 */
    @Resource
    private TagService tagService;

    /**
     * 获取当前用户的已有标签，可按名称关键词筛选。
     *
     * @param keyword 可选标签关键词
     * @param limit   最大返回数量，默认二十条
     * @return 标签列表
     */
    @GetMapping
    public Result<List<TagRes>> list(@RequestParam(required = false) String keyword,
                                     @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        // 1. 交由服务在当前用户数据范围内执行标签查询。
        return Result.success(tagService.list(keyword, limit));
    }
}
