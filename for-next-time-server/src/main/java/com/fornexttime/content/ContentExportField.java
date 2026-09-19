package com.fornexttime.content;

import com.fornexttime.common.exception.BusinessException;
import com.fornexttime.common.exception.ErrorCode;

import java.util.Arrays;

/**
 * 内容导出允许选择的固定字段。
 */
public enum ContentExportField {

    /** 内容正文。 */
    CONTENT("content", "正文"),

    /** 固定分类。 */
    CATEGORY("category", "分类"),

    /** 内容标签。 */
    TAGS("tags", "标签"),

    /** 补充备注。 */
    REMARK("remark", "备注"),

    /** 是否置顶。 */
    PINNED("pinned", "置顶状态"),

    /** 成功复制次数。 */
    COPY_COUNT("copyCount", "复制次数"),

    /** 最近成功复制时间。 */
    LAST_COPY_TIME("lastCopyTime", "最近使用时间"),

    /** 创建时间。 */
    CREATE_TIME("createTime", "创建时间"),

    /** 最后编辑时间。 */
    UPDATE_TIME("updateTime", "修改时间");

    /** URL 参数中的固定字段编码。 */
    private final String code;

    /** 导出 TXT 中的字段中文名称。 */
    private final String label;

    /**
     * 创建固定导出字段。
     *
     * @param code  URL 参数编码
     * @param label 导出展示名称
     */
    ContentExportField(String code, String label) {
        // 1. 保存不可由客户端覆盖的字段编码和展示名称。
        this.code = code;
        this.label = label;
    }

    /**
     * 根据 URL 参数解析一个固定导出字段。
     *
     * @param code 前端传入的字段编码
     * @return 对应的固定导出字段
     */
    public static ContentExportField fromCode(String code) {
        // 1. 仅在固定白名单中查找完全匹配的字段。
        return Arrays.stream(values())
                .filter(field -> field.code.equals(code))
                .findFirst()
                // 2. 未知字段统一返回参数错误，不允许扩展任意导出列。
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "导出字段无效"));
    }

    /**
     * 获取导出 TXT 中使用的中文字段名称。
     *
     * @return 固定中文字段名称
     */
    public String getLabel() {
        // 1. 返回与字段编码一一对应的展示名称。
        return label;
    }
}
