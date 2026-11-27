package com.ican.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ican.annotation.RateLimit;
import com.ican.cache.MultiLevelCacheManager;
import com.ican.exception.ServiceException;
import com.ican.entity.*;
import com.ican.mapper.*;
import com.ican.metrics.BlogMetrics;
import com.ican.model.dto.*;
import com.ican.model.vo.*;
import com.ican.service.AiTaskService;
import com.ican.service.ArticleService;
import com.ican.service.ArticleAccessService;
import com.ican.service.HotArticleWarmupService;
import com.ican.service.RedisService;
import com.ican.service.TagService;
import com.ican.strategy.context.SearchStrategyContext;
import com.ican.strategy.context.UploadStrategyContext;
import com.ican.utils.BeanCopyUtils;
import com.ican.utils.FileUtils;
import com.ican.utils.PageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.ican.constant.CommonConstant.FALSE;
import static com.ican.constant.RedisConstant.*;
import static com.ican.enums.FilePathEnum.ARTICLE;

/**
 * 文章业务接口实现类（Phase 2 重构版）
 *
 * 重构内容：
 * 1. 使用 MultiLevelCacheManager 替代手写缓存逻辑
 * 2. 使用 Redisson 分布式锁替代手写 setIfAbsent
 * 3. 添加 @RateLimit 限流保护
 * 4. 移除 Guava Striped 锁和 Caffeine 手动管理
 *
 * @author ican
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    private final ArticleAccessService articleAccessService;

    private final CategoryMapper categoryMapper;

    private final ArticleTagMapper articleTagMapper;

    private final TagMapper tagMapper;

    private final TagService tagService;

    private final ArticleMapper articleMapper;

    private final RedisService redisService;

    private final SearchStrategyContext searchStrategyContext;

    private final UploadStrategyContext uploadStrategyContext;

    private final BlogFileMapper blogFileMapper;

    private final BlogMetrics blogMetrics;

    private final MultiLevelCacheManager cacheManager;

    private final HotArticleWarmupService hotArticleWarmupService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private AiTaskService aiTaskService;

    private final ArticleViewService articleViewService;

    private final AtomicBoolean homeWarmupScheduled = new AtomicBoolean();

    @Autowired
    private ThreadPoolTaskExecutor hotArticleExecutor;

    @Override
    public PageResult<ArticleBackVO> listArticleBackVO(ConditionDTO condition) {
        // 查询文章数量
        Long count = articleMapper.countArticleBackVO(condition);
        if (count == 0) {
            return new PageResult<>();
        }
        // 查询文章后台信息
        List<ArticleBackVO> articleBackVOList = articleMapper.selectArticleBackVO(PageUtils.getLimit(), PageUtils.getSize(), condition);
        // 浏览量
        Map<Object, Double> viewCountMap = redisService.getZsetAllScore(ARTICLE_VIEW_COUNT);
        // 点赞量
        Map<String, Integer> likeCountMap = redisService.getHashAll(ARTICLE_LIKE_COUNT);
        // 封装文章后台信息
        articleBackVOList.forEach(item -> {
            Double viewCount = Optional.ofNullable(viewCountMap.get(item.getId())).orElse((double) 0);
            item.setViewCount(viewCount.intValue());
            Integer likeCount = likeCountMap.get(item.getId().toString());
            item.setLikeCount(Optional.ofNullable(likeCount).orElse(0));
        });

        return new PageResult<>(articleBackVOList, count);
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void addArticle(ArticleDTO article) {
        // 保存文章分类
        Integer categoryId = saveArticleCategory(article);
        // 添加文章
        Article newArticle = BeanCopyUtils.copyBean(article, Article.class);
        if (StringUtils.isBlank(newArticle.getArticleCover())) {
            SiteConfig siteConfig = redisService.getObject(SITE_SETTING);
            newArticle.setArticleCover(siteConfig.getArticleCover());
        }
        newArticle.setCategoryId(categoryId);
        newArticle.setUserId(StpUtil.getLoginIdAsInt());
        newArticle.setContentVersion(UUID.randomUUID().toString());
        newArticle.setSourceVersion(UUID.randomUUID().toString());
        baseMapper.insert(newArticle);
        // 保存文章标签
        saveArticleTag(article, newArticle.getId());
        // 提交后失效，也清除文章创建前可能存在的负缓存及旧装载。
        cacheManager.evict("article:" + newArticle.getId());
        // 创建文章AI任务并异步投递
        createArticleAiTask(newArticle.getId(), newArticle.getArticleTitle(), newArticle.getArticleContent());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteArticle(List<Integer> articleIdList) {
        // 删除文章标签
        articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>()
                .in(ArticleTag::getArticleId, articleIdList));
        // 删除文章
        articleMapper.deleteBatchIds(articleIdList);
        // 清除缓存
        articleIdList.forEach(id -> cacheManager.evict("article:" + id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateArticleDelete(DeleteDTO delete) {
        try {
            // 批量更新文章删除状态
            List<Article> articleList = delete.getIdList()
                    .stream()
                    .map(id -> Article.builder()
                            .id(id)
                            .isDelete(delete.getIsDelete())
                            .isTop(FALSE) // 取消置顶
                            .isRecommend(FALSE) // 取消推荐
                            .build())
                    .collect(Collectors.toList());

            this.updateBatchById(articleList);

            // 清除缓存
            delete.getIdList().forEach(id -> cacheManager.evict("article:" + id));

            log.info("成功更新 {} 篇文章的删除状态", articleList.size());
        } catch (Exception e) {
            log.error("更新文章删除状态失败", e);
            throw new RuntimeException("更新文章删除状态失败", e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateArticle(ArticleDTO article) {
        // 保存文章分类
        Integer categoryId = saveArticleCategory(article);
        // 修改文章
        Article newArticle = BeanCopyUtils.copyBean(article, Article.class);
        newArticle.setCategoryId(categoryId);
        newArticle.setUserId(StpUtil.getLoginIdAsInt());
        newArticle.setContentVersion(UUID.randomUUID().toString());
        newArticle.setSourceVersion(UUID.randomUUID().toString());
        baseMapper.updateById(newArticle);
        articleMapper.resetArticleAiResult(newArticle.getId(), newArticle.getSourceVersion());
        // 保存文章标签
        saveArticleTag(article, newArticle.getId());
        // 清除缓存
        cacheManager.evict("article:" + newArticle.getId());
        // 创建文章AI任务并异步投递
        createArticleAiTask(newArticle.getId(), newArticle.getArticleTitle(), newArticle.getArticleContent());
    }

    @Override
    public ArticleInfoVO editArticle(Integer articleId) {
        // 查询文章信息
        ArticleInfoVO articleInfoVO = articleMapper.selectArticleInfoById(articleId);
        Assert.notNull(articleInfoVO, "没有该文章");
        // 查询文章分类名称
        Category category = categoryMapper.selectOne(new LambdaQueryWrapper<Category>()
                .select(Category::getCategoryName)
                .eq(Category::getId, articleInfoVO.getCategoryId()));
        // 查询文章标签名称
        List<String> tagNameList = tagMapper.selectTagNameByArticleId(articleId);
        articleInfoVO.setCategoryName(category.getCategoryName());
        articleInfoVO.setTagNameList(tagNameList);
        return articleInfoVO;
    }

    @Override
    public void updateArticleTop(TopDTO top) {
        // 修改文章置顶状态
        Article newArticle = Article.builder()
                .id(top.getId())
                .isTop(top.getIsTop())
                .build();
        articleMapper.updateById(newArticle);
        // 清除缓存
        cacheManager.evict("article:" + top.getId());
    }

    @Override
    public void updateArticleRecommend(RecommendDTO recommend) {
        // 修改文章推荐状态
        Article newArticle = Article.builder()
                .id(recommend.getId())
                .isRecommend(recommend.getIsRecommend())
                .build();
        articleMapper.updateById(newArticle);
        // 清除缓存
        cacheManager.evict("article:" + recommend.getId());
    }

    @Override
    public List<ArticleSearchVO> listArticlesBySearch(String keyword) {
        return searchStrategyContext.executeSearchStrategy(keyword);
    }

    @Override
    public PageResult<ArticleHomeVO> listArticleHomeVO(String sort, Integer tagId, String start, String end) {

        String safeSort = normalizeSort(sort);
        boolean isSpecialEmail = articleAccessService.canReadPrivateArticles();
        List<ArticleHomeVO> articles = queryArticles(safeSort, tagId, start, end, isSpecialEmail);
        if (CollectionUtils.isEmpty(articles)) {
            return new PageResult<>();
        }
        PageResult<ArticleHomeVO> result = paginateResults(articles);
        processHotArticles(result.getRecordList());
        return result;
    }

    /**
     * 缓存热点文章内容（使用多级缓存管理器）
     */
    private void processHotArticles(List<ArticleHomeVO> articles) {
        Map<Object, Double> viewCountMap = redisService.getZsetAllScore(ARTICLE_VIEW_COUNT);
        Set<Integer> hotCandidates = new LinkedHashSet<>();

        for (ArticleHomeVO article : articles) {
            article.setArticleContent(article.getArticleContent().replaceAll("#", ""));
            sortTags(article.getTagVOList());
            Double viewCount = viewCountMap.getOrDefault(article.getId(), 0D);

            if (viewCount >= 20 || article.getIsTop() == 1) {
                hotCandidates.add(article.getId());
            }
        }

        // 同一实例首页最多一个待执行/运行中的预热，避免重复请求占满队列。
        if (!hotCandidates.isEmpty() && homeWarmupScheduled.compareAndSet(false, true)) {
            try {
                hotArticleExecutor.execute(() -> {
                    try {
                        hotArticleWarmupService.warmupArticleIds(hotCandidates);
                    } catch (RuntimeException e) {
                        log.warn("首页热点预热失败，不影响已取得的列表", e);
                    } finally {
                        homeWarmupScheduled.set(false);
                    }
                });
            } catch (RuntimeException e) {
                homeWarmupScheduled.set(false);
                log.warn("首页热点预热未能入队", e);
            }
        }
    }

    /**
     * 查询文章详情（使用多级缓存 + 限流）
     */
    @Override
    @RateLimit(key = "api:article:view:#{#articleId}", perClient = true, limit = 10, period = 60)
    public ArticleVO getArticleHomeById(Integer articleId) {
        // 先选持久版本，旧实例L1、漏广播或失败删除不再决定详情正文版本。
        // 此处仅选已发布版本；读者权限仍在装载完成后确认。
        String contentVersion = articleMapper.selectVisibleArticleVersion(articleId, true);
        if (contentVersion == null) {
            return null;
        }
        String cacheKey = "article:" + articleId + ":v:" + contentVersion + ":body";

        // 正文回源不读取动态统计；统计故障不能污染正文负缓存或阻止回填。
        ArticleVO article = cacheManager.get(cacheKey, key -> {
            ArticleVO loaded = articleMapper.selectArticleHomeById(articleId, contentVersion);
            if (loaded == null) {
                // 选版本后撤回/删除可能使装载为空，不能给可恢复的同版本正文留下负缓存。
                throw new ServiceException("内容已更新，请重新读取");
            }
            return loaded;
        }, 30);

        // 即使命中负缓存，也要检查装载期间是否发生发布/编辑。
        boolean privateAccess = articleAccessService.canReadPrivateArticles();
        ArticleDetailStateVO state = articleMapper.selectVisibleArticleDetailState(articleId, privateAccess);
        if (state == null) {
            return null;
        }
        if (!contentVersion.equals(state.getContentVersion())) {
            throw new ServiceException("内容已更新，请重新读取");
        }
        if (article == null) {
            // 清理升级前可能留下的同版本负缓存；不把当前存在的文章伪装为不存在。
            cacheManager.evict(cacheKey);
            throw new ServiceException("内容已更新，请重新读取");
        }
        // 共享缓存不保存当前读者的导航；每次覆盖旧缓存中可能残留的上下篇。
        article = BeanCopyUtils.copyBean(article, ArticleVO.class);
        article.setCategory(state.getCategory());
        article.setTagVOList(state.getTagVOList());
        article.setLastArticle(state.getLastArticle());
        article.setNextArticle(state.getNextArticle());
        updateArticleStatsFromRedis(articleId, article, state.getViews());
        return article;
    }

    private void updateArticleStatsFromRedis(Integer articleId, ArticleVO articleVO, Integer persistedViews) {
        articleVO.setViews(articleViewService.recordView(articleId, persistedViews));
        try {
            Integer likes = redisService.getHash(ARTICLE_LIKE_COUNT, articleId.toString());
            articleVO.setLikeCount(Optional.ofNullable(likes).orElse(0));
        } catch (DataAccessException e) {
            blogMetrics.recordArticleStatsFailure("likes_read");
            articleVO.setLikeCount(0);
            log.debug("文章点赞统计不可用: articleId={}", articleId, e);
        }
    }

    @Override
    public PageResult<ArchiveVO> listArchiveVO() {
        boolean privateAccess = articleAccessService.canReadPrivateArticles();
        Long count = articleMapper.countVisibleArticles(privateAccess);
        if (count == 0) {
            return new PageResult<>();
        }
        List<ArchiveVO> archiveList = articleMapper.selectArchiveList(PageUtils.getLimit(), PageUtils.getSize(), privateAccess);
        return new PageResult<>(archiveList, count);
    }

    @Override
    public List<ArticleRecommendVO> listArticleRecommendVO() {
        return articleMapper.selectArticleRecommend(articleAccessService.canReadPrivateArticles());
    }

    @Override
    public String saveArticleImages(MultipartFile file) {
        // 上传文件
        String url = uploadStrategyContext.executeUploadStrategy(file, ARTICLE.getPath());
        try {
            // 获取文件md5值
            String md5 = FileUtils.getMd5(file.getInputStream());
            // 获取文件扩展名
            String extName = FileUtils.getExtension(file);
            BlogFile existFile = blogFileMapper.selectOne(new LambdaQueryWrapper<BlogFile>()
                    .select(BlogFile::getId)
                    .eq(BlogFile::getFileName, md5)
                    .eq(BlogFile::getFilePath, ARTICLE.getFilePath()));
            if (Objects.isNull(existFile)) {
                // 保存文件信息
                BlogFile newFile = BlogFile.builder()
                        .fileUrl(url)
                        .fileName(md5)
                        .filePath(ARTICLE.getFilePath())
                        .extendName(extName)
                        .fileSize((int) file.getSize())
                        .isDir(FALSE)
                        .build();
                blogFileMapper.insert(newFile);
            }
        } catch (IOException e) {
            log.error("文件上传持久化错误:" + e.getMessage());
        }
        return url;
    }

    /**
     * 保存文章分类
     *
     * @param article 文章信息
     * @return 文章分类
     */
    private Integer saveArticleCategory(ArticleDTO article) {
        // 查询分类
        Category category = categoryMapper.selectOne(new LambdaQueryWrapper<Category>()
                .select(Category::getId)
                .eq(Category::getCategoryName, article.getCategoryName()));
        // 分类不存在
        if (Objects.isNull(category)) {
            category = Category.builder()
                    .categoryName(article.getCategoryName())
                    .build();
            // 保存分类
            categoryMapper.insert(category);
        }
        return category.getId();
    }

    /**
     * 保存文章标签
     *
     * @param article   文章信息
     * @param articleId 文章id
     */
    private void saveArticleTag(ArticleDTO article, Integer articleId) {
        // 删除文章标签
        articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>()
                .eq(ArticleTag::getArticleId, articleId));
        // 标签名列表
        List<String> tagNameList = article.getTagNameList();
        if (CollectionUtils.isNotEmpty(tagNameList)) {
            // 查询出已存在的标签
            List<Tag> existTagList = tagMapper.selectTagList(tagNameList);
            List<String> existTagNameList = existTagList.stream()
                    .map(Tag::getTagName)
                    .collect(Collectors.toList());
            List<Integer> existTagIdList = existTagList.stream()
                    .map(Tag::getId)
                    .collect(Collectors.toList());
            // 移除已存在的标签列表
            tagNameList.removeAll(existTagNameList);
            // 含有新标签
            if (CollectionUtils.isNotEmpty(tagNameList)) {
                // 新标签列表
                List<Tag> newTagList = tagNameList.stream()
                        .map(item -> Tag.builder()
                                .tagName(item)
                                .build())
                        .collect(Collectors.toList());
                // 批量保存新标签
                tagService.saveBatch(newTagList);
                // 获取新标签id列表
                List<Integer> newTagIdList = newTagList.stream()
                        .map(Tag::getId)
                        .collect(Collectors.toList());
                // 新标签id添加到id列表
                existTagIdList.addAll(newTagIdList);
            }
            // 将所有的标签绑定到文章标签关联表
            articleTagMapper.saveBatchArticleTag(articleId, existTagIdList);
        }

    }

    // ----------- 辅助方法 ----------- //

    // 查询文章列表（核心查询方法）
    private List<ArticleHomeVO> queryArticles(String sort, Integer tagId, String start,
                                              String end, boolean isSpecial) {
        if (isSpecial) {
            // 指定账号可查看私人标记，但仍限于已发布且未删除文章
            return articleMapper.selectArticleAllList(sort, tagId, start, end);
        } else {
            // 普通用户只能查询已发布文章
            return articleMapper.PselectArticleAllList(sort, tagId, start, end);
        }
    }

    // 内存分页处理
    private PageResult<ArticleHomeVO> paginateResults(List<ArticleHomeVO> articles) {
        int total = articles.size();
        int pageSize = Math.toIntExact(PageUtils.getSize());
        int currentPage = Math.toIntExact(PageUtils.getCurrent());

        // 计算分页区间
        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, total);

        if (start >= total) {
            return new PageResult<>(List.of(), (long) total);
        }
        return new PageResult<>(articles.subList(start, end), (long) total);
    }

    // 标签排序
    private void sortTags(List<TagOptionVO> tags) {
        if (!CollectionUtils.isEmpty(tags)) {
            tags.sort(Comparator.comparingInt(TagOptionVO::getId));
        }
    }

    private String normalizeSort(String sort) {
        if ("views".equals(sort)) {
            return "views";
        }
        return "create_time";
    }

    private void createArticleAiTask(Integer articleId, String title, String content) {
        if (aiTaskService == null || articleId == null || StringUtils.isBlank(content)) {
            return;
        }
        aiTaskService.createArticleTask(articleId, title, content);
    }
}
