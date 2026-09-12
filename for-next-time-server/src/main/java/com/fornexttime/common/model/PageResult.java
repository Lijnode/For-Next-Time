package com.fornexttime.common.model;

import java.util.List;

public record PageResult<T>(int pageNum, int pageSize, long total, int pages, List<T> list) {

    public static <T> PageResult<T> of(int pageNum, int pageSize, long total, List<T> list) {
        int pages = total == 0 ? 0 : (int) Math.ceil((double) total / pageSize);
        return new PageResult<>(pageNum, pageSize, total, pages, list);
    }
}
