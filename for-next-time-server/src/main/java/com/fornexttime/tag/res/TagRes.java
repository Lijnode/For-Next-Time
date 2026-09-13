package com.fornexttime.tag.res;

/**
 * 对外展示的标签基础信息。
 */
public record TagRes(
        /** 标签主键。 */
        Long id,

        /** 标签名称。 */
        String name
) {
}
