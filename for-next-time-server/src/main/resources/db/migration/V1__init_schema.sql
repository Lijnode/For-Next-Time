CREATE TABLE user_account (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt密码哈希',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_user_account_username (username)
) ENGINE=InnoDB
DEFAULT CHARACTER SET=utf8mb4
COLLATE=utf8mb4_0900_ai_ci
COMMENT='用户账号表';

CREATE TABLE content (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '内容ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '所属用户ID',
    content TEXT NOT NULL COMMENT '正文',
    category_code VARCHAR(32) DEFAULT NULL COMMENT '分类编码',
    remark VARCHAR(1000) DEFAULT NULL COMMENT '备注',
    is_pinned TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否置顶：0否 1是',
    copy_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '累计复制次数',
    last_copy_time DATETIME DEFAULT NULL COMMENT '最近成功复制时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近编辑时间',
    deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',

    PRIMARY KEY (id),
    KEY idx_content_user_list (user_id, deleted, is_pinned, update_time, id),
    KEY idx_content_user_category (user_id, deleted, category_code, update_time),
    KEY idx_content_user_last_copy (user_id, deleted, last_copy_time)
) ENGINE=InnoDB
DEFAULT CHARACTER SET=utf8mb4
COLLATE=utf8mb4_0900_ai_ci
COMMENT='用户内容表';

CREATE TABLE tag (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '标签ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '所属用户ID',
    name VARCHAR(20) NOT NULL COMMENT '标签名称',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_tag_user_name (user_id, name)
) ENGINE=InnoDB
DEFAULT CHARACTER SET=utf8mb4
COLLATE=utf8mb4_0900_ai_ci
COMMENT='用户标签表';

CREATE TABLE content_tag (
    content_id BIGINT UNSIGNED NOT NULL COMMENT '内容ID',
    tag_id BIGINT UNSIGNED NOT NULL COMMENT '标签ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (content_id, tag_id),
    KEY idx_content_tag_tag (tag_id, content_id)
) ENGINE=InnoDB
DEFAULT CHARACTER SET=utf8mb4
COLLATE=utf8mb4_0900_ai_ci
COMMENT='内容标签关系表';
