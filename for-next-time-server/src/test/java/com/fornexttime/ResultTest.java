package com.fornexttime;

import com.fornexttime.common.model.PageResult;
import com.fornexttime.common.model.Result;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultTest {

    @Test
    void createsStandardSuccessAndEmptyPageResponses() {
        Result<String> result = Result.success("ok");
        PageResult<String> page = PageResult.of(1, 20, 0, List.of());

        assertEquals(0, result.code());
        assertEquals("success", result.message());
        assertEquals("ok", result.data());
        assertEquals(0, page.pages());
        assertTrue(page.list().isEmpty());
    }
}
