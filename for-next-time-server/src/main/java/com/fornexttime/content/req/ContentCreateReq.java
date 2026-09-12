package com.fornexttime.content.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ContentCreateReq(
        @NotBlank(message = "内容正文不能为空")
        @Size(max = 5000, message = "内容正文长度超限") String content,
        String categoryCode,
        @Size(max = 10, message = "标签数量超过限制") List<String> tagNames,
        @Size(max = 1000, message = "备注长度超限") String remark
) {
}
