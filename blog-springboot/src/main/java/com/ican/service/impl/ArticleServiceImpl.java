package com.ican.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
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
 * @author ican
 * @date 2022/12/04 22:31
 **/
@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ArticleTagMapper articleTagMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private TagService tagService;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private RedisService redisService;

    @Autowired
    private SearchStrategyContext searchStrategyContext;

    @Autowired
    private UploadStrategyContext uploadStrategyContext;

    @Autowired
    private BlogFileMapper blogFileMapper;

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
            redisService.setHash(cacheKey, articleId.toString(), JSONUtil.toJsonStr(article), 1, TimeUnit.HOURS);
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

    // 查询文章信息
    @Override
    public ArticleVO getArticleHomeById(Integer articleId) {
        // Redis 缓存 Key
        String cacheKey = HOT_ARTICLE;
        String articleJsonStr = redisService.getHash(cacheKey,articleId.toString());
        // 热点文章和常规文章划分
        if (StrUtil.isEmpty(articleJsonStr)) {
            cacheKey = USUAL_ARTICLE;
            articleJsonStr = redisService.getHash(cacheKey,articleId.toString());
        }
        ArticleVO article = JSONUtil.toBean(articleJsonStr, ArticleVO.class);
        if (Objects.isNull(articleJsonStr)) {
            // 2. 缓存未命中，从数据库加载文章信息
            article = articleMapper.selectArticleHomeById(articleId);
            if (Objects.isNull(article)) {
                return null;
            }
            updateArticleStatsFromRedis(articleId,article);
            // 3. 缓存文章基本信息
            cacheArticleDetails(cacheKey, article);
        }
        return article;
    }

    private void updateArticleStatsFromRedis(Integer articleId,ArticleVO articleVO) {
        Double viewCount = Optional.ofNullable(redisService
                        .getZsetScore(ARTICLE_VIEW_COUNT, articleId)).orElse((double) 0);
        // 浏览量+1
        articleMapper.incrementViews(Long.valueOf(articleId));
        redisService.incrZet(ARTICLE_VIEW_COUNT, articleId, 1D);
        Integer likeCount = redisService.getHash(ARTICLE_LIKE_COUNT, articleId.toString());
        // 查询下一篇文章
        ArticlePaginationVO lastArticle = articleMapper.selectLastArticle(articleId);
        ArticlePaginationVO nextArticle = articleMapper.selectNextArticle(articleId);
        articleVO.setLikeCount(Optional.ofNullable(likeCount).orElse(0));
        articleVO.setLastArticle(lastArticle);
        articleVO.setNextArticle(nextArticle);
        articleVO.setViewCount(viewCount.intValue());
    }

    private void cacheArticleDetails(String cacheKey, ArticleVO article) {
        // 缓存文章信息（1小时TTL）
        if(redisService.hasHashValue(cacheKey,article.getId().toString())){
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
}
