package com.ican;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 博客启动类
 *
 * @author ican
 * @date 2022/11/28 18:54
 **/
@SpringBootApplication
@EnableCaching
public class BlogApplication {

    private static Environment environment = null;

    public BlogApplication(Environment environment) {
        BlogApplication.environment = environment;
    }

    public static void main(String[] args) {
        SpringApplication.run(BlogApplication.class, args);
        String port = environment.getProperty("server.port");

        // 格式化当前时间
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedTime = LocalDateTime.now().format(formatter);
        String pink = "\033[38;5;206m"; // 粉色
        String reset = "\033[0m";       // 重置颜色

        System.out.println("---------------------博客系统启动成功！--------------------");
        System.out.println("|🚪 Service Port : " + padRightDisplay(port) + "|");
        System.out.println("|📅 Start Time   : " + padRightDisplay(formattedTime) + "|");
        System.out.println("|💻 Java Version : " + padRightDisplay(System.getProperty("java.version")) + "|");
        System.out.println("|📄 API Docs     : " + padRightDisplay("http://w-love-p.top:" + port + "/doc.html") + "|");
        System.out.println("|🔗 Frontend URL : " + padRightDisplay("http://w-love-p.top") + "|");
        System.out.println("|📊 Admin Panel  : " + padRightDisplay("http://w-love-p.top:30") + "|");
        System.out.println(pink + "--------------------Ciallo～(∠·ω< )⌒★------------------" + reset);

    }

    private static String padRightDisplay(String s) {
        return String.format("%-" + 36 + "s", s);
    }
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}