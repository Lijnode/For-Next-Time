package com.fornexttime.content;

import lombok.Getter;

import java.util.Arrays;

/**
 * V1 固定内容分类编码及其中文展示名称。
 */
public enum CategoryCode {

    /** 搞笑。 */
    FUNNY("搞笑"),

    /** 抽象。 */
    ABSTRACT("抽象"),

    /** 感情。 */
    EMOTION("感情"),

    /** 文艺。 */
    LITERARY("文艺"),

    /** 治愈。 */
    HEALING("治愈"),

    /** 清醒。 */
    SOBER("清醒"),

    /** 阴阳。 */
    SARCASM("阴阳"),

    /** 其他。 */
    OTHER("其他");

    /** 与分类编码一一对应的中文展示名称。 */
    @Getter
    private final String label;

    /**
     * 创建携带中文展示名称的固定分类。
     *
     * @param label 分类中文展示名称
     */
    CategoryCode(String label) {
        // 1. 保存与枚举编码固定对应的中文名称。
        this.label = label;
    }

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
