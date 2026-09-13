package com.fornexttime.content.service;

import com.fornexttime.common.exception.BusinessException;
import com.fornexttime.common.exception.ErrorCode;
import com.fornexttime.common.security.UserContext;
import com.fornexttime.common.model.PageResult;
import com.fornexttime.content.CategoryCode;
import com.fornexttime.content.entity.Content;
import com.fornexttime.content.mapper.ContentMapper;
import com.fornexttime.content.mapper.ContentTagMapper;
import com.fornexttime.content.req.ContentCreateReq;
import com.fornexttime.content.res.ContentDetailRes;
import com.fornexttime.content.res.ContentIdRes;
import com.fornexttime.content.res.ContentListRes;
import com.fornexttime.tag.entity.Tag;
import com.fornexttime.tag.mapper.TagMapper;
import com.fornexttime.tag.res.TagRes;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.Resource;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.fornexttime.content.res.ContentTagRes;

@Service
public class ContentService {

    /** 内容主体数据访问组件。 */
    @Resource
    private ContentMapper contentMapper;
    /** 用户标签数据访问组件。 */
    @Resource
    private TagMapper tagMapper;
    /** 内容标签关系数据访问组件。 */
    @Resource
    private ContentTagMapper contentTagMapper;

    /**
     * 创建当前用户的内容，并在同一事务中处理标签与关系。
     */
    @Transactional
    public ContentIdRes create(ContentCreateReq request) {
        // 1. 清洗并校验正文、分类、标签和备注。
        String contentText = normalizeContent(request.content());
        String categoryCode = normalizeCategory(request.categoryCode());
        List<String> tagNames = normalizeTagNames(request.tagNames());
        String remark = normalizeRemark(request.remark());

        // 2. 保存只属于当前登录用户的内容主体。
        Content content = new Content();
        content.setUserId(UserContext.requireUserId());
        content.setContent(contentText);
        content.setCategoryCode(categoryCode);
        content.setRemark(remark);
        contentMapper.insert(content);

        // 3. 逐个复用或创建标签，并写入内容标签关系。
        for (String tagName : tagNames) {
            Tag tag = findOrCreateTag(content.getUserId(), tagName);
            contentTagMapper.insert(content.getId(), tag.getId());
        }
        return new ContentIdRes(content.getId());
    }

