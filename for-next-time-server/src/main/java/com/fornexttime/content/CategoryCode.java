package com.fornexttime.content;

import java.util.Arrays;

public enum CategoryCode {
    FUNNY, ABSTRACT, EMOTION, LITERARY, HEALING, SOBER, SARCASM, OTHER;

    /**
     * 校验可选分类编码是否属于 V1 固定分类。
     */
    public static String normalize(String categoryCode) {
        // 1. 空白分类统一按未分类保存。
        if (categoryCode == null || categoryCode.isBlank()) {
            return null;
        }
        // 2. 仅允许固定枚举中的分类编码。
        return Arrays.stream(values())
                .map(Enum::name)
                .filter(categoryCode::equals)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("分类不存在"));
    }
}
