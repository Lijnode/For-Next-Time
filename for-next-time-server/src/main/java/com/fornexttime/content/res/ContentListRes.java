package com.fornexttime.content.res;

import java.util.List;

/**
 * 内容列表、搜索和最近使用场景共用的卡片数据。
 */
public record ContentListRes(
    /** 内容主键。 */
    Long id,

    /** 内容正文。 */
    String content,

        /** 固定分类编码，取值见 CategoryCode；未分类时为空。 */
        String categoryCode,

    /** 是否置顶。 */
    boolean pinned,

    /** 成功复制次数。 */
    int copyCount,

    /** 当前内容关联的标签列表，未关联时返回空数组。 */
    List<ContentTagRes> tags
) {
}
