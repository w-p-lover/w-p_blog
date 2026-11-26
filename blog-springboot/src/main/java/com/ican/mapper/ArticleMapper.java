package com.ican.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ican.entity.Article;
import com.ican.model.dto.ConditionDTO;
import com.ican.model.vo.*;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 文章 Mapper
 *
 * @author xcs
 * @date 2022/12/04 22:29
 **/
@Repository
public interface ArticleMapper extends BaseMapper<Article> {

    /**
     * 查询后台文章数量
     *
     * @param condition 条件
     * @return 文章数量
     */
    Long countArticleBackVO(@Param("condition") ConditionDTO condition);

    /**
     * 查询后台文章列表
     *
     * @param limit     页码
     * @param size      大小
     * @param condition 条件
     * @return 后台文章列表
     */
    List<ArticleBackVO> selectArticleBackVO(@Param("limit") Long limit, @Param("size") Long size, @Param("condition") ConditionDTO condition);

    /**
     * 根据id查询文章信息
     *
     * @param articleId 文章id
     * @return 文章信息
     */
    ArticleInfoVO selectArticleInfoById(@Param("articleId") Integer articleId);

    /**
     * 文章搜索
     *
     * @param keyword 关键字
     * @return 文章列表
     */
    List<ArticleSearchVO> searchArticle(@Param("keyword") String keyword);

    /**
     * 从当前数据库状态确认当前读者可见的公开主键，不依赖内容缓存或搜索索引。
     */
    List<Integer> selectVisibleArticleIds(@Param("articleIds") Collection<Integer> articleIds,
                                          @Param("privateAccess") boolean privateAccess);


/**
     * 查询首页文章
     *
     * @param limit 页码
     * @param size  大小
     * @param sort
     * @return 首页文章
     *//*
    List<ArticleHomeVO> selectArticleHomeList(@Param("limit") Long limit, @Param("size") Long size, String sort, Integer tagId, String start, String end);
*/
/**
     * 私人浏览，对于非我和老婆的，统一不给看我老婆
     *
     * @param limit
     * @param size
     * @param sort
     * @return
     *//*
    List<ArticleHomeVO> PselectArticleHomeList(@Param("limit") Long limit, @Param("size") Long size, String sort, Integer tagId, String start, String end);
*/

    /**
     * 查询首页文章
     *
     * @param sort
     * @return 公共筛选文章列表
     */
    List<ArticleHomeVO> PselectArticleAllList(String sort, Integer tagId, String start, String end);

    /**
     * 私人浏览，对于非我和老婆的，统一不给看我老婆
     *
     * @param sort
     * @return 私人筛选文章列表
     */
    List<ArticleHomeVO> selectArticleAllList(String sort, Integer tagId, String start, String end);

    /**
     * 根据id查询首页文章
     *
     * @param articleId 文章id
     * @return 首页文章
     */
    ArticleVO selectArticleHomeById(@Param("articleId") Integer articleId,
                                    @Param("contentVersion") String contentVersion);

    /**
     * 从当前数据库取得可见正文版本，不读取大字段，也不信任Redis失效结果。
     */
    String selectVisibleArticleVersion(@Param("articleId") Integer articleId,
                                       @Param("privateAccess") boolean privateAccess);

    /**
     * 当前读者可见的版本与分类/标签快照，不缓存关联名称。
     */
    ArticleDetailStateVO selectVisibleArticleDetailState(@Param("articleId") Integer articleId,
                                                        @Param("privateAccess") boolean privateAccess);

    /**
     * 查询上一篇文章
     *
     * @param articleId 文章id
     * @return 上一篇文章
     */
    ArticlePaginationVO selectLastArticle(@Param("articleId") Integer articleId, @Param("privateAccess") boolean privateAccess);

    /**
     * 查询下一篇文章
     *
     * @param articleId 文章id
     * @return 下一篇文章
     */
    ArticlePaginationVO selectNextArticle(@Param("articleId") Integer articleId, @Param("privateAccess") boolean privateAccess);

    /**
     * 查询文章归档
     *
     * @param limit 页码
     * @param size  大小
     * @return 文章归档
     */
    List<ArchiveVO> selectArchiveList(@Param("limit") Long limit, @Param("size") Long size,
                                      @Param("privateAccess") boolean privateAccess);

    /**
     * 与归档列表使用相同可见性条件的数量。
     */
    Long countVisibleArticles(@Param("privateAccess") boolean privateAccess);

    /**
     * 查询文章统计
     *
     * @return 文章统计
     */
    List<ArticleStatisticsVO> selectArticleStatistics();

    /**
     * 查询热度分计算所需文章基础数据
     *
     * @return 文章热度分来源数据
     */
    List<ArticleHotScoreSourceVO> selectHotScoreSourceArticles();

    /**
     * 查询推荐文章
     *
     * @return 推荐文章
     */
    List<ArticleRecommendVO> selectArticleRecommend(@Param("privateAccess") boolean privateAccess);

    /**
     * 根据条件查询文章
     *
     * @param limit     页码
     * @param size      大小
     * @param condition 条件
     * @return 文章列表
     */
    List<ArticleConditionVO> listArticleByCondition(@Param("limit") Long limit, @Param("size") Long size, @Param("condition") ConditionDTO condition,
                                                    @Param("privateAccess") boolean privateAccess);

    @Update("UPDATE t_article SET views = views + 1 WHERE id = #{articleId}")
    void incrementViews(@Param("articleId") Long articleId);

    /** 展示型PV的累计快照；重试和乱序写入不重复加数，也不回退已持久化值。 */
    @Update("UPDATE t_article SET views = GREATEST(COALESCE(views, 0), #{views}) WHERE id = #{articleId}")
    int persistViewSnapshot(@Param("articleId") Integer articleId, @Param("views") int views);

    @Update("UPDATE t_article SET views = views + #{i} WHERE id = #{articleId}")
    void incrementViewsByi(@Param("articleId") Integer id, @Param("i") Double aDouble);

}
