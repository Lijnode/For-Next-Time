package com.fornexttime.content.req;

/**
 * 设置内容置顶状态的请求参数。
 */
public record ContentPinReq(
        /** true 表示置顶，false 表示取消置顶。 */
        boolean pinned
) {
}
