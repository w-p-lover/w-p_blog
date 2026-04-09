package com.ican.aspect;

import com.ican.annotation.RateLimit;
import com.ican.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.Collections;

/**
 * 限流切面
 * 基于 Redis + Lua 脚本实现滑动窗口限流
 *
 * @author ican
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RedisTemplate<String, Object> redisTemplate;
    private DefaultRedisScript<Long> rateLimitScript;
    private final ExpressionParser parser = new SpelExpressionParser();

    @PostConstruct
    public void init() {
        rateLimitScript = new DefaultRedisScript<>();
        rateLimitScript.setResultType(Long.class);
        rateLimitScript.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("lua/rate_limit.lua")));
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 解析限流 key（支持 SpEL 表达式）
        String key = parseKey(rateLimit.key(), joinPoint);
        int limit = rateLimit.limit();
        int period = rateLimit.period();

        // 执行 Lua 脚本进行限流判断
        Long result = redisTemplate.execute(
                rateLimitScript,
                Collections.singletonList(key),
                limit,
                period,
                System.currentTimeMillis()
        );

        if (result != null && result == 0) {
            log.warn("限流触发: key={}, limit={}/{} 秒", key, limit, period);
            throw new ServiceException(rateLimit.message());
        }

        return joinPoint.proceed();
    }

    /**
     * 解析限流 key（支持 SpEL 表达式）
     */
    private String parseKey(String key, ProceedingJoinPoint joinPoint) {
        if (!key.contains("#")) {
            return key;
        }

        // 获取方法参数
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        // 构建 SpEL 上下文
        EvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }

        // 使用模板表达式模式，支持 "api:article:view:#{#articleId}" 这类混合字面量+SpEL
        return parser.parseExpression(key, ParserContext.TEMPLATE_EXPRESSION)
                .getValue(context, String.class);
    }
}
