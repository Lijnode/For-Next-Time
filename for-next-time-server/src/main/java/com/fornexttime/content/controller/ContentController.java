package com.fornexttime.content.controller;

import com.fornexttime.common.model.Result;
import com.fornexttime.content.req.ContentCreateReq;
import com.fornexttime.content.req.ContentPinReq;
import com.fornexttime.content.res.ContentDetailRes;
import com.fornexttime.content.res.ContentCopyRes;
import com.fornexttime.content.res.ContentPinRes;
import com.fornexttime.content.res.ContentIdRes;
import com.fornexttime.content.service.ContentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.annotation.Resource;
import com.fornexttime.common.model.PageResult;
import com.fornexttime.content.res.ContentListRes;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contents")
public class ContentController {

    /** 内容业务服务。 */
    @Resource
    private ContentService contentService;

    /**
     * 创建当前登录用户的一条内容。
     */
    @PostMapping
    public Result<ContentIdRes> create(@Valid @RequestBody ContentCreateReq request) {
        // 参数校验通过后交由服务完成事务写入。
        return Result.success(contentService.create(request));
    }

    /**
     * 获取当前登录用户可访问的一条内容详情。
     *
     * @param id 内容主键
     * @return 内容详情
     */
    @GetMapping("/{id}")
    public Result<ContentDetailRes> detail(@PathVariable @Min(1) Long id) {
        // 1. 交由服务在当前用户数据范围内查询详情。
        return Result.success(contentService.getDetail(id));
    }

    /**
     * 搜索当前用户正文、备注、标签和分类匹配的内容。
     *
     * @param keyword      必填搜索关键词
     * @param pageNum      页码
     * @param pageSize     每页数量
     * @param categoryCode 可选分类二次筛选
     * @return 内容搜索分页结果
     */
    @GetMapping("/search")
    public Result<PageResult<ContentListRes>> search(@RequestParam String keyword,
                                                      @RequestParam(defaultValue = "1") @Min(1) int pageNum,
                                                      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
                                                      @RequestParam(required = false) String categoryCode) {
        // 1. 交由服务校验关键词并在当前用户数据范围内执行搜索。
        return Result.success(contentService.search(keyword, pageNum, pageSize, categoryCode));
    }

    /**
     * 在前端剪贴板复制成功后记录当前用户内容的复制统计。
     *
     * @param id 内容主键
     * @return 更新后的复制统计
     */
    @PostMapping("/{id}/copy")
    public Result<ContentCopyRes> copy(@PathVariable @Min(1) Long id) {
        // 1. 交由服务在当前用户范围内原子更新复制统计。
        return Result.success(contentService.recordCopy(id));
    }

    /**
     * 获取当前用户最近成功复制过的内容。
     *
     * @param limit 最大返回数量，默认五条
     * @return 最近使用内容列表
     */
    @GetMapping("/recent")
    public Result<List<ContentListRes>> recent(@RequestParam(defaultValue = "5") @Min(1) @Max(20) int limit) {
        // 1. 交由服务按最近复制时间查询当前用户内容。
        return Result.success(contentService.recent(limit));
    }

    /**
     * 设置当前用户内容的置顶状态。
     */
    @PutMapping("/{id}/pin")
    public Result<ContentPinRes> pin(@PathVariable @Min(1) Long id, @RequestBody ContentPinReq request) {
        // 1. 按请求的明确状态设置置顶，不使用 toggle 语义。
        return Result.success(contentService.setPinned(id, request.pinned()));
    }

    /**
     * 更新当前登录用户的一条内容及其标签关系。
     *
     * @param id      内容主键
     * @param request 编辑请求参数
     * @return 空成功响应
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable @Min(1) Long id, @Valid @RequestBody ContentCreateReq request) {
        // 1. 将编辑请求交由服务在当前用户范围内完成事务更新。
        contentService.update(id, request);
        // 2. 返回不包含额外数据的成功响应。
        return Result.success();
    }

    /**
     * 逻辑删除当前登录用户的一条内容。
     *
     * @param id 内容主键
     * @return 空成功响应
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable @Min(1) Long id) {
        // 1. 将删除操作限制在当前登录用户的数据范围内。
        contentService.delete(id);
        // 2. 返回不包含额外数据的成功响应。
        return Result.success();
    }

    /**
     * 分页获取当前用户内容列表。
     */
    @GetMapping
    public Result<PageResult<ContentListRes>> list(
            @RequestParam(defaultValue = "1") @Min(1) int pageNum,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) Boolean pinned) {
        // 使用当前登录用户范围查询分页内容。
        return Result.success(contentService.list(pageNum, pageSize, categoryCode, pinned));
    }
}