    /**
     * 查询当前登录用户的一条内容详情，并组装分类和标签展示数据。
     *
     * @param id 内容主键
     * @return 当前用户可访问的内容详情
     */
    public ContentDetailRes getDetail(Long id) {
        // 1. 以内容主键和当前用户主键共同查询未删除内容。
        Content content = contentMapper.findDetail(UserContext.requireUserId(), id);
        if (content == null) {
            throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND);
        }
        // 2. 查询内容标签，并转换为不携带内部 contentId 的对外结构。
        List<TagRes> tags = tagMapper.findByContentIds(List.of(id)).stream()
                .map(tag -> new TagRes(tag.id(), tag.name()))
                .toList();
        // 3. 由固定枚举解析中文分类名，组装完整详情响应。
        CategoryCode category = CategoryCode.fromCode(content.getCategoryCode());
        return new ContentDetailRes(content.getId(), content.getContent(), content.getCategoryCode(),
                category == null ? null : category.getLabel(), tags, content.getRemark(), content.isPinned(),
                content.getCopyCount(), content.getLastCopyTime(), content.getCreateTime(), content.getUpdateTime());
    }

    /**
     * 清理正文两端空白，同时保留内部换行与空格。
     */
    private String normalizeContent(String content) {
        // 1. 使用 strip 仅清理首尾无意义空白。
        String normalized = content == null ? "" : content.strip();
        // 2. 按固定内容边界返回业务错误。
        if (normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.CONTENT_EMPTY);
        }
        if (normalized.length() > 5000) {
            throw new BusinessException(ErrorCode.CONTENT_TOO_LONG);
        }
        return normalized;
    }

    /**
     * 校验分类编码并将未分类统一为 null。
     */
    private String normalizeCategory(String categoryCode) {
        // 1. 使用固定枚举校验分类。
        try {
            return CategoryCode.normalize(categoryCode);
        } catch (IllegalArgumentException exception) {
            // 2. 对外返回锁定的分类错误码。
            throw new BusinessException(ErrorCode.CATEGORY_INVALID);
        }
    }

    /**
     * 清洗标签集合、去重并校验 V1 标签上限。
     */
    private List<String> normalizeTagNames(List<String> tagNames) {
        // 1. 清理空白标签并保留输入顺序去重。
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (tagNames != null) {
            for (String tagName : tagNames) {
                String value = tagName == null ? "" : tagName.trim();
                if (!value.isEmpty()) {
                    normalized.add(value);
                }
            }
        }
        // 2. 校验数量和单个标签长度。
        if (normalized.size() > 10) {
            throw new BusinessException(ErrorCode.TAG_LIMIT_EXCEEDED);
        }
        if (normalized.stream().anyMatch(tagName -> tagName.length() > 20)) {
            throw new BusinessException(ErrorCode.TAG_NAME_TOO_LONG);
        }
        return List.copyOf(normalized);
    }

    /**
     * 清理可选备注，并校验其最大长度。
     */
    private String normalizeRemark(String remark) {
        // 1. 将空白备注转换为数据库 null。
        String normalized = remark == null ? null : remark.trim();
        if (normalized != null && normalized.isEmpty()) {
            return null;
        }
        // 2. 对非空备注执行长度校验。
        if (normalized != null && normalized.length() > 1000) {
            throw new BusinessException(ErrorCode.REMARK_TOO_LONG);
        }
        return normalized;
    }

    /**
     * 在当前用户范围内查找标签，不存在时创建并处理并发重复。
     */
    private Tag findOrCreateTag(Long userId, String tagName) {
        // 1. 优先复用当前用户已有的同名标签。
        Tag tag = tagMapper.findByUserIdAndName(userId, tagName);
        if (tag != null) {
            return tag;
        }
        // 2. 不存在时创建标签，并处理唯一索引竞争。
        tag = new Tag();
        tag.setUserId(userId);
        tag.setName(tagName);
        try {
            tagMapper.insert(tag);
            return tag;
        } catch (DuplicateKeyException exception) {
            return tagMapper.findByUserIdAndName(userId, tagName);
        }
    }

    /**
     * 分页查询当前用户内容，并按置顶、更新时间和 ID 排序。
     */
    public PageResult<ContentListRes> list(int pageNum, int pageSize, String categoryCode, Boolean pinned) {
        // 1. 校验可选分类并计算分页偏移量。
        String normalizedCategory = normalizeCategory(categoryCode);
        Long userId = UserContext.requireUserId();
        long total = contentMapper.countPage(userId, normalizedCategory, pinned);
        // 2. 只查询当前用户未删除的当前页内容。
        List<Content> contents = contentMapper.findPage(userId, normalizedCategory, pinned,
                (pageNum - 1) * pageSize, pageSize);
        // 3. 一次性查询当前页标签并按内容 ID 分组，避免 N+1。
        Map<Long, List<ContentTagRes>> tagsByContentId = contents.isEmpty() ? Map.of() : tagMapper
                .findByContentIds(contents.stream().map(Content::getId).toList()).stream()
                .collect(Collectors.groupingBy(ContentTagRes::contentId));
        List<ContentListRes> list = contents.stream()
                .map(content -> new ContentListRes(content.getId(), content.getContent(), content.getCategoryCode(),
                        content.isPinned(), content.getCopyCount(), tagsByContentId.getOrDefault(content.getId(), List.of())
                        .stream().map(tag -> new TagRes(tag.id(), tag.name())).toList()))
                .toList();
        return PageResult.of(pageNum, pageSize, total, list);
    }
}
