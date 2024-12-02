package com.ican.aspect;

import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class CorsFilter implements Filter {

    // 定义允许的来源
    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
            "http://localhost:5173",
            "http://localhost:5174",
            "http://another-frontend.com"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        // 获取请求来源
        String origin = httpServletRequest.getHeader("Origin");

        // 检查是否是允许的来源
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            httpServletResponse.setHeader("Access-Control-Allow-Origin", origin); // 动态设置来源
            httpServletResponse.setHeader("Access-Control-Allow-Credentials", "true");
            httpServletResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
            httpServletResponse.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        }

        // 对于预检请求，直接返回成功响应
        if ("OPTIONS".equalsIgnoreCase(httpServletRequest.getMethod())) {
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(request, response);
    }
}
