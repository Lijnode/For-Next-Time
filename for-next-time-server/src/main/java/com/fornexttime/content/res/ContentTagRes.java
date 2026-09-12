package com.fornexttime.content.res;

/**
 * 内容列表批量组装时使用的标签数据。
 */
public record ContentTagRes(
    /** 所属内容主键，仅供服务端按内容分组。 */
    Long contentId,

    /** 标签主键。 */
    Long id,

    /** 标签名称。 */
    String name
) {
}
