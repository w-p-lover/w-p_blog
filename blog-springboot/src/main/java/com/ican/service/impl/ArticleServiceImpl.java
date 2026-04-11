package com.ican.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ican.annotation.RateLimit;
import com.ican.cache.MultiLevelCacheManager;
import com.ican.entity.*;
import com.ican.mapper.*;
import com.ican.metrics.BlogMetrics;
import com.ican.model.dto.*;
import com.ican.model.dto.ArticleAiMessage;
import com.ican.model.vo.*;
import com.ican.service.ArticleService;
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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.ican.constant.CommonConstant.FALSE;
import static com.ican.constant.PersonConstant.MY_MAIL;
import static com.ican.constant.PersonConstant.MY_RED_MAIL;
import static com.ican.constant.RedisConstant.*;
import static com.ican.constant.MqConstant.ARTICLE_AI_EXCHANGE;
import static com.ican.constant.MqConstant.ARTICLE_AI_KEY;
import static com.ican.enums.ArticleStatusEnum.PUBLIC;
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

    private final UserMapper userMapper;

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

    private final RabbitTemplate rabbitTemplate;

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
        baseMapper.insert(newArticle);
        // 保存文章标签
        saveArticleTag(article, newArticle.getId());
        // 发送文章 AI 异步处理消息
        sendArticleAiMessage(newArticle.getId(), newArticle.getArticleTitle(), newArticle.getArticleContent());
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
        baseMapper.updateById(newArticle);
        // 保存文章标签
        saveArticleTag(article, newArticle.getId());
        // 清除缓存
        cacheManager.evict("article:" + newArticle.getId());
        // 发送文章 AI 异步处理消息
        sendArticleAiMessage(newArticle.getId(), newArticle.getArticleTitle(), newArticle.getArticleContent());
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

        String email = getCurrentUserEmail();
        boolean isSpecialEmail = checkSpecialEmail(email);
        List<ArticleHomeVO> articles = queryArticles(sort, tagId, start, end, isSpecialEmail);
        if (CollectionUtils.isEmpty(articles)) {
            return new PageResult<>();
        }
        PageResult<ArticleHomeVO> result = paginateResults(articles);
        processHotArticles(articles);
        return result;
    }

    /**
     * 缓存热点文章内容（使用多级缓存管理器）
     */
    private void processHotArticles(List<ArticleHomeVO> articles) {
        Map<Object, Double> viewCountMap = redisService.getZsetAllScore(ARTICLE_VIEW_COUNT);
        Set<Integer> hotCandidates = new HashSet<>();

        for (ArticleHomeVO article : articles) {
            article.setArticleContent(article.getArticleContent().replaceAll("#", ""));
            sortTags(article.getTagVOList());
            Double viewCount = viewCountMap.getOrDefault(article.getId(), 0D);

            if (viewCount >= 20 || article.getIsTop() == 1) {
                hotCandidates.add(article.getId());
            }
        }

        if (!hotCandidates.isEmpty()) {
            CompletableFuture.runAsync(() -> batchProcessHotArticles(hotCandidates), hotArticleExecutor);
        }
    }

    private void batchProcessHotArticles(Set<Integer> articleIds) {
        articleIds.forEach(id -> {
            String cacheKey = "article:" + id;
            // 使用多级缓存管理器预热缓存
            cacheManager.get(cacheKey, key -> {
                ArticleVO article = articleMapper.selectArticleHomeById(id);
                if (article != null) {
                    updateArticleStatsFromRedis(id, article);
                }
                return article;
            }, 30); // 30 分钟过期
        });
    }

    /**
     * 查询文章详情（使用多级缓存 + 限流）
     */
    @Override
    @RateLimit(key = "api:article:view:#{#articleId}", limit = 10, period = 60)
    public ArticleVO getArticleHomeById(Integer articleId) {
        String cacheKey = "article:" + articleId;

        // 使用多级缓存管理器（自动处理缓存穿透、击穿、雪崩）
        ArticleVO article = cacheManager.get(cacheKey, key -> {
            ArticleVO dbArticle = articleMapper.selectArticleHomeById(articleId);
            if (dbArticle != null) {
                updateArticleStatsFromRedis(articleId, dbArticle);
            }
            return dbArticle;
        }, 30); // 30 分钟过期

        return article;
    }

    private void updateArticleStatsFromRedis(Integer articleId, ArticleVO articleVO) {
        blogMetrics.incrementArticleView(articleId);
        Double viewCount = Optional.ofNullable(redisService
                .getZsetScore(ARTICLE_VIEW_COUNT, articleId)).orElse((double) 0);
        // 缓存中浏览量+1
        redisService.incrZet(ARTICLE_VIEW_COUNT, articleId, 1D);
        Integer likeCount = redisService.getHash(ARTICLE_LIKE_COUNT, articleId.toString());
        // 查询下一篇文章
        ArticlePaginationVO lastArticle = articleMapper.selectLastArticle(articleId);
        ArticlePaginationVO nextArticle = articleMapper.selectNextArticle(articleId);
        articleVO.setLikeCount(Optional.ofNullable(likeCount).orElse(0));
        articleVO.setLastArticle(lastArticle);
        articleVO.setNextArticle(nextArticle);
        articleVO.setViews(viewCount.intValue() + 1);
        // 异步更新数据库
        CompletableFuture.runAsync(() ->
                articleMapper.incrementViews(Long.valueOf(articleId)), hotArticleExecutor);
    }

    @Override
    public PageResult<ArchiveVO> listArchiveVO() {
        // 查询文章数量
        Long count = articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                .eq(Article::getIsDelete, FALSE)
                .eq(Article::getStatus, PUBLIC.getStatus()));
        if (count == 0) {
            return new PageResult<>();
        }
        String email = null;
        if (StpUtil.isLogin()) {
            int userId = StpUtil.getLoginIdAsInt();
            email = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .select(User::getEmail).eq(User::getId, userId)).getEmail();
        }
        // 查询用户信息
        if (ObjectUtil.isNotNull(email) && (email.equals(MY_MAIL) || email.equals(MY_RED_MAIL))) {
            // 查询首页文章
            List<ArchiveVO> archiveList = articleMapper.PselectArchiveList(PageUtils.getLimit(), PageUtils.getSize());
            return new PageResult<>(archiveList, count);
        } else {
            List<ArchiveVO> articleHomeVOList = articleMapper.selectArchiveList(PageUtils.getLimit(), PageUtils.getSize());
            return new PageResult<>(articleHomeVOList, count);
        }

    }

    @Override
    public List<ArticleRecommendVO> listArticleRecommendVO() {
        return articleMapper.selectArticleRecommend();
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

    // 获取当前用户邮箱
    private String getCurrentUserEmail() {
        try {

            if (StpUtil.isLogin()) {
                User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                        .select(User::getEmail)
                        .eq(User::getId, StpUtil.getLoginIdAsInt()));
                return user != null ? user.getEmail() : null;
            }
            return null;
        } catch (Exception e) {
            log.error("获取用户邮箱失败", e);
            return null;
        }
    }

    // 判断是否特权邮箱
    private boolean checkSpecialEmail(String email) {
        return ObjectUtil.isNotNull(email) && (email.equals(MY_MAIL) || email.equals(MY_RED_MAIL));
    }

    // 查询文章列表（核心查询方法）
    private List<ArticleHomeVO> queryArticles(String sort, Integer tagId, String start,
                                              String end, boolean isSpecial) {
        if (isSpecial) {
            // 特权用户查询所有文章（包括未发布）
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

    private void sendArticleAiMessage(Integer articleId, String title, String content) {
        if (articleId == null || StringUtils.isBlank(content)) {
            return;
        }
        ArticleAiMessage message = new ArticleAiMessage();
        message.setArticleId(articleId);
        message.setArticleTitle(title);
        message.setArticleContent(content);
        rabbitTemplate.convertAndSend(ARTICLE_AI_EXCHANGE, ARTICLE_AI_KEY, message);
    }
}
