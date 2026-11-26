package com.ican.service.impl;

import com.ican.config.RedisConfig;
import com.ican.mapper.ArticleMapper;
import com.ican.metrics.BlogMetrics;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ArticleViewServiceTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ArticleMapper mapper = mock(ArticleMapper.class);
    private final BlogMetrics metrics = mock(BlogMetrics.class);
    private final ArticleViewService service = new ArticleViewService(redis, mapper, metrics);

    @Test
    void atomicResultIsReturnedAndUnknownRecordIsNotRetriedOrWrittenToDb() {
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(112L).thenThrow(new QueryTimeoutException("reply lost")).thenReturn(null);
        assertEquals(112, service.recordView(1, 100));
        assertEquals(100, service.recordView(1, 100));
        assertEquals(100, service.recordView(1, 100));
        verify(redis, times(3)).execute(any(RedisScript.class), anyList(), any(Object[].class));
        verify(metrics, times(1)).incrementArticleView(1);
        verify(metrics, times(2)).recordArticleStatsFailure("view_record");
        verifyNoInteractions(mapper);
    }

    @Test
    void databaseFailureKeepsPendingAndDoesNotStopOtherSnapshotsThenRecovers() {
        ZSetOperations<String, String> values = mock(ZSetOperations.class);
        when(redis.opsForZSet()).thenReturn(values);
        when(values.zCard("article:view:pending")).thenReturn(1L, 0L);
        when(redis.execute(argThat((RedisScript<List> script) -> script != null && script.getResultType() == List.class),
                anyList(), eq("100"))).thenReturn(List.of("1", "101", "2", "42"), List.of("1", "101"));
        when(mapper.persistViewSnapshot(1, 101)).thenThrow(new QueryTimeoutException("DB unavailable")).thenReturn(1);
        when(redis.execute(argThat((RedisScript<Long> script) -> script != null && script.getResultType() == Long.class),
                anyList(), any(Object[].class))).thenReturn(1L);

        service.flushPendingViews();
        verify(mapper).persistViewSnapshot(2, 42);
        verify(redis, never()).execute(any(RedisScript.class), anyList(), eq("1"), eq("101"));
        service.flushPendingViews();
        verify(mapper, times(2)).persistViewSnapshot(1, 101);
        verify(redis).execute(any(RedisScript.class), anyList(), eq("1"), eq("101"));
        verify(metrics).setArticleViewsPending(0);
    }

    @Test
    void lostAckAfterDatabaseCommitReplaysSameAbsoluteSnapshot() {
        when(redis.execute(argThat((RedisScript<List> script) -> script != null && script.getResultType() == List.class),
                anyList(), eq("100"))).thenReturn(List.of("1", "101"));
        ZSetOperations<String, String> values = mock(ZSetOperations.class);
        when(redis.opsForZSet()).thenReturn(values);
        when(redis.execute(argThat((RedisScript<Long> script) -> script != null && script.getResultType() == Long.class),
                anyList(), eq("1"), eq("101")))
                .thenThrow(new QueryTimeoutException("ACK reply lost")).thenReturn(1L);

        service.flushPendingViews();
        service.flushPendingViews();

        var order = inOrder(mapper, redis);
        order.verify(redis).execute(any(RedisScript.class), anyList(), eq("100"));
        order.verify(mapper).persistViewSnapshot(1, 101);
        order.verify(redis).execute(any(RedisScript.class), anyList(), eq("1"), eq("101"));
        verify(mapper, times(2)).persistViewSnapshot(1, 101);
        verify(mapper, never()).incrementViews(any());
    }

    @Test
    void stringScriptMembersMatchExistingIntegerZsetSerialization() {
        var configured = new RedisConfig().redisTemplate(mock(RedisConnectionFactory.class));
        assertArrayEquals(((org.springframework.data.redis.serializer.RedisSerializer<Object>) configured.getValueSerializer()).serialize(1),
                new StringRedisSerializer().serialize("1"));
    }
}
