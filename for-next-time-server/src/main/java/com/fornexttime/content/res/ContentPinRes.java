package com.fornexttime.content.res;

/**
 * 设置置顶状态后返回的最终状态。
 */
public record ContentPinRes(
        /** 内容是否已置顶。 */
        boolean pinned
) {
}
