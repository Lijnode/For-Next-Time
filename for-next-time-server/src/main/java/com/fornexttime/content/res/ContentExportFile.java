package com.fornexttime.content.res;

/**
 * 内容导出接口生成的下载文件数据。
 */
public record ContentExportFile(
        /** 建议下载时使用的 UTF-8 文件名。 */
        String fileName,

        /** UTF-8 编码前的 TXT 文本内容。 */
        String text
) {
}
