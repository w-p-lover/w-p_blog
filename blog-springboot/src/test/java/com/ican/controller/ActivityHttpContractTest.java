package com.ican.controller;

import cn.dev33.satoken.spring.SaBeanInject;
import cn.dev33.satoken.spring.SaBeanRegister;
import cn.dev33.satoken.spring.SaTokenContextRegister;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ican.aspect.RateLimitAspect;
import com.ican.config.BlogRuntimeProperties;
import com.ican.handler.GlobalExceptionHandler;
import com.ican.interceptor.AccessLimitInterceptor;
import com.ican.model.vo.ActivityRegistrationVO;
import com.ican.model.vo.AdminActivityRegistrationVO;
import com.ican.entity.ActivityRegistration;
import com.ican.model.vo.PageResult;
import com.ican.service.RedisService;
import com.ican.service.UserService;
import com.ican.service.impl.ActivityRegistrationExecutionService;
import com.ican.service.impl.ActivityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.annotation.DirtiesContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real loopback HTTP/filter/interceptor/AOP; business services, permissions and Redis are controlled. */
@SpringBootTest(classes = ActivityHttpContractTest.HttpConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.config.location=optional:classpath:/activity-http-test-no-config.yml",
        "blog.registration.enabled=true", "server.address=127.0.0.1", "spring.main.banner-mode=off",
        "sa-token.token-name=Authorization", "sa-token.token-prefix=Bearer", "sa-token.is-log=false"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ActivityHttpContractTest {
    @LocalServerPort int port;
    @Autowired ActivityService activities;
    @Autowired ActivityRegistrationExecutionService execution;
    @Autowired StringRedisTemplate redis;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private String reader;
    private String admin;
    private static final String ID = "12345678-1234-1234-1234-123456789abc";

    @BeforeEach
    void prepare() {
        reset(activities, execution, redis);
        reader = StpUtil.getStpLogic().createLoginSession(7);
        admin = StpUtil.getStpLogic().createLoginSession(1);
        when(activities.list(false, 1, 20)).thenReturn(new PageResult<>(List.of(), 0L));
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(1L);
    }

    @Test
    void realHttpEnforcesIdentityPermissionsValidationAndPendingContract() throws Exception {
        assertTrue(call("GET", "/activities", null, null).get("flag").asBoolean());
        assertEquals(402, call("POST", "/activities/1/registration", null, "{\"requestId\":\"" + ID + "\"}").get("code").asInt());
        assertFalse(call("GET", "/admin/activities", reader, null).get("flag").asBoolean());
        verify(activities, never()).list(eq(true), anyInt(), anyInt());
        var pending = new ActivityRegistrationVO();
        pending.setRequestId(ID); pending.setActivityId(1); pending.setStatus("PENDING");
        when(activities.submit(1, 7, ID)).thenReturn(pending);
        JsonNode accepted = call("POST", "/activities/1/registration", reader,
                "{\"requestId\":\"" + ID + "\",\"userId\":999}");
        assertEquals("PENDING", accepted.path("data").path("status").asText());
        verify(activities).submit(1, 7, ID); // Client identity cannot override the token.
        verify(redis).execute(any(RedisScript.class), eq(List.of("api:activity:register:1")),
                eq("100"), eq("1"), anyString());
        assertEquals(400, call("POST", "/activities/1/registration", reader, "{\"requestId\":\"bad\"}").get("code").asInt());
        assertEquals(400, call("GET", "/activities/0", null, null).get("code").asInt());
        assertEquals(400, call("POST", "/admin/activities/registrations/1-1-1-1-1/retry", admin, null).get("code").asInt());
        verifyNoInteractions(execution);
        when(execution.retry(ID)).thenReturn(pending);
        assertEquals("PENDING", call("POST", "/admin/activities/registrations/" + ID.toUpperCase() + "/retry", admin, null)
                .path("data").path("status").asText());
        verify(execution).retry(ID); // Canonical identity is retained for authorized recovery.
    }

    @Test
    void malformedPathsAndJsonAreParameterErrorsBeforeBusinessWrites() throws Exception {
        assertEquals(400, call("GET", "/activities/not-an-integer", null, null).get("code").asInt());
        JsonNode invalidJson = call("POST", "/activities/1/registration", reader, "{broken-json");
        assertEquals(400, invalidJson.get("code").asInt());
        assertEquals("请求参数不合法", invalidJson.get("msg").asText());
        assertEquals(400, call("POST", "/activities/1/registration", reader,
                "{\"requestId\":{\"private-input\":1}}").get("code").asInt());
        verify(activities, never()).submit(anyInt(), anyInt(), anyString());
        verifyNoInteractions(execution);
    }

    @Test
    void rateDenialDependencyFailureAndSystemErrorNeverReturnAcceptedOrLeakDetails() throws Exception {
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(0L);
        assertFalse(call("POST", "/activities/1/registration", reader, "{\"requestId\":\"" + ID + "\"}").get("flag").asBoolean());
        verify(activities, never()).submit(anyInt(), anyInt(), anyString());
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new RedisConnectionFailureException("synthetic-private-redis-host"));
        JsonNode unavailable = call("POST", "/activities/1/registration", reader, "{\"requestId\":\"" + ID + "\"}");
        assertEquals("访问保护暂不可用，请稍后重试", unavailable.get("msg").asText());
        verify(activities, never()).submit(anyInt(), anyInt(), anyString());
        when(activities.mine(1, 7)).thenThrow(new RuntimeException("synthetic-private-jdbc-password"));
        JsonNode failed = call("GET", "/activities/1/registration", reader, null);
        assertEquals("系统繁忙，请稍后重试", failed.get("msg").asText());
        assertFalse(failed.toString().contains("synthetic-private"));
    }

    @Test
    void diagnosticsAreAdminOnlyAndPublicResultsRetainTheirOriginalFields() throws Exception {
        var snapshot = new AdminActivityRegistrationVO();
        snapshot.setRequestId(ID); snapshot.setActivityId(1); snapshot.setStatus("PENDING");
        snapshot.setDispatchAttempts(2); snapshot.setFailureCount(1);
        snapshot.setNextDispatchAt(LocalDateTime.of(2026, 11, 26, 10, 1));
        snapshot.setFailureSummary("确认事务失败或结果未知，按当前状态恢复");
        when(activities.registrations(1, "PENDING", 1, 20)).thenReturn(new PageResult<>(List.of(snapshot), 1L));
        assertFalse(call("GET", "/admin/activities/1/registrations?status=PENDING", reader, null).path("flag").asBoolean());
        verify(activities, never()).registrations(anyInt(), any(), anyInt(), anyInt());
        JsonNode diagnostics = call("GET", "/admin/activities/1/registrations?status=PENDING", admin, null)
                .path("data").path("recordList").get(0);
        assertEquals(2, diagnostics.path("dispatchAttempts").asInt());
        assertEquals(1, diagnostics.path("failureCount").asInt());
        assertFalse(diagnostics.path("nextDispatchAt").isNull());
        assertFalse(diagnostics.has("dispatchToken"));
        assertFalse(diagnostics.has("lastError"));

        var persistent = new ActivityRegistration();
        persistent.setRequestId(ID); persistent.setActivityId(1); persistent.setStatus("PENDING");
        persistent.setDispatchAttempts(2); persistent.setFailureCount(1);
        persistent.setDispatchToken("private-token"); persistent.setLastError("private-db-context");
        persistent.setNextDispatchAt(snapshot.getNextDispatchAt());
        var publicResult = ActivityRegistrationVO.from(persistent);
        when(activities.mine(1, 7)).thenReturn(publicResult);
        when(activities.submit(1, 7, ID)).thenReturn(publicResult);
        when(execution.cancel(1, 7)).thenReturn(publicResult);
        when(execution.retry(ID)).thenReturn(publicResult);
        for (JsonNode response : List.of(call("GET", "/activities/1/registration", reader, null),
                call("POST", "/activities/1/registration", reader, "{\"requestId\":\"" + ID + "\"}"),
                call("DELETE", "/activities/1/registration", reader, null),
                call("POST", "/admin/activities/registrations/" + ID + "/retry", admin, null))) {
            JsonNode data = response.path("data");
            assertEquals(ID, data.path("requestId").asText());
            assertEquals(5, data.size());
            for (String internal : List.of("dispatchAttempts", "failureCount", "nextDispatchAt", "failureSummary", "dispatchToken", "lastError", "userId")) {
                assertFalse(data.has(internal), internal);
            }
        }
    }

    private JsonNode call(String method, String path, String token, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(5));
        if (token != null) request.header("Authorization", "Bearer " + token);
        request.header("Content-Type", "application/json");
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        var response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());
        return json.readTree(response.body());
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({ServletWebServerFactoryAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
            WebMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class, JacksonAutoConfiguration.class,
            ValidationAutoConfiguration.class, AopAutoConfiguration.class})
    @Import({ActivityController.class, GlobalExceptionHandler.class, RateLimitAspect.class,
            com.ican.satoken.SaTokenConfig.class, AccessLimitInterceptor.class,
            SaBeanRegister.class, SaBeanInject.class, SaTokenContextRegister.class})
    static class HttpConfig {
        @Bean ActivityService activities() { return mock(ActivityService.class); }
        @Bean ActivityRegistrationExecutionService execution() { return mock(ActivityRegistrationExecutionService.class); }
        @Bean StringRedisTemplate redis() { return mock(StringRedisTemplate.class); }
        @Bean RedisService legacyRedis() { return mock(RedisService.class); }
        @Bean UserService users() { return mock(UserService.class); }
        @Bean BlogRuntimeProperties properties() { return new BlogRuntimeProperties(); }
        @Bean StpInterface permissions() {
            return new StpInterface() {
                public List<String> getPermissionList(Object id, String type) {
                    return "1".equals(id.toString()) ? List.of("activity:event:list", "activity:registration:retry", "activity:registration:list") : List.of();
                }
                public List<String> getRoleList(Object id, String type) { return List.of(); }
            };
        }
    }
}
