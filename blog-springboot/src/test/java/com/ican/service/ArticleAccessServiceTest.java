package com.ican.service;

import cn.dev33.satoken.stp.StpUtil;
import com.ican.entity.User;
import com.ican.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.ican.constant.PersonConstant.MY_MAIL;
import static com.ican.constant.PersonConstant.MY_RED_MAIL;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ArticleAccessServiceTest {

    @BeforeAll
    static void initializeUserLambdaMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "visibility-test"), User.class);
    }

    private final UserMapper mapper = mock(UserMapper.class);
    private final ArticleAccessService service = new ArticleAccessService(mapper);

    @Test
    void anonymousReaderDoesNotQueryAnAccount() {
        try (var session = mockStatic(StpUtil.class)) {
            session.when(StpUtil::isLogin).thenReturn(false);
            assertFalse(service.canReadPrivateArticles());
            verifyNoInteractions(mapper);
        }
    }

    @Test
    void bothExistingSpecialAccountsCanReadPrivateMarkers() {
        try (var session = mockStatic(StpUtil.class)) {
            session.when(StpUtil::isLogin).thenReturn(true);
            session.when(StpUtil::getLoginIdAsInt).thenReturn(1);
            for (String email : new String[]{MY_MAIL, MY_RED_MAIL}) {
                User user = new User();
                user.setEmail(email);
                when(mapper.selectOne(any())).thenReturn(user);
                assertTrue(service.canReadPrivateArticles());
            }
            verify(mapper, times(2)).selectOne(any());
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "ordinary@example.invalid"})
    void ordinaryOrIncompleteAccountsHaveNoPrivateAccess(String email) {
        try (var session = mockStatic(StpUtil.class)) {
            session.when(StpUtil::isLogin).thenReturn(true);
            User user = new User();
            user.setEmail(email);
            when(mapper.selectOne(any())).thenReturn(user);
            assertFalse(service.canReadPrivateArticles());
            verify(mapper).selectOne(any());
        }
    }

    @Test
    void deletedAccountAndAccountLookupFailureNeverGrantAccess() {
        try (var session = mockStatic(StpUtil.class)) {
            session.when(StpUtil::isLogin).thenReturn(true);
            when(mapper.selectOne(any())).thenReturn(null).thenThrow(new IllegalStateException("受控账号查询失败"));
            assertFalse(service.canReadPrivateArticles());
            assertFalse(service.canReadPrivateArticles());
            verify(mapper, times(2)).selectOne(any());
        }
    }
}
