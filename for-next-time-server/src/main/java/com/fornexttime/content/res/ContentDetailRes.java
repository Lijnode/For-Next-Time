package com.fornexttime.content.res;

import com.fornexttime.tag.res.TagRes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 内容详情页展示的完整数据。
 */
public record ContentDetailRes(
        /** 内容主键。 */
        Long id,

        /** 内容正文。 */
        String content,

        /** 固定分类编码，取值见 CategoryCode；未分类时为空。 */
        String categoryCode,

        /** 分类中文展示名称，未分类时为空。 */
        String categoryName,

        /** 当前内容关联的标签列表，未关联时返回空数组。 */
        List<TagRes> tags,

        /** 用户填写的补充备注，未填写时为空。 */
        String remark,

        /** 是否置顶。 */
        boolean pinned,

        /** 成功复制次数。 */
        int copyCount,

        /** 最近一次成功复制时间，从未复制时为空。 */
        LocalDateTime lastCopyTime,

        /** 内容创建时间。 */
        LocalDateTime createTime,

        /** 内容最后编辑时间。 */
        LocalDateTime updateTime
) {
}
