package com.ican.satoken;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaHttpMethod;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import cn.hutool.json.JSONUtil;
import com.ican.config.BlogRuntimeProperties;
import com.ican.interceptor.AccessLimitInterceptor;
import com.ican.interceptor.PageableInterceptor;
import com.ican.model.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static com.ican.enums.StatusCodeEnum.UNAUTHORIZED;

/**
 * SaToken配置
 *
 * @author xcs
 * @date 2022/11/28 22:12
 **/
@Component
@RequiredArgsConstructor
public class SaTokenConfig implements WebMvcConfigurer {

    private final AccessLimitInterceptor accessLimitInterceptor;
    private final BlogRuntimeProperties blogRuntimeProperties;

    private final String[] EXCLUDE_PATH_PATTERNS = {
            "/swagger-resources",
            "/webjars/**",
            "/v2/api-docs",
            "/doc.html",
            "/favicon.ico",
            "/oauth/*",
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new PageableInterceptor());
        registry.addInterceptor(accessLimitInterceptor);
        registry.addInterceptor(new SaInterceptor()).addPathPatterns("/**");
    }

    @Bean
    public SaServletFilter getSaServletFilter() {
        return new SaServletFilter()
                .addInclude("/**")
                .addExclude(EXCLUDE_PATH_PATTERNS)
                .setBeforeAuth(obj -> {
                    SaHolder.getResponse()
                            .setHeader("Access-Control-Allow-Methods", "*")
                            .setHeader("Access-Control-Allow-Credentials", "true")
                            .setHeader("Access-Control-Max-Age", "3600")
                            .setHeader("Access-Control-Allow-Headers", "*");

                    String origin = SaHolder.getRequest().getHeader("Origin");
                    if (origin != null && blogRuntimeProperties.getSecurity().getAllowedOrigins().contains(origin)) {
                        SaHolder.getResponse().setHeader("Access-Control-Allow-Origin", origin);
                    }

                    SaRouter.match(SaHttpMethod.OPTIONS)
                            .free(r -> System.out.println("--------OPTIONS预检请求，不做处理"))
                            .back();
                })
                .setAuth(obj -> {
                    SaRouter.match("/admin/**").check(r -> StpUtil.checkLogin());
                    long renewThresholdSeconds = blogRuntimeProperties.getSatoken().getRenewThresholdSeconds();
                    long renewTimeoutSeconds = blogRuntimeProperties.getSatoken().getRenewTimeoutSeconds();
                    if (StpUtil.getTokenTimeout() < renewThresholdSeconds) {
                        StpUtil.renewTimeout(renewTimeoutSeconds);
                    }
                    SaManager.getLog().debug("----- 请求path={}  提交token={}", SaHolder.getRequest().getRequestPath(), StpUtil.getTokenValue());
                })
                .setError(e -> {
                    SaHolder.getResponse().setHeader("Content-Type", "application/json;charset=UTF-8");
                    if (e instanceof NotLoginException) {
                        return JSONUtil.toJsonStr(Result.fail(UNAUTHORIZED.getCode(), UNAUTHORIZED.getMsg()));
                    }
                    return SaResult.error(e.getMessage());
                });
    }

}
