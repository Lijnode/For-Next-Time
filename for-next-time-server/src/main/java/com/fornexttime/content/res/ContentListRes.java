package com.fornexttime.content.res;

import java.util.List;

public record ContentListRes(Long id, String content, String categoryCode, boolean pinned, int copyCount,
                             List<ContentTagRes> tags) {
}
