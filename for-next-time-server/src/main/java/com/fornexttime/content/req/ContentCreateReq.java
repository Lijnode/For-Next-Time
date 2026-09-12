package com.fornexttime.content.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 新增内容请求参数；编辑内容时复用相同字段模型。
 */
public record ContentCreateReq(
    /** 用户保存的内容正文。 */
    @NotBlank(message = "内容正文不能为空")
    @Size(max = 5000, message = "内容正文长度超限")
    String content,

        /** 固定分类编码，取值见 CategoryCode；未选择分类时为空。 */
        String categoryCode,

    /** 用户输入的标签名称列表，最多十个。 */
    @Size(max = 10, message = "标签数量超过限制")
    List<String> tagNames,

    /** 与正文配套的补充备注。 */
    @Size(max = 1000, message = "备注长度超限")
    String remark
) {
}
