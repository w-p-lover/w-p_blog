package com.ican.controller;


import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.application.vo.CaptchaResponse;
import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import cloud.tianai.captcha.common.constant.CaptchaTypeConstant;
import cloud.tianai.captcha.common.response.ApiResponse;
import cloud.tianai.captcha.spring.plugins.secondary.SecondaryVerificationApplication;
import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

@RestController
public class CaptchaController {
    @Autowired
    private ImageCaptchaApplication imageCaptchaApplication;

    private static final String[] CAPTCHA_TYPES = {
            CaptchaTypeConstant.SLIDER,
            CaptchaTypeConstant.CONCAT,
            CaptchaTypeConstant.ROTATE,
            CaptchaTypeConstant.WORD_IMAGE_CLICK
    };

    /**
     * 前端返回type，获取验证码资源
     * @param request
     * @param type
     * @return
     */
    @RequestMapping("/gen")
    @ResponseBody
    public CaptchaResponse<ImageCaptchaVO> genCaptcha(HttpServletRequest request, @RequestParam(value = "type", required = false) String type) {
        if (StringUtils.isBlank(type)) {
            type = CaptchaTypeConstant.SLIDER;
        }
        if ("RANDOM".equals(type)) {
            return generateRandomCaptcha();
        }
        return generateCaptchaWithFallback(type);
    }

    private CaptchaResponse<ImageCaptchaVO> generateRandomCaptcha() {
        int startIndex = ThreadLocalRandom.current().nextInt(0, CAPTCHA_TYPES.length);
        RuntimeException lastException = null;
        for (int i = 0; i < CAPTCHA_TYPES.length; i++) {
            String tryType = CAPTCHA_TYPES[(startIndex + i) % CAPTCHA_TYPES.length];
            try {
                return imageCaptchaApplication.generateCaptcha(tryType);
            } catch (RuntimeException ex) {
                lastException = ex;
                if (!isTemplateEmptyError(ex)) {
                    throw ex;
                }
            }
        }
        throw lastException == null ? new RuntimeException("验证码模板不可用") : lastException;
    }

    private CaptchaResponse<ImageCaptchaVO> generateCaptchaWithFallback(String preferredType) {
        try {
            return imageCaptchaApplication.generateCaptcha(preferredType);
        } catch (RuntimeException ex) {
            if (!isTemplateEmptyError(ex)) {
                throw ex;
            }
            for (String tryType : CAPTCHA_TYPES) {
                if (tryType.equals(preferredType)) {
                    continue;
                }
                try {
                    return imageCaptchaApplication.generateCaptcha(tryType);
                } catch (RuntimeException fallbackEx) {
                    if (!isTemplateEmptyError(fallbackEx)) {
                        throw fallbackEx;
                    }
                }
            }
            throw ex;
        }
    }

    private boolean isTemplateEmptyError(RuntimeException ex) {
        String msg = ex.getMessage();
        return msg != null && msg.contains("模板为空");
    }

    /**
     * 内置确认，确认验证
     * @param data
     * @param request
     * @return
     */
    @PostMapping("/check")
    @ResponseBody
    public ApiResponse<?> checkCaptcha(@RequestBody Data data, HttpServletRequest request) {
        ApiResponse<?> response = imageCaptchaApplication.matching(data.getId(), data.getData());
        if (response.isSuccess()) {
            return ApiResponse.ofSuccess(Collections.singletonMap("id", data.getId()));
        }
        return response;
    }

    @lombok.Data
    public static class Data {
        private String id;
        private ImageCaptchaTrack data;
    }

    /**
     * 二次验证，一般用于机器内部调用，这里为了方便测试
     * @param id id
     * @return boolean
     */
    @GetMapping("/check2")
    @ResponseBody
    public boolean check2Captcha(@RequestParam("id") String id) {
        // 如果开启了二次验证
        if (imageCaptchaApplication instanceof SecondaryVerificationApplication) {
            return ((SecondaryVerificationApplication) imageCaptchaApplication).secondaryVerification(id);
        }
        return false;
    }
}
