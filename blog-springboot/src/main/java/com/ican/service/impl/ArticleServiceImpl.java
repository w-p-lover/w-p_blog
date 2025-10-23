package com.ican.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ican.entity.*;
import com.ican.mapper.*;
import com.ican.model.dto.*;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.google.common.util.concurrent.Striped;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static com.ican.constant.CommonConstant.FALSE;
import static com.ican.constant.PersonConstant.MY_MAIL;
import static com.ican.constant.PersonConstant.MY_RED_MAIL;
import static com.ican.constant.RedisConstant.*;
import static com.ican.enums.ArticleStatusEnum.PUBLIC;
import static com.ican.enums.FilePathEnum.ARTICLE;

/**
 * 文章业务接口实现类
 *
 * @author xcs
 * @date 2022/12/04 22:31
 **/
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

    @Autowired
    private ThreadPoolTaskExecutor hotArticleExecutor;

    // 使用Guava的Striped锁插件，在细粒度竞争时保证高性能，并且尽量避免锁长期保存的内存OMM问题
    @SuppressWarnings("UnstableApiUsage")
    public static final Striped<Lock> articleLocks =
            Striped.lock(Runtime.getRuntime().availableProcessors() * 2);

    Cache<Integer, ArticleVO> localCache = Caffeine.newBuilder()
            .maximumSize(200)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

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
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteArticle(List<Integer> articleIdList) {
        // 删除文章标签
        articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>()
                .in(ArticleTag::getArticleId, articleIdList));
        // 删除文章
        articleMapper.deleteBatchIds(articleIdList);
    }

    @Override
    @Transactional
    public void updateArticleDelete(DeleteDTO delete) {
        // 批量更新文章删除状态
        List<Article> articleList = delete.getIdList()
                .stream()
                .map(id -> Article.builder()
                        .id(id)
                        .isDelete(delete.getIsDelete())
                        .isTop(FALSE)
                        .isRecommend(FALSE)
                        .build())
                .collect(Collectors.toList());
        this.updateBatchById(articleList);
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
    }

    @Override
    public void updateArticleRecommend(RecommendDTO recommend) {
        // 修改文章推荐状态
        Article newArticle = Article.builder()
                .id(recommend.getId())
                .isRecommend(recommend.getIsRecommend())
                .build();
        articleMapper.updateById(newArticle);
    }

    @Override
    public List<ArticleSearchVO> listArticlesBySearch(String keyword) {
        return searchStrategyContext.executeSearchStrategy(keyword);
    }


    /**
     * 缓存热点文章内容
     *
     * @param articleId 文章ID
     */
    private void cacheHotArticleContent(Integer articleId) {
        // 检查是否已缓存
        String cacheKey = HOT_ARTICLE;
        if (redisService.hasHashValue(cacheKey, articleId.toString())) {
            redisService.setExpire(cacheKey, 1, TimeUnit.HOURS);
            return;
        }
        ArticleVO article = articleMapper.selectArticleHomeById(articleId);
        if (article != null) {
            updateArticleStatsFromRedis(articleId,article);
            redisService.setHash(cacheKey, articleId.toString(),
                    JSONUtil.toJsonStr(article), 1, TimeUnit.HOURS);
        }
    }

    @Override
    public PageResult<ArticleHomeVO> listArticleHomeVO(String sort, Integer tagId, String start, String end) {
        // 获取登录用户的电子邮件
        String email = null;
        if (StpUtil.isLogin()) {
            User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .select(User::getEmail)
                    .eq(User::getId, StpUtil.getLoginIdAsInt()));
            email = user != null ? user.getEmail() : null; // 防止空指针
        }

        // 判断是否使用特殊邮件
        boolean isSpecialEmail = ObjectUtil.isNotNull(email) && (email.equals(MY_MAIL) || email.equals(MY_RED_MAIL));

        // 获取文章列表
        List<ArticleHomeVO> articleHomeVOS = isSpecialEmail
                ? articleMapper.selectArticleAllList(sort, tagId, start, end)
                : articleMapper.PselectArticleAllList(sort, tagId, start, end);

        long count = articleHomeVOS.size();
        if (count == 0) {
            return new PageResult<>();
        }

        // 处理分页逻辑
        int itemStart = (int) ((PageUtils.getCurrent() - 1) * PageUtils.getSize());
        int itemEnd = (int) Math.min(itemStart + PageUtils.getSize(), (int) count);
        List<ArticleHomeVO> paginatedArticles = articleHomeVOS.subList(itemStart, itemEnd);

        // 如果指定标签 ID，直接返回分页结果
        if (ObjectUtil.isNotNull(tagId)) {
            return new PageResult<>(paginatedArticles, count);
        }
        // 浏览量
        Map<Object, Double> viewCountMap = redisService.getZsetAllScore(ARTICLE_VIEW_COUNT);
        paginatedArticles.forEach(item -> {
            item.setArticleContent(item.getArticleContent().replaceAll("#", ""));
            item.getTagVOList().sort(Comparator.comparingInt(TagOptionVO::getId));
            Double viewCount = Optional.ofNullable(viewCountMap.get(item.getId())).orElse((double) 0);
            if (viewCount >= 20 || item.getIsTop() == 1) { // 热点文章阈值
                cacheHotArticleContent(item.getId());
            }
        });
        return new PageResult<>(paginatedArticles, count);
    }

    // 处理热点文章逻辑
    private void processHotArticles(List<ArticleHomeVO> articles) {
        // 批量获取所有文章的浏览数
        Map<Object, Double> viewCountMap = redisService.getZsetAllScore(ARTICLE_VIEW_COUNT);
        Set<Integer> hotCandidates = new HashSet<>(); // 线程安全集合：收集需要异步处理的热点文章ID
        // 顺序流处理文章，保证线程安全
        for (ArticleHomeVO article : articles) {
            article.setArticleContent(article.getArticleContent().replaceAll("#", ""));
            sortTags(article.getTagVOList());
            Double viewCount = viewCountMap.getOrDefault(article.getId(), 0D);
            // 判断是否为热点文章（浏览数≥20 或 置顶文章）
            if (viewCount >= 20 || article.getIsTop() == 1) {
                hotCandidates.add(article.getId());  // 将热点文章ID加入集合
            }
        }
        if (!hotCandidates.isEmpty()) { // 批量异步处理热点文章
            CompletableFuture.runAsync(() -> batchProcessHotArticles(hotCandidates), hotArticleExecutor);
        }
    }

    // 批量处理热点文章
    private void batchProcessHotArticles(Set<Integer> articleIds) {
        articleIds.forEach(id -> {
            if (needCacheUpdate(id)) {  // 检查是否需要更新缓存
                asyncCacheHotArticle(id);  // 异步缓存热点文章
            }
        });
    }

    // 判断是否需要更新缓存的逻辑
    private boolean needCacheUpdate(Integer articleId) {
        // 获取当前文章的缓存
        String cacheKey = HOT_ARTICLE;
        if (!redisService.hasHashValue(cacheKey, articleId.toString())) {  // 1. 检查缓存是否存在
            return true;
        }
        Long ttl = redisService.getExpire(cacheKey, TimeUnit.MINUTES);// 2. 检查缓存是否过期（TTL）
        if (ttl != null && ttl < 5) {
            return true;
        }
        Double viewCount = redisService.getZsetScore(ARTICLE_VIEW_COUNT, articleId); // 3. 检查浏览量是否变化（假设超过某个阈值就认为需要更新缓存）
        if (viewCount != null && viewCount > 1000) {
            return true;
        }
        ArticleVO article = articleMapper.selectArticleHomeById(articleId);// 4. 检查数据库中的文章内容是否有更新
        if (article == null) {
            return false;
        }
        return false;
    }


    // 异步缓存热点文章（保持哈希结构）
    @Async("cacheRefreshPool")
    protected void asyncCacheHotArticle(Integer articleId) {
        try {
            final String cacheKey = HOT_ARTICLE;
            // 第一层快速检查（非原子性检查，允许少量重复）
            if (redisService.hasHashValue(cacheKey, articleId.toString())) {
                // 动态延长哈希整体过期时间（30±5分钟）
                redisService.setExpire(cacheKey, 25 + (int) (Math.random() * 10), TimeUnit.MINUTES);
                return;
            }
            // 使用本地锁（ReentrantLock）
            ReentrantLock lock = (ReentrantLock) articleLocks.get(articleId);
            lock.lock();  // 获取锁
            try {
                // 第二层精确检查
                if (redisService.hasHashValue(cacheKey, articleId.toString())) {
                    return;
                }
                // 查询数据库并更新缓存
                ArticleVO article = articleMapper.selectArticleHomeById(articleId);
                if (article != null) {
                    // 缓存数据并设置动态TTL
                    redisService.setHash(
                            cacheKey,
                            articleId.toString(),
                            JSONUtil.toJsonStr(article)
                    );
                    redisService.setExpire(cacheKey,
                            getDynamicTTL(articleId),
                            TimeUnit.MINUTES);
                }
            } finally {
                lock.unlock();  // 释放锁
            }
        } catch (Exception e) {
            log.error("异步缓存哈希结构热点文章失败", e);
        }
    }

    // 根据文章的阅读量来计算动态TTL
    private int getDynamicTTL(Integer articleId) {
        int baseTTL = 30;
        int maxTTL = 60;

        // 获取文章的浏览量
        Double viewCount = redisService.getZsetScore(ARTICLE_VIEW_COUNT, articleId);
        if (viewCount == null) {
            viewCount = 0D; // 如果缓存中没有浏览量，则默认值为 0
        }
        double logFactor = Math.log10(viewCount + 1) * 20;
        int dynamicTTL = baseTTL + (int) Math.min(maxTTL, logFactor);

        // 随机增加偏移量（避免缓存雪崩）符合正态分布的偏移量（μ=5min, σ=2min）
        double normalOffset = new Random().nextGaussian() * 2 + 5;
        int safeOffset = (int) Math.min(10, Math.max(0, normalOffset));
        return dynamicTTL + safeOffset;
    }

    // 查询文章信息
    @Override
    public ArticleVO getArticleHomeById(Integer articleId) {
        // 1. 加入本地缓存层
        String nullCacheKey = "cache:null:article:" + articleId.toString();
        String cachedNullValue = redisService.getObject(nullCacheKey);
        // 如果缓存了空值，直接返回 null
        if ("EMPTY".equals(cachedNullValue)) {
            return null;
        }
        ArticleVO article = localCache.get(articleId, Id -> {
            // 2. 使用 ReentrantLock 来替代 synchronized，提升性能
            ReentrantLock lock = (ReentrantLock) articleLocks.get(articleId);
            lock.lock();  // 获取锁
            try {
                // 3. 检查 Redis 缓存
                String articleJson = redisService.getHash(HOT_ARTICLE, articleId.toString());
                if (articleJson != null)
                    return JSONUtil.toBean(articleJson, ArticleVO.class);
                // 4. 从数据库查询
                ArticleVO dbArticle = articleMapper.selectArticleHomeById(articleId);
                if (dbArticle == null) {
                    redisService.setObject(nullCacheKey, "EMPTY", 5, TimeUnit.MINUTES);
                    return null;
                }
                // 5. 更新本地缓存和 Redis
                updateArticleStatsFromRedis(articleId, dbArticle);
                // localCache.put(articleId, dbArticle);   更新本地缓存
                redisService.setHash(HOT_ARTICLE, articleId.toString(), JSONUtil.toJsonStr(dbArticle));  // 更新 Redis
                return dbArticle;
            } finally {
                lock.unlock();
            }
        });
        return Optional.ofNullable(article)
                .map(a -> {
                    updateArticleStatsFromRedis(articleId, a);
                    return a;  // 返回更新后的 article
                })
                .orElse(null);
    }

    private void updateArticleStatsFromRedis(Integer articleId, ArticleVO articleVO) {
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
        //异步对数据库数据更新，实现 优先更新 Redis，异步更新数据库 的策略。
        CompletableFuture.runAsync(() ->
                articleMapper.incrementViews(Long.valueOf(articleId)), hotArticleExecutor);
    }

    private void cacheArticleDetails(String cacheKey, ArticleVO article) {
        // 缓存文章信息（15分钟TTL）
        if (redisService.hasHashValue(cacheKey, article.getId().toString())) {
            redisService.setExpire(cacheKey, 15, TimeUnit.MINUTES);
        }
        redisService.setHash(cacheKey, article.getId().toString(), JSONUtil.toJsonStr(article), 15, TimeUnit.MINUTES);
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
}
