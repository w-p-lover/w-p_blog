package com.ican.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @InjectMocks
    private RedisServiceImpl redisService;

    @Test
    void setZsetScore_shouldWriteExactScore() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        redisService.setZsetScore("blog:article:hot_score", 1, 88.5);

        verify(zSetOperations).add("blog:article:hot_score", 1, 88.5);
    }

    @Test
    void zReverseRangeWithScore_shouldKeepRedisOrder() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        Set<ZSetOperations.TypedTuple<Object>> tuples = new LinkedHashSet<>(List.of(
                new TestTuple(2, 91.0),
                new TestTuple(1, 88.5)
        ));
        when(zSetOperations.reverseRangeWithScores("blog:article:hot_score", 0, 1)).thenReturn(tuples);

        Map<Object, Double> result = redisService.zReverseRangeWithScore("blog:article:hot_score", 0, 1);

        assertThat(new ArrayList<>(result.keySet())).containsExactly(2, 1);
        assertThat(result).containsEntry(2, 91.0).containsEntry(1, 88.5);
    }

    private record TestTuple(Object value, Double score) implements ZSetOperations.TypedTuple<Object> {

        @Override
        public int compareTo(ZSetOperations.TypedTuple<Object> other) {
            return Double.compare(other.getScore(), score);
        }

        @Override
        public Object getValue() {
            return value;
        }

        @Override
        public Double getScore() {
            return score;
        }
    }
}
