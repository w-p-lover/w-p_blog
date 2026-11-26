package com.ican.strategy.context;

import com.ican.mapper.ArticleMapper;
import com.ican.model.vo.ArticleSearchVO;
import com.ican.strategy.SearchStrategy;
import com.ican.service.ArticleAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.ican.enums.SearchModeEnum.getStrategy;

/**
 * 搜索策略上下文
 *
 * @author xcs
 */
@Service
public class SearchStrategyContext {

    /**
     * 搜索模式
     */
    @Value("${search.mode}")
    private String searchMode;

    @Autowired
    private Map<String, SearchStrategy> searchStrategyMap;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ArticleAccessService articleAccessService;

    /**
     * 执行搜索策略
     *
     * @param keyword 关键字
     * @return {@link List <ArticleSearchVO>} 搜索文章
     */
    public List<ArticleSearchVO> executeSearchStrategy(String keyword) {
        List<ArticleSearchVO> candidates = searchStrategyMap.get(getStrategy(searchMode)).searchArticle(keyword);
        List<Integer> ids = candidates.stream().map(ArticleSearchVO::getId)
                .filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        // 索引只决定候选与排序；数据库校验失败时直接失败，不能降级为输出旧索引内容。
        Set<Integer> visibleIds = new HashSet<>(articleMapper.selectVisibleArticleIds(ids, articleAccessService.canReadPrivateArticles()));
        return candidates.stream().filter(article -> visibleIds.contains(article.getId())).toList();
    }

}