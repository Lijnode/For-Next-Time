ALTER TABLE content
    ADD KEY idx_content_user_hot (user_id, deleted, is_pinned, copy_count, update_time, id),
    ADD KEY idx_content_user_category_hot (user_id, deleted, category_code, is_pinned, copy_count, update_time, id);
