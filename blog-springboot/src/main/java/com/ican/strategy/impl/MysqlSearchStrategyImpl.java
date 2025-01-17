package com.ican.strategy.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ican.entity.User;
import com.ican.mapper.ArticleMapper;
import com.ican.mapper.UserMapper;
import com.ican.model.vo.ArticleSearchVO;
import com.ican.model.vo.CategoryVO;
import com.ican.strategy.SearchStrategy;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.ican.constant.ElasticConstant.POST_TAG;
import static com.ican.constant.ElasticConstant.PRE_TAG;
import static com.ican.constant.PersonConstant.MY_MAIL;
import static com.ican.constant.PersonConstant.MY_RED_MAIL;

/**
 * MySQL搜索策略
 *
 * @author ican
 */
@Service("mySqlSearchStrategyImpl")
public class MysqlSearchStrategyImpl implements SearchStrategy {
    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private UserMapper userMapper;
    /**
     * 搜索功能使用私人屏蔽
     * @param keyword 关键字
     * @return
     */
    @Override
    public List<ArticleSearchVO> searchArticle(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return new ArrayList<>();
        }
        List<ArticleSearchVO> articleSearchVOList = articleMapper.searchArticle(keyword);
        String email = null;
        if (StpUtil.isLogin()) {
            int userId = StpUtil.getLoginIdAsInt();
            email = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .select(User::getEmail).eq(User::getId, userId)).getEmail();
        }
        if (ObjectUtil.isNull(email) || (!email.equals(MY_MAIL) && !email.equals(MY_RED_MAIL))) {
            articleSearchVOList.removeIf(articleSearch ->
                    articleSearch.getCategoryId() == 60 || articleSearch.getCategoryId() == 61);
        }
        return articleSearchVOList.stream()
                .map(article -> {
                    // 获取关键词第一次出现的位置
                    String articleContent = article.getArticleContent();
                    int index = article.getArticleContent().indexOf(keyword);
                    if (index != -1) {
                        // 获取关键词前面的文字
                        int preIndex = index > 25 ? index - 25 : 0;
                        String preText = article.getArticleContent().substring(preIndex, index);
                        // 获取关键词到后面的文字
                        int last = index + keyword.length();
                        int postLength = article.getArticleContent().length() - last;
                        int postIndex = postLength > 175 ? last + 175 : last + postLength;
                        String postText = article.getArticleContent().substring(index, postIndex);
                        // 文章内容高亮
                        articleContent = (preText + postText).replaceAll(keyword, PRE_TAG + keyword + POST_TAG);
                    }
                    // 文章标题高亮
                    String articleTitle = article.getArticleTitle().replaceAll(keyword, PRE_TAG + keyword + POST_TAG);
                    return ArticleSearchVO.builder()
                            .id(article.getId())
                            .articleTitle(articleTitle)
                            .articleContent(articleContent)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
