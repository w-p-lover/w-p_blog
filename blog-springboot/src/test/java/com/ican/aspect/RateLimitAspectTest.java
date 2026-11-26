package com.ican.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.ican.annotation.RateLimit;
import com.ican.exception.ServiceException;
import com.ican.service.impl.ArticleServiceImpl;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RateLimitAspectTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    private final RateLimitAspect aspect = new RateLimitAspect(redis);
    private RateLimit limit;

    @BeforeEach
    void setUp() throws Exception {
        aspect.init();
        var method = ArticleServiceImpl.class.getDeclaredMethod("getArticleHomeById", Integer.class);
        limit = method.getAnnotation(RateLimit.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[]{"articleId"});
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{1});
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void distinctLoggedInReadersUseSeparateBudgetsAndUniqueStringMembers() throws Throwable {
        List<List<String>> keys = new ArrayList<>();
        List<String> members = new ArrayList<>();
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenAnswer(call -> {
            keys.add(call.getArgument(1));
            assertEquals("10", call.getArgument(2));
            assertEquals("60", call.getArgument(3));
            members.add(call.getArgument(4));
            return 1L;
        });
        try (var auth = mockStatic(StpUtil.class)) {
            auth.when(StpUtil::getLoginIdDefaultNull).thenReturn(11, 12, 11);
            aspect.around(joinPoint, limit);
            aspect.around(joinPoint, limit);
            aspect.around(joinPoint, limit);
        }

        assertEquals(List.of("api:article:view:1:user:11"), keys.get(0));
        assertEquals(List.of("api:article:view:1:user:12"), keys.get(1));
        assertEquals(keys.get(0), keys.get(2));
        assertEquals(3, members.stream().distinct().count());
        verify(joinPoint, times(3)).proceed();
    }

    @Test
    void anonymousBudgetUsesRemoteAddressAndIgnoresSpoofedHeaders() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.1");
        request.addHeader("X-Forwarded-For", "198.51.100.2");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(1L);
        try (var auth = mockStatic(StpUtil.class)) {
            auth.when(StpUtil::getLoginIdDefaultNull).thenReturn(null);
            aspect.around(joinPoint, limit);
        }

        verify(redis).execute(any(RedisScript.class), eq(List.of("api:article:view:1:ip:192.0.2.1")),
                eq("10"), eq("60"), anyString());
    }

    @Test
    void exhaustedOrUnknownBudgetNeverExecutesArticleRead() throws Throwable {
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(0L, null, 1L);
        try (var auth = mockStatic(StpUtil.class)) {
            auth.when(StpUtil::getLoginIdDefaultNull).thenReturn(11);
            assertThrows(ServiceException.class, () -> aspect.around(joinPoint, limit));
            assertThrows(ServiceException.class, () -> aspect.around(joinPoint, limit));
            aspect.around(joinPoint, limit);
        }
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    void redisFailureDoesNotBypassProtectionAndNextRequestCanRecover() throws Throwable {
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new QueryTimeoutException("controlled timeout")).thenReturn(1L);
        try (var auth = mockStatic(StpUtil.class)) {
            auth.when(StpUtil::getLoginIdDefaultNull).thenReturn(11);
            assertThrows(ServiceException.class, () -> aspect.around(joinPoint, limit));
            verify(joinPoint, never()).proceed();
            aspect.around(joinPoint, limit);
        }
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    void resourceScopedAnnotationRetainsItsOriginalScope() throws Throwable {
        RateLimit resourceLimit = Endpoint.class.getDeclaredMethod("write").getAnnotation(RateLimit.class);
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(1L);

        aspect.around(joinPoint, resourceLimit);

        verify(redis).execute(any(RedisScript.class), eq(List.of("ai:write")),
                eq("20"), eq("3600"), anyString());
    }

    static class Endpoint {
        @RateLimit(key = "ai:write", limit = 20, period = 3600)
        void write() {
        }
    }
}
