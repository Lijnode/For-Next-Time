package com.fornexttime.content.service;

import com.fornexttime.common.exception.BusinessException;
import com.fornexttime.common.exception.ErrorCode;
import com.fornexttime.common.security.JwtUser;
import com.fornexttime.common.security.UserContext;
import com.fornexttime.content.entity.Content;
import com.fornexttime.content.mapper.ContentMapper;
import com.fornexttime.content.mapper.ContentTagMapper;
import com.fornexttime.content.req.ContentCreateReq;
import com.fornexttime.tag.mapper.TagMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentServiceTest {

    @Mock
    private ContentMapper contentMapper;
    @Mock
    private TagMapper tagMapper;
    @Mock
    private ContentTagMapper contentTagMapper;
    @InjectMocks
    private ContentService contentService;

    @BeforeEach
    void bindCurrentUser() {
        UserContext.set(new JwtUser(1L, "admin"));
    }

    @AfterEach
    void clearCurrentUser() {
        UserContext.clear();
    }

    @Test
    void rejectsCreatingTheSameTrimmedContentForTheCurrentUser() {
        when(contentMapper.existsActiveByUserIdAndContent(1L, "1", null)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> contentService.create(new ContentCreateReq(" 1 ", null, List.of(), null)));

        assertEquals(ErrorCode.CONTENT_EXISTS, exception.getErrorCode());
        verify(contentMapper, never()).insert(any(Content.class));
    }

    @Test
    void rejectsEditingContentToAnotherExistingContent() {
        when(contentMapper.findDetail(1L, 8L)).thenReturn(new Content());
        when(contentMapper.existsActiveByUserIdAndContent(1L, "1", 8L)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> contentService.update(8L, new ContentCreateReq("1", null, List.of(), null)));

        assertEquals(ErrorCode.CONTENT_EXISTS, exception.getErrorCode());
        verify(contentMapper, never()).update(eq(8L), eq(1L), any(), any(), any());
    }
}
