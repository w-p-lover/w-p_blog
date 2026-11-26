package com.ican.strategy.context;

import com.ican.enums.SearchModeEnum;
import com.ican.mapper.ArticleMapper;
import com.ican.model.vo.ArticleSearchVO;
import com.ican.strategy.SearchStrategy;
import com.ican.service.ArticleAccessService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SearchStrategyContextTest {

    @ParameterizedTest
    @EnumSource(SearchModeEnum.class)
    void bothModesFilterWithdrawnCandidatesInOneBatchAndPreserveRankAndHighlight(SearchModeEnum mode) {
        ArticleMapper mapper = mock(ArticleMapper.class);
        SearchStrategy strategy = mock(SearchStrategy.class);
        ArticleSearchVO first = article(8);
        first.setArticleTitle("<em>关键词</em>仍公开");
        ArticleSearchVO withdrawn = article(9);
        withdrawn.setArticleContent("不能返回的旧摘要");
        ArticleSearchVO last = article(7);
        when(strategy.searchArticle("关键词")).thenReturn(List.of(first, withdrawn, last, article(null)));
        when(mapper.selectVisibleArticleIds(List.of(8, 9, 7), false)).thenReturn(List.of(7, 8));

        List<ArticleSearchVO> result = context(mode, strategy, mapper).executeSearchStrategy("关键词");

        assertEquals(List.of(first, last), result);
        assertEquals("<em>关键词</em>仍公开", result.get(0).getArticleTitle());
        verify(mapper).selectVisibleArticleIds(List.of(8, 9, 7), false);
        verifyNoMoreInteractions(mapper);
    }

    @ParameterizedTest
    @EnumSource(SearchModeEnum.class)
    void statusLookupFailurePropagatesInsteadOfReturningStaleResults(SearchModeEnum mode) {
        ArticleMapper mapper = mock(ArticleMapper.class);
        SearchStrategy strategy = mock(SearchStrategy.class);
        when(strategy.searchArticle("词")).thenReturn(List.of(article(8)));
        when(mapper.selectVisibleArticleIds(List.of(8), false)).thenThrow(new IllegalStateException("数据库不可用"));

        assertThrows(IllegalStateException.class, () -> context(mode, strategy, mapper).executeSearchStrategy("词"));
    }

    @ParameterizedTest
    @EnumSource(SearchModeEnum.class)
    void noCandidateDoesNotIssueAnUnboundedStatusQuery(SearchModeEnum mode) {
        ArticleMapper mapper = mock(ArticleMapper.class);
        SearchStrategy strategy = mock(SearchStrategy.class);
        when(strategy.searchArticle("无匹配")).thenReturn(List.of());

        assertTrue(context(mode, strategy, mapper).executeSearchStrategy("无匹配").isEmpty());

        verifyNoInteractions(mapper);
    }

    @ParameterizedTest
    @EnumSource(SearchModeEnum.class)
    void bothModesRecheckPrivateAccessAfterItsLossAndRecovery(SearchModeEnum mode) {
        ArticleMapper mapper = mock(ArticleMapper.class);
        SearchStrategy strategy = mock(SearchStrategy.class);
        ArticleAccessService access = mock(ArticleAccessService.class);
        when(strategy.searchArticle("词")).thenReturn(List.of(article(8), article(9)));
        when(access.canReadPrivateArticles()).thenReturn(true, false, true);
        when(mapper.selectVisibleArticleIds(List.of(8, 9), true)).thenReturn(List.of(8, 9));
        when(mapper.selectVisibleArticleIds(List.of(8, 9), false)).thenReturn(List.of(8));
        SearchStrategyContext context = context(mode, strategy, mapper);
        ReflectionTestUtils.setField(context, "articleAccessService", access);

        assertEquals(List.of(8, 9), context.executeSearchStrategy("词").stream().map(ArticleSearchVO::getId).toList());
        assertEquals(List.of(8), context.executeSearchStrategy("词").stream().map(ArticleSearchVO::getId).toList());
        assertEquals(List.of(8, 9), context.executeSearchStrategy("词").stream().map(ArticleSearchVO::getId).toList());
    }

    private SearchStrategyContext context(SearchModeEnum mode, SearchStrategy strategy, ArticleMapper mapper) {
        SearchStrategyContext context = new SearchStrategyContext();
        ReflectionTestUtils.setField(context, "searchMode", mode.getMode());
        ReflectionTestUtils.setField(context, "searchStrategyMap", Map.of(mode.getStrategy(), strategy));
        ReflectionTestUtils.setField(context, "articleMapper", mapper);
        ReflectionTestUtils.setField(context, "articleAccessService", mock(ArticleAccessService.class));
        return context;
    }

    private ArticleSearchVO article(Integer id) {
        return ArticleSearchVO.builder().id(id).build();
    }
}
