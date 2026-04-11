package com.ican.config;

import cloud.tianai.captcha.common.constant.CaptchaTypeConstant;
import cloud.tianai.captcha.resource.ResourceStore;
import cloud.tianai.captcha.resource.common.model.dto.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CaptchaResourceConfiguration {

    private final ResourceStore resourceStore;
    private final ResourcePatternResolver resourcePatternResolver;

    //type枚举，用于随机设置类型
    private static final String[] CAPTCHA_TYPES = {
            CaptchaTypeConstant.ROTATE,
            CaptchaTypeConstant.WORD_IMAGE_CLICK,
            CaptchaTypeConstant.SLIDER,
            CaptchaTypeConstant.CONCAT
    };

    /**
     * 加载验证码图片资源
     * @throws IOException
     */
    @PostConstruct
    public void init() throws IOException {
        List<String> imagePaths = loadImagePaths();
        for (String path : imagePaths) {
            for (String captchaType : CAPTCHA_TYPES) {
                addCaptchaResource(captchaType, path);
            }
        }
        log.info("验证码模板加载完成: 图片数={}, 类型数={}, 总模板数={}", imagePaths.size(), CAPTCHA_TYPES.length, imagePaths.size() * CAPTCHA_TYPES.length);
    }

    private List<String> loadImagePaths() throws IOException {
        var resources = resourcePatternResolver.getResources("classpath:bg_images/*.jpg");
        List<String> imagePaths = new ArrayList<>();
        for (var resource : resources) {
            imagePaths.add(resource.getFilename());
        }
        return imagePaths;
    }

    private void addCaptchaResource(String captchaType, String path) {
        resourceStore.addResource(captchaType,
                new Resource("classpath", "bg_images/" + path, null));
        resourceStore.addResource(captchaType,
                new Resource("classpath", "bg_images/" + path, "default"));
    }
}
