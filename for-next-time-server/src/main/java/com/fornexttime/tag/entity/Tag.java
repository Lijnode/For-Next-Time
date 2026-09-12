package com.fornexttime.tag.entity;

import lombok.Data;

/**
 * 用户自定义标签的数据库实体。
 */
@Data
public class Tag {

    /** 标签主键。 */
    private Long id;

    /** 标签所属用户主键。 */
    private Long userId;

    /** 经清洗后保存的标签名称。 */
    private String name;
}
