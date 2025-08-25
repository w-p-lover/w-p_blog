package com.ican;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 博客启动类
 *
 * @author ican
 * @date 2022/11/28 18:54
 **/
@SpringBootApplication
@EnableCaching
@EnableKnife4j
@Slf4j
public class BlogApplication {

    public static void main(String[] args)  {
        ConfigurableApplicationContext application = SpringApplication.run(BlogApplication.class, args);
        ConfigurableEnvironment environment = application.getEnvironment();
        String ip;
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            ip = "127.0.0.1";
            log.warn("无法获取本机 IP 地址，使用默认 127.0.0.1", e);
        }
        String port = environment.getProperty("server.port", "8080");
        log.info("\n----------------------------------------------------------\n\t" +
                        "BlogApplication is running! Access URLs:\n\t" +
                        "Local: \t\thttp://localhost:{}\n\t" +
                        "External: \thttp://{}:{}\n\t" +
                        "BlogUrl: \thttp://w-love-p.top/\n\t" +
                        "Swagger: \thttp://{}:{}/doc.html\n\t" +
                        "----------------------------------------------------------",
                port, ip, port, ip, port);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
