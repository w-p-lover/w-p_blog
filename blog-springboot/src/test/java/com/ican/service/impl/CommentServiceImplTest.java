package com.ican.service.impl;

import com.ican.mapper.ArticleMapper;
import com.ican.mapper.CommentMapper;
import com.ican.mapper.UserMapper;
import com.ican.mapper.TalkMapper;
import com.ican.entity.Talk;
import com.ican.model.dto.CommentDTO;
import com.ican.service.RedisService;
import com.ican.service.SiteConfigService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommentServiceImplTest {
    @Test
    void unsupportedCommentTypeIsRejectedBeforeReadingOrWriting() {
        var articles = mock(ArticleMapper.class);
        var comments = mock(CommentMapper.class);
        var talks = mock(TalkMapper.class);
        var users = mock(UserMapper.class);
        var site = mock(SiteConfigService.class);
        var redis = mock(RedisService.class);
        var rabbit = mock(RabbitTemplate.class);
        var service = new CommentServiceImpl(articles, talks, comments, users, site, redis, rabbit);
        var request = new CommentDTO(1, 4, null, null, null, "无效类型请求");

        assertThrows(IllegalArgumentException.class, () -> service.addComment(request));
        verifyNoInteractions(articles, talks, comments, users, site, redis, rabbit);
    }

    @Test
    void validationAcceptsArticleFriendAndTalkAndRejectsUnsupportedType() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(new CommentDTO(1, 1, null, null, null, "文章评论")).isEmpty());
            assertTrue(validator.validate(new CommentDTO(null, 2, null, null, null, "友链评论")).isEmpty());
            assertTrue(validator.validate(new CommentDTO(1, 3, null, null, null, "说说评论")).isEmpty());
            assertFalse(validator.validate(new CommentDTO(1, 4, null, null, null, "无效类型请求")).isEmpty());
        }
    }

    @Test
    void talkCommentChecksTheTalkInsteadOfTheArticle() {
        var articles = mock(ArticleMapper.class);
        var talks = mock(TalkMapper.class);
        var comments = mock(CommentMapper.class);
        var service = new CommentServiceImpl(articles, talks, comments, mock(UserMapper.class),
                mock(SiteConfigService.class), mock(RedisService.class), mock(RabbitTemplate.class));
        when(talks.selectOne(any())).thenReturn(Talk.builder().id(7).build());
        ReflectionTestUtils.invokeMethod(service, "verifyComment", new CommentDTO(7, 3, null, null, null, "说说评论"));
        verify(talks).selectOne(any());
        verifyNoInteractions(articles, comments);
    }
}
