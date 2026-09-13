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
import com.fornexttime.content.res.ContentPinRes;
import com.fornexttime.content.res.ContentCopyRes;
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
        Long userId = UserContext.requireUserId();

        // 2. 拒绝当前用户已经保存过的相同正文。
        ensureContentNotDuplicated(userId, contentText, null);

        // 3. 保存只属于当前登录用户的内容主体。
        Content content = new Content();
        content.setUserId(userId);
        content.setContent(contentText);
        content.setCategoryCode(categoryCode);
        content.setRemark(remark);
        contentMapper.insert(content);

        // 4. 逐个复用或创建标签，并写入内容标签关系。
        saveContentTags(content.getId(), content.getUserId(), tagNames);
        return new ContentIdRes(content.getId());
    }

    /**
     * 更新当前用户的一条内容，并在同一事务中替换全部标签关系。
     *
     * @param id      内容主键
     * @param request 编辑请求参数
     */
    @Transactional
    public void update(Long id, ContentCreateReq request) {
        // 1. 清洗并校验正文、分类、标签和备注。
        String contentText = normalizeContent(request.content());
        String categoryCode = normalizeCategory(request.categoryCode());
        List<String> tagNames = normalizeTagNames(request.tagNames());
        String remark = normalizeRemark(request.remark());
        Long userId = UserContext.requireUserId();

        // 2. 先确认目标内容属于当前用户，再排除自身校验重复正文。
        if (contentMapper.findDetail(userId, id) == null) {
            throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND);
        }
        ensureContentNotDuplicated(userId, contentText, id);

        // 3. 仅更新属于当前用户且未删除的内容主体。
        int affectedRows = contentMapper.update(id, userId, contentText, categoryCode, remark);
        if (affectedRows == 0) {
            throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND);
        }

        // 4. 删除旧标签关系后，按新输入重新建立关系。
        contentTagMapper.deleteByContentId(id);
        saveContentTags(id, userId, tagNames);
    }

    /**
     * 逻辑删除当前用户的一条内容，不物理删除标签关系。
     *
     * @param id 内容主键
     */
    public void delete(Long id) {
        // 1. 仅在当前用户和未删除范围内更新删除标记。
        int affectedRows = contentMapper.softDelete(id, UserContext.requireUserId());
        if (affectedRows == 0) {
            throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND);
        }
        // 2. 保留 content_tag 历史关系，避免额外物理删除。
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
     * 在当前用户范围内分页搜索正文、备注、标签和分类中文名称。
     *
     * @param keyword      搜索关键词
     * @param pageNum      页码
     * @param pageSize     每页数量
     * @param categoryCode 可选分类二次筛选
     * @return 统一的内容列表分页结果
     */
    public PageResult<ContentListRes> search(String keyword, int pageNum, int pageSize, String categoryCode) {
        // 1. 校验搜索关键词与可选分类，并映射命中的中文分类编码。
        String normalizedKeyword = normalizeSearchKeyword(keyword);
        String normalizedCategory = normalizeCategory(categoryCode);
        List<String> categoryCodes = CategoryCode.findCodesByLabelKeyword(normalizedKeyword);
        Long userId = UserContext.requireUserId();

        // 2. 查询符合搜索范围的总数和当前页内容。
        long total = contentMapper.countSearch(userId, normalizedKeyword, normalizedCategory, categoryCodes);
        List<Content> contents = contentMapper.searchPage(userId, normalizedKeyword, normalizedCategory, categoryCodes,
                (pageNum - 1) * pageSize, pageSize);

        // 3. 批量组装标签后返回与普通列表一致的分页结构。
        return PageResult.of(pageNum, pageSize, total, toListRes(contents));
    }

    /**
     * 在剪贴板复制成功后，原子记录当前用户内容的复制统计。
     *
     * @param id 内容主键
     * @return 更新后的复制统计
     */
    public ContentCopyRes recordCopy(Long id) {
        // 1. 在当前用户和未删除范围内原子增加复制次数。
        Long userId = UserContext.requireUserId();
        int affectedRows = contentMapper.incrementCopyCount(id, userId);
        if (affectedRows == 0) {
            throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND);
        }
        // 2. 读取更新后的统计数据，不修改内容的 updateTime。
        Content content = contentMapper.findDetail(userId, id);
        return new ContentCopyRes(content.getCopyCount(), content.getLastCopyTime());
    }

    /**
     * 查询当前用户最近成功复制过的内容。
     *
     * @param limit 最大返回数量
     * @param offset 跳过的最近使用数量
     * @return 统一内容列表响应
     */
    public List<ContentListRes> recent(int limit, int offset) {
        // 1. 仅查询当前用户未删除且存在复制时间的一段内容。
        List<Content> contents = contentMapper.findRecent(UserContext.requireUserId(), offset, limit);
        // 2. 批量组装标签和分类展示数据。
        return toListRes(contents);
    }

    /**
     * 设置当前用户内容的置顶状态，不修改内容修改时间。
     */
    public ContentPinRes setPinned(Long id, boolean pinned) {
        // 1. 在当前用户和未删除范围内更新明确的置顶状态。
        if (contentMapper.updatePinned(id, UserContext.requireUserId(), pinned) == 0) {
            throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND);
        }
        // 2. 返回与请求一致的最终状态。
        return new ContentPinRes(pinned);
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
     * 校验当前用户的未删除内容中不存在相同正文；编辑时排除自身。
     *
     * @param userId    当前登录用户主键
     * @param content   清洗后的正文
     * @param excludeId 编辑时需要排除的内容主键；新增时为空
     */
    private void ensureContentNotDuplicated(Long userId, String content, Long excludeId) {
        // 1. 仅在当前用户、未删除内容范围内检查完全相同的正文。
        if (contentMapper.existsActiveByUserIdAndContent(userId, content, excludeId)) {
            throw new BusinessException(ErrorCode.CONTENT_EXISTS);
        }
    }

    /**
     * 清理搜索关键词并校验 V1 搜索长度边界。
     *
     * @param keyword 原始搜索关键词
     * @return 清洗后的非空搜索关键词
     */
    private String normalizeSearchKeyword(String keyword) {
        // 1. 清理首尾空白，避免空白关键词触发全量搜索。
        String normalized = keyword == null ? "" : keyword.trim();
        if (normalized.isEmpty() || normalized.length() > 100) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        // 2. 返回允许单个中文字符的有效关键词。
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
     * 为指定内容复用或创建标签，并建立全部内容标签关系。
     *
     * @param contentId 内容主键
     * @param userId    当前登录用户主键
     * @param tagNames  清洗后的标签名称列表
     */
    private void saveContentTags(Long contentId, Long userId, List<String> tagNames) {
        // 1. 逐个复用或创建当前用户范围内的标签。
        for (String tagName : tagNames) {
            Tag tag = findOrCreateTag(userId, tagName);
            // 2. 建立内容与标签的关系。
            contentTagMapper.insert(contentId, tag.getId());
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
        // 3. 一次性查询当前页标签并组装列表响应，避免 N+1。
        return PageResult.of(pageNum, pageSize, total, toListRes(contents));
    }

    /**
     * 批量查询内容关联标签，并转换为对外内容列表结构。
     *
     * @param contents 当前页内容实体
     * @return 携带分类名称和标签的内容列表响应
     */
    private List<ContentListRes> toListRes(List<Content> contents) {
        // 1. 一次性查询当前页全部标签并按内容主键分组。
        Map<Long, List<ContentTagRes>> tagsByContentId = contents.isEmpty() ? Map.of() : tagMapper
                .findByContentIds(contents.stream().map(Content::getId).toList()).stream()
                .collect(Collectors.groupingBy(ContentTagRes::contentId));
        // 2. 解析分类中文名称，并移除标签关系中的内部 contentId。
        List<ContentListRes> list = contents.stream()
                .map(content -> {
                    CategoryCode category = CategoryCode.fromCode(content.getCategoryCode());
                    List<TagRes> tags = tagsByContentId.getOrDefault(content.getId(), List.of()).stream()
                            .map(tag -> new TagRes(tag.id(), tag.name()))
                            .toList();
                    return new ContentListRes(content.getId(), content.getContent(), content.getCategoryCode(),
                            category == null ? null : category.getLabel(), content.isPinned(), content.getCopyCount(),
                            content.getLastCopyTime(), content.getCreateTime(), content.getUpdateTime(), tags);
                })
                .toList();
        return list;
    }
}
