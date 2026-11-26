package com.ican.controller;

import cn.dev33.satoken.spring.SaBeanInject;
import cn.dev33.satoken.spring.SaBeanRegister;
import cn.dev33.satoken.spring.SaTokenContextRegister;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ican.aspect.*;
import com.ican.config.*;
import com.ican.consumer.ActivityRegistrationConsumer;
import com.ican.handler.GlobalExceptionHandler;
import com.ican.handler.MyMetaObjectHandler;
import com.ican.interceptor.AccessLimitInterceptor;
import com.ican.mapper.*;
import com.ican.satoken.StpInterfaceImpl;
import com.ican.service.RedisService;
import com.ican.service.UserService;
import com.ican.service.impl.*;
import com.ican.utils.SpringUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/** Opt-in activity application slice. Real HTTP/MySQL/MQ/Redis/auth/logging, synthetic identities only. */
@EnabledIfEnvironmentVariable(named = "BLOG_ACTIVITY_SCHEMA", matches = "blog_activity_[0-9a-f]{32}")
@SpringBootTest(classes = ActivityRegistrationHttpIntegrationTest.Config.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.config.location=optional:classpath:/activity-http-test-no-config.yml",
        "blog.registration.enabled=true", "server.address=127.0.0.1", "spring.main.banner-mode=off",
        "sa-token.token-name=Authorization", "sa-token.token-prefix=Bearer", "sa-token.is-log=false",
        "spring.rabbitmq.publisher-confirm-type=correlated", "spring.rabbitmq.publisher-returns=true",
        "spring.rabbitmq.listener.simple.auto-startup=false"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ActivityRegistrationHttpIntegrationTest {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired StringRedisTemplate redis;
    @Autowired RabbitListenerEndpointRegistry listeners;
    @Autowired ActivityRegistrationDispatchService dispatcher;
    @Autowired ScheduledExecutorService scheduledExecutorService;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    static String required(String name) { return Objects.requireNonNull(System.getenv(name), name); }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry properties) {
        for (String suffix : List.of("host", "port", "username", "password")) {
            String env = suffix.equals("username") ? "USER" : suffix.toUpperCase();
            properties.add("spring.rabbitmq." + suffix, () -> required("BLOG_ACTIVITY_MQ_" + env));
        }
        properties.add("spring.rabbitmq.virtual-host", () -> required("BLOG_ACTIVITY_SCHEMA"));
        properties.add("spring.data.redis.host", () -> required("BLOG_ACTIVITY_REDIS_HOST"));
        properties.add("spring.data.redis.port", () -> required("BLOG_ACTIVITY_REDIS_PORT"));
        properties.add("spring.data.redis.password", () -> required("BLOG_ACTIVITY_REDIS_PASSWORD"));
        properties.add("spring.data.redis.database", () -> required("BLOG_ACTIVITY_REDIS_DATABASE"));
        properties.add("spring.data.redis.timeout", () -> "3s");
        properties.add("spring.data.redis.connect-timeout", () -> "3s");
    }

    @Test
    void realHttpAdmissionConfirmationCancellationRecoveryAndAuditFailureKeepPersistentFacts() throws Exception {
        assertEquals(required("BLOG_ACTIVITY_SCHEMA"), jdbc.queryForObject("SELECT DATABASE()", String.class));
        seedPermissions();
        // Synthetic session creation exercises the real Redis DAO; password/OAuth login is outside this slice.
        String admin = StpUtil.getStpLogic().createLoginSession(1);
        String reader = StpUtil.getStpLogic().createLoginSession(7);
        String other = StpUtil.getStpLogic().createLoginSession(8);
        String recovering = StpUtil.getStpLogic().createLoginSession(9);
        assertEquals(402, call("POST", "/admin/activities", null, "{}").path("code").asInt());
        assertFalse(call("GET", "/admin/activities", reader, null).path("flag").asBoolean());
        LocalDateTime start = LocalDateTime.now(ZoneId.of("Asia/Shanghai")).plusMinutes(5).withNano(0);
        String body = json.writeValueAsString(Map.of("title", "Synthetic HTTP activity", "description", "Isolated only",
                "capacity", 1, "startsAt", start.toString(), "endsAt", start.plusHours(1).toString()));
        JsonNode created = success("POST", "/admin/activities", admin, body).path("data");
        int id = created.path("id").asInt(); assertTrue(id > 0);
        success("PUT", "/admin/activities/" + id, admin, body);
        success("POST", "/admin/activities/" + id + "/publish", admin, null);
        assertEquals(id, success("GET", "/activities/" + id, null, null).path("data").path("id").asInt());
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertTrue(count("t_operation_log") >= 3));
        // Real SQL role/menu mappings have populated serialized Redis sessions, not a permission stub.
        assertEquals(List.of("synthetic-admin"), StpUtil.getSessionByLoginId(1).get("Role_List"));
        assertFalse(call("POST", "/admin/activities/" + id + "/publish", admin, null).path("flag").asBoolean());
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertTrue(count("t_exception_log") >= 1));
        jdbc.update("UPDATE t_activity SET starts_at=TIMESTAMPADD(MINUTE,-1,TIMESTAMPADD(HOUR,8,UTC_TIMESTAMP(6))) WHERE id=?", id);

        String requestId = UUID.randomUUID().toString();
        String registration = "/activities/" + id + "/registration";
        assertEquals("PENDING", success("POST", registration, reader, request(requestId)).path("data").path("status").asText());
        assertEquals(requestId, success("POST", registration, reader, request(UUID.randomUUID().toString()))
                .path("data").path("requestId").asText());
        assertEquals("PENDING", success("GET", registration, reader, null).path("data").path("status").asText());
        assertTrue(redis.opsForZSet().zCard("api:activity:register:" + id) > 0);
        listeners.start();
        waitStatus(registration, reader, "CONFIRMED");
        success("POST", registration, other, request(UUID.randomUUID().toString()));
        waitStatus(registration, other, "SOLD_OUT");
        success("DELETE", registration, reader, null); success("DELETE", registration, reader, null);
        assertEquals(1, jdbc.queryForObject("SELECT remaining FROM t_activity WHERE id=?", Integer.class, id));
        assertEquals("CANCELED", success("POST", registration, reader, request(UUID.randomUUID().toString()))
                .path("data").path("status").asText());

        // Force only this isolated request's DB terminal write to fail; the actual consumer records failures.
        jdbc.execute("CREATE TRIGGER reject_http_terminal BEFORE UPDATE ON t_activity_registration FOR EACH ROW "
                + "BEGIN IF NEW.status='CONFIRMED' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='synthetic terminal failure'; END IF; END");
        String failed = UUID.randomUUID().toString(); success("POST", registration, recovering, request(failed));
        for (int attempt = 1; attempt <= 3; attempt++) {
            final int expected = attempt;
            await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertEquals(expected,
                    jdbc.queryForObject("SELECT failure_count FROM t_activity_registration WHERE request_id=?", Integer.class, failed)));
            if (attempt < 3) {
                jdbc.update("UPDATE t_activity_registration SET next_dispatch_at=TIMESTAMPADD(SECOND,-1,TIMESTAMPADD(HOUR,8,UTC_TIMESTAMP(6))) WHERE request_id=?", failed);
                dispatcher.recoverPending();
            }
        }
        waitStatus(registration, recovering, "FAILED");
        jdbc.execute("DROP TRIGGER reject_http_terminal");
        assertFalse(call("POST", "/admin/activities/registrations/" + failed + "/retry", reader, null).path("flag").asBoolean());
        assertEquals(failed, success("POST", "/admin/activities/registrations/" + failed + "/retry", admin, null)
                .path("data").path("requestId").asText());
        waitStatus(registration, recovering, "CONFIRMED");
        assertEquals(0, jdbc.queryForObject("SELECT remaining FROM t_activity WHERE id=?", Integer.class, id));
        assertEquals(0, jdbc.queryForObject("SELECT pending_count FROM t_activity WHERE id=?", Integer.class, id));
        assertEquals(3, count("t_activity_registration"));
        success("GET", "/admin/activities/" + id + "/registrations?status=CONFIRMED", admin, null);

        // Corrupt only our namespaced limiter key: real Lua fails and no new durable request is admitted.
        redis.delete("api:activity:register:" + id);
        redis.opsForValue().set("api:activity:register:" + id, "synthetic-wrong-type", Duration.ofSeconds(10));
        JsonNode protectedFailure = call("POST", registration, other, request(UUID.randomUUID().toString()));
        assertEquals("访问保护暂不可用，请稍后重试", protectedFailure.path("msg").asText());
        assertEquals(3, count("t_activity_registration")); redis.delete("api:activity:register:" + id);
        assertEquals("SOLD_OUT", success("POST", registration, other, request(UUID.randomUUID().toString()))
                .path("data").path("status").asText());

        // A stopped logging executor must not turn a committed new draft into a failed HTTP response.
        scheduledExecutorService.shutdown();
        int before = count("t_activity");
        assertTrue(success("POST", "/admin/activities", admin, body).path("data").path("id").asInt() > 0);
        assertEquals(before + 1, count("t_activity"));
        JsonNode original = call("POST", "/admin/activities/" + id + "/publish", admin, null);
        assertEquals("仅能发布尚未开始的草稿活动", original.path("msg").asText());
    }

    private void seedPermissions() {
        jdbc.update("INSERT INTO t_role(id,role_name,create_time) VALUES ('synthetic-admin','Synthetic',UTC_TIMESTAMP())");
        jdbc.update("INSERT INTO t_user_role(user_id,role_id) VALUES (1,'synthetic-admin')");
        int id = 0;
        for (String permission : List.of("activity:event:list", "activity:event:add", "activity:event:update",
                "activity:event:publish", "activity:registration:list", "activity:registration:retry")) {
            jdbc.update("INSERT INTO t_menu(id,menu_type,menu_name,perms,create_time) VALUES (?,'F','Synthetic',?,UTC_TIMESTAMP())", ++id, permission);
            jdbc.update("INSERT INTO t_role_menu(role_id,menu_id) VALUES ('synthetic-admin',?)", id);
        }
    }
    private int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }
    private String request(String id) { return "{\"requestId\":\"" + id + "\"}"; }
    private void waitStatus(String path, String token, String status) {
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertEquals(status,
                success("GET", path, token, null).path("data").path("status").asText()));
    }
    private JsonNode success(String method, String path, String token, String body) throws Exception {
        JsonNode result = call(method, path, token, body);
        assertTrue(result.path("flag").asBoolean(), result.toString()); return result;
    }
    private JsonNode call(String method, String path, String token, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(10));
        request.header("Content-Type", "application/json").header("User-Agent", "SyntheticActivityIntegration/1.0");
        if (token != null) request.header("Authorization", "Bearer " + token);
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        var response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body()); return json.readTree(response.body());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableRabbit
    @EnableTransactionManagement
    @ImportAutoConfiguration({ServletWebServerFactoryAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
            WebMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class, JacksonAutoConfiguration.class,
            ValidationAutoConfiguration.class, AopAutoConfiguration.class, RabbitAutoConfiguration.class, RedisAutoConfiguration.class})
    @Import({ActivityController.class, GlobalExceptionHandler.class, RateLimitAspect.class, OptLogAspect.class, ExceptionLogAspect.class,
            com.ican.satoken.SaTokenConfig.class, AccessLimitInterceptor.class, StpInterfaceImpl.class,
            SaBeanRegister.class, SaBeanInject.class, SaTokenContextRegister.class, SaTokenDaoRedis.class, RedisConfig.class,
            ActivityRegistrationConfig.class, ActivityService.class, ActivityRegistrationExecutionService.class,
            ActivityRegistrationDispatchService.class, ActivityRegistrationConsumer.class,
            OperationLogServiceImpl.class, ExceptionLogServiceImpl.class, SpringUtils.class})
    static class Config {
        @Bean HikariDataSource dataSource() {
            String schema = required("BLOG_ACTIVITY_SCHEMA"); String url = required("BLOG_ACTIVITY_JDBC_URL");
            assertTrue(schema.matches("blog_activity_[0-9a-f]{32}"));
            assertEquals("/" + schema, URI.create(url.substring(5)).getPath());
            var pool = new HikariConfig(); pool.setJdbcUrl(url.replace("/" + schema + "?", "/information_schema?"));
            pool.setUsername(required("BLOG_ACTIVITY_DB_USER")); pool.setPassword(required("BLOG_ACTIVITY_DB_PASSWORD"));
            pool.setCatalog(schema); pool.setMaximumPoolSize(4); pool.setMinimumIdle(1); pool.setConnectionTimeout(5000);
            return new HikariDataSource(pool);
        }
        @Bean JdbcTemplate jdbc(HikariDataSource dataSource) { return new JdbcTemplate(dataSource); }
        @Bean DataSourceTransactionManager transactions(HikariDataSource dataSource) { return new DataSourceTransactionManager(dataSource); }
        @Bean SqlSessionTemplate sql(HikariDataSource dataSource) throws Exception {
            var factory = new MybatisSqlSessionFactoryBean(); var configuration = new MybatisConfiguration();
            configuration.setMapUnderscoreToCamelCase(true); factory.setConfiguration(configuration); factory.setDataSource(dataSource);
            factory.setGlobalConfig(new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig().setTablePrefix("t_"))
                    .setMetaObjectHandler(new MyMetaObjectHandler()));
            factory.setMapperLocations(new ClassPathResource("mapper/ActivityMapper.xml"), new ClassPathResource("mapper/MenuMapper.xml"),
                    new ClassPathResource("mapper/RoleMapper.xml"), new ClassPathResource("mapper/OperationLogMapper.xml"),
                    new ClassPathResource("mapper/ExceptionLogMapper.xml"));
            return new SqlSessionTemplate(factory.getObject());
        }
        @Bean ActivityMapper activities(SqlSessionTemplate sql) { return sql.getMapper(ActivityMapper.class); }
        @Bean MenuMapper menus(SqlSessionTemplate sql) { return sql.getMapper(MenuMapper.class); }
        @Bean RoleMapper roles(SqlSessionTemplate sql) { return sql.getMapper(RoleMapper.class); }
        @Bean OperationLogMapper operations(SqlSessionTemplate sql) { return sql.getMapper(OperationLogMapper.class); }
        @Bean ExceptionLogMapper exceptions(SqlSessionTemplate sql) { return sql.getMapper(ExceptionLogMapper.class); }
        @Bean MessageConverter messages() { return new Jackson2JsonMessageConverter(); }
        @Bean MeterRegistry metrics() { return new SimpleMeterRegistry(); }
        @Bean BlogRuntimeProperties runtime() { return new BlogRuntimeProperties(); }
        // Unused by activity endpoints: existing interceptor/exception-handler dependencies only.
        @Bean RedisService legacyRedis() { return mock(RedisService.class); }
        @Bean UserService users() { return mock(UserService.class); }
        @Bean(destroyMethod = "shutdownNow") ScheduledExecutorService scheduledExecutorService() {
            return new ScheduledThreadPoolExecutor(1,
                    r -> { var t = new Thread(r, "activity-test-audit"); t.setDaemon(true); return t; }) {
                @Override protected void afterExecute(Runnable task, Throwable error) {
                    super.afterExecute(task, error); com.ican.utils.ThreadUtils.printException(task, error);
                }
            };
        }
        @Bean static BeanPostProcessor isolatedRedisKeys() {
            String prefix = required("BLOG_ACTIVITY_SCHEMA") + ":";
            var serializer = new StringRedisSerializer() {
                @Override public byte[] serialize(String value) { return super.serialize(value == null ? null : prefix + value); }
                @Override public String deserialize(byte[] value) {
                    String key = super.deserialize(value);
                    if (key != null && !key.startsWith(prefix)) throw new IllegalStateException("Non-isolated Redis key");
                    return key == null ? null : key.substring(prefix.length());
                }
            };
            return new BeanPostProcessor() {
                @Override public Object postProcessAfterInitialization(Object bean, String name) {
                    if (bean instanceof RedisTemplate<?, ?> template) template.setKeySerializer(serializer);
                    return bean;
                }
            };
        }
    }
}
