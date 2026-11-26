package com.ican.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.ican.annotation.RateLimit;
import com.ican.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.UUID;

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

    private final StringRedisTemplate redisTemplate;
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
        if (rateLimit.perClient()) {
            key += clientKey();
        }
        int limit = rateLimit.limit();
        int period = rateLimit.period();

        // 执行 Lua 脚本进行限流判断
        Long result;
        try {
            // 脚本参数按字符串编码，避免通用 JSON 序列化器改变 Lua 参数。
            result = redisTemplate.execute(
                    rateLimitScript,
                    Collections.singletonList(key),
                    String.valueOf(limit),
                    String.valueOf(period),
                    UUID.randomUUID().toString()
            );
        } catch (DataAccessException e) {
            log.warn("限流依赖暂不可用: key={}", key, e);
            throw new ServiceException("访问保护暂不可用，请稍后重试");
        }

        if (result == null) {
            throw new ServiceException("访问保护暂不可用，请稍后重试");
        }
        if (result != 1) {
            log.warn("限流触发: key={}, limit={}/{} 秒", key, limit, period);
            throw new ServiceException(rateLimit.message());
        }

        return joinPoint.proceed();
    }

    private String clientKey() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (loginId != null) {
            return ":user:" + loginId;
        }
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            throw new ServiceException("无法识别访问来源");
        }
        // 只采用容器解析后的连接地址；不直接信任客户端伪造的转发头。
        return ":ip:" + attributes.getRequest().getRemoteAddr();
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
