package com.fornexttime.content.entity;

import lombok.Data;

/**
 * 用户保存内容的数据库实体。
 */
@Data
public class Content {

    /** 内容主键。 */
    private Long id;

    /** 所属用户主键，仅用于服务端数据隔离。 */
    private Long userId;

    /** 用户保存的正文。 */
    private String content;

    /** 固定分类编码，取值见 CategoryCode；未分类时为空。 */
    private String categoryCode;

    /** 用户填写的补充备注，可为空。 */
    private String remark;

    /** 是否置顶。 */
    private boolean pinned;

    /** 已成功复制的次数。 */
    private int copyCount;
}
