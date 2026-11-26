package com.ican.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ican.entity.User;
import com.ican.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.ican.constant.PersonConstant.MY_MAIL;
import static com.ican.constant.PersonConstant.MY_RED_MAIL;

/**
 * 公开文章读取时复用既有私人账号规则，不授予草稿或已撤回内容的访问权。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleAccessService {

    private final UserMapper userMapper;

    public boolean canReadPrivateArticles() {
        try {
            if (!StpUtil.isLogin()) {
                return false;
            }
            User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .select(User::getEmail).eq(User::getId, StpUtil.getLoginIdAsInt()));
            return user != null && (MY_MAIL.equals(user.getEmail()) || MY_RED_MAIL.equals(user.getEmail()));
        } catch (Exception e) {
            // 无法确认指定账号时收紧权限，不能依赖客户端或缓存中的身份放行。
            log.error("确认私人文章访问资格失败", e);
            return false;
        }
    }
}
