# Phase2 Cache Hot Score Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有多级缓存体系之上，交付文章热度分、Redis ZSet 热榜、热点文章缓存预热、缓存管理接口、指标和阶段总结文档。

**Architecture:** 保留当前 `MultiLevelCacheManager` 作为文章详情缓存基础设施，不做全站 Redis 大重构。新增 `ArticleHotScoreService` 负责聚合浏览、点赞、评论、收藏、推荐、置顶和时间衰减，使用 Redis ZSet 维护 `blog:article:hot_score`；新增 `HotArticleWarmupService` 根据热榜预热 `article:{id}` 详情缓存，并通过 `CacheController` 和 `TimedTask` 支持手动与定时触发。

**Tech Stack:** Spring Boot 3.2.5, Java 17, MyBatis-Plus, Redis ZSet, Caffeine, Redisson, Quartz, Micrometer, JUnit 5, Mockito

---

## Branch And Baseline

- Development branch: `codex/phase2-cache-audit`
- Base branch: local `wyp`
- Baseline command already verified:

```powershell
$env:JAVA_HOME='C:\Users\IT074\.jdks\ms-21.0.10'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; & 'D:\IntelliJ IDEA 2025.2.4\plugins\maven\lib\maven3\bin\mvn.cmd' test
```

Baseline result:

```text
Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## File Structure

| Operation | File | Responsibility |
|---|---|---|
| Modify | `src/main/java/com/ican/entity/UserFavorite.java` | Keep entity contract aligned with `favorite_id` and article favorite semantics |
| Modify | `src/main/java/com/ican/mapper/UserFavoriteMapper.java` | Use `Integer favoriteId`; add article favorite count query |
| Modify | `src/main/resources/mapper/UserFavoriteMapper.xml` | Replace wrong `doc_id` column with `favorite_id`; filter article favorites |
| Create | `src/test/java/com/ican/mapper/UserFavoriteMapperContractTest.java` | Guard XML contract so `doc_id` does not come back |
| Create | `src/main/java/com/ican/cache/ArticleHotScoreProperties.java` | Configurable weights, limits, TTL and warmup settings |
| Modify | `src/main/java/com/ican/constant/RedisConstant.java` | Add hot score key, article cache prefix and warmup/refresh locks |
| Create | `src/main/java/com/ican/model/vo/ArticleHotScoreSourceVO.java` | Database source projection for hot score calculation |
| Create | `src/main/java/com/ican/model/vo/ArticleHotScoreVO.java` | Admin/top-list hot score response |
| Create | `src/main/java/com/ican/model/vo/ArticleFavoriteCountVO.java` | Article favorite count projection |
| Create | `src/main/java/com/ican/model/vo/ArticleHotScoreRefreshResultVO.java` | Refresh result response |
| Create | `src/main/java/com/ican/model/vo/HotArticleWarmupResultVO.java` | Warmup result response |
| Modify | `src/main/java/com/ican/service/RedisService.java` | Add exact ZSet score write and ordered read contract |
| Modify | `src/main/java/com/ican/service/impl/RedisServiceImpl.java` | Implement exact ZSet score write and preserve ZSet order |
| Create | `src/test/java/com/ican/service/impl/RedisServiceImplTest.java` | Verify ZSet helpers without real Redis |
| Modify | `src/main/java/com/ican/mapper/ArticleMapper.java` | Add hot score source query |
| Modify | `src/main/resources/mapper/ArticleMapper.xml` | Add hot score source SQL |
| Modify | `src/main/java/com/ican/mapper/CommentMapper.java` | Add article comment count query |
| Modify | `src/main/resources/mapper/CommentMapper.xml` | Add article comment count SQL |
| Create | `src/main/java/com/ican/service/ArticleHotScoreService.java` | Hot score refresh/top-list contract |
| Create | `src/main/java/com/ican/service/impl/ArticleHotScoreServiceImpl.java` | Calculate and write Redis ZSet hot scores |
| Create | `src/test/java/com/ican/service/impl/ArticleHotScoreServiceImplTest.java` | Verify formula, data aggregation and Redis ZSet writes |
| Create | `src/main/java/com/ican/service/HotArticleWarmupService.java` | Hot article warmup contract |
| Create | `src/main/java/com/ican/service/impl/HotArticleWarmupServiceImpl.java` | Warm article detail cache from hot score ranking |
| Create | `src/test/java/com/ican/service/impl/HotArticleWarmupServiceImplTest.java` | Verify warmup behavior and skipped/failed counts |
| Modify | `src/main/java/com/ican/service/impl/ArticleServiceImpl.java` | Delegate old candidate warmup to warmup service; keep existing behavior |
| Modify | `src/main/java/com/ican/controller/CacheController.java` | Add hot score refresh, hot score top list and warmup endpoints |
| Create | `src/test/java/com/ican/controller/CacheControllerTest.java` | Verify admin endpoints call services without real Redis |
| Modify | `src/main/java/com/ican/quartz/task/TimedTask.java` | Add schedulable methods for refresh and warmup |
| Modify | `src/main/java/com/ican/metrics/BlogMetrics.java` | Add hot score refresh and warmup metrics |
| Modify | `src/test/java/com/ican/metrics/BlogMetricsTest.java` | Verify new metric counters/timers |
| Modify | `src/main/resources/application.yml` | Add `article.hot-score` weights and limits |
| Create | `docs/03-缓存体系/02-Phase2-文章热度分与热点预热总结.md` | Stage summary required by user workflow |

---

### Task 1: Fix UserFavorite Column Contract

**Files:**
- Modify: `src/main/java/com/ican/entity/UserFavorite.java`
- Modify: `src/main/java/com/ican/mapper/UserFavoriteMapper.java`
- Modify: `src/main/resources/mapper/UserFavoriteMapper.xml`
- Create: `src/main/java/com/ican/model/vo/ArticleFavoriteCountVO.java`
- Create: `src/test/java/com/ican/mapper/UserFavoriteMapperContractTest.java`

- [ ] **Step 1: Write the failing mapper contract test**

Create `src/test/java/com/ican/mapper/UserFavoriteMapperContractTest.java`:

```java
package com.ican.mapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class UserFavoriteMapperContractTest {

    @Test
    void mapperXml_shouldUseFavoriteIdColumnInsteadOfDocId() throws Exception {
        String xml = Files.readString(Path.of("src/main/resources/mapper/UserFavoriteMapper.xml"));

        assertThat(xml).contains("favorite_id");
        assertThat(xml).doesNotContain("doc_id");
    }
}
```

- [ ] **Step 2: Run test and verify it fails**

Run:

```powershell
mvn -Dtest=UserFavoriteMapperContractTest test
```

Expected: FAIL because current XML still contains `doc_id`.

- [ ] **Step 3: Add article favorite count VO**

Create `src/main/java/com/ican/model/vo/ArticleFavoriteCountVO.java`:

```java
package com.ican.model.vo;

import lombok.Data;

@Data
public class ArticleFavoriteCountVO {
    private Integer articleId;
    private Integer favoriteCount;
}
```

- [ ] **Step 4: Update mapper interface to use `favorite_id` semantics**

Modify `UserFavoriteMapper.java`:

```java
List<Integer> selectFavoriteIdsByUserId(@Param("userId") Integer userId);

UserFavorite selectByUserIdAndFavoriteId(@Param("userId") Integer userId,
                                         @Param("favoriteId") Integer favoriteId);

List<Integer> selectUserIdsByFavoriteId(@Param("favoriteId") Integer favoriteId);

Integer countByFavoriteId(@Param("favoriteId") Integer favoriteId);

List<ArticleFavoriteCountVO> selectArticleFavoriteCount();
```

Add import:

```java
import com.ican.model.vo.ArticleFavoriteCountVO;
```

- [ ] **Step 5: Replace `doc_id` in mapper XML**

Modify `UserFavoriteMapper.xml`:

```xml
<sql id="Base_Column_List">
    id, user_id, favorite_id, favorite_type, create_time, update_time
</sql>

<select id="selectFavoriteIdsByUserId" resultType="java.lang.Integer">
    SELECT favorite_id
    FROM t_user_favorite
    WHERE user_id = #{userId}
      AND is_deleted = 0
</select>

<select id="selectByUserIdAndFavoriteId" resultType="com.ican.entity.UserFavorite">
    SELECT
    <include refid="Base_Column_List"/>
    FROM t_user_favorite
    WHERE user_id = #{userId}
      AND favorite_id = #{favoriteId}
      AND is_deleted = 0
</select>

<select id="selectUserIdsByFavoriteId" resultType="java.lang.Integer">
    SELECT user_id
    FROM t_user_favorite
    WHERE favorite_id = #{favoriteId}
      AND is_deleted = 0
</select>

<select id="countByFavoriteId" resultType="java.lang.Integer">
    SELECT COUNT(1)
    FROM t_user_favorite
    WHERE favorite_id = #{favoriteId}
      AND is_deleted = 0
</select>

<select id="selectArticleFavoriteCount" resultType="com.ican.model.vo.ArticleFavoriteCountVO">
    SELECT favorite_id AS articleId,
           COUNT(1) AS favoriteCount
    FROM t_user_favorite
    WHERE favorite_type = 2
      AND is_deleted = 0
    GROUP BY favorite_id
</select>
```

- [ ] **Step 6: Run the contract test**

Run:

```powershell
mvn -Dtest=UserFavoriteMapperContractTest test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add src/main/java/com/ican/entity/UserFavorite.java src/main/java/com/ican/mapper/UserFavoriteMapper.java src/main/java/com/ican/model/vo/ArticleFavoriteCountVO.java src/main/resources/mapper/UserFavoriteMapper.xml src/test/java/com/ican/mapper/UserFavoriteMapperContractTest.java
git commit -m "fix: 修正收藏表字段映射"
```

---

### Task 2: Add Hot Score Configuration And Models

**Files:**
- Create: `src/main/java/com/ican/cache/ArticleHotScoreProperties.java`
- Modify: `src/main/java/com/ican/constant/RedisConstant.java`
- Create: `src/main/java/com/ican/model/vo/ArticleHotScoreSourceVO.java`
- Create: `src/main/java/com/ican/model/vo/ArticleHotScoreVO.java`
- Create: `src/main/java/com/ican/model/vo/ArticleHotScoreRefreshResultVO.java`
- Create: `src/main/java/com/ican/model/vo/HotArticleWarmupResultVO.java`
- Modify: `src/main/resources/application.yml`
- Create: `src/test/java/com/ican/cache/ArticleHotScorePropertiesTest.java`

- [ ] **Step 1: Write the failing properties test**

Create `src/test/java/com/ican/cache/ArticleHotScorePropertiesTest.java`:

```java
package com.ican.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleHotScorePropertiesTest {

    @Test
    void defaults_shouldMatchPhase2HotScoreModel() {
        ArticleHotScoreProperties properties = new ArticleHotScoreProperties();

        assertThat(properties.getTopLimit()).isEqualTo(20);
        assertThat(properties.getWarmupLimit()).isEqualTo(10);
        assertThat(properties.getDetailCacheTtlMinutes()).isEqualTo(30);
        assertThat(properties.getWeights().getView()).isEqualTo(1.0);
        assertThat(properties.getWeights().getLike()).isEqualTo(5.0);
        assertThat(properties.getWeights().getComment()).isEqualTo(8.0);
        assertThat(properties.getWeights().getFavorite()).isEqualTo(10.0);
        assertThat(properties.getWeights().getRecommend()).isEqualTo(30.0);
        assertThat(properties.getWeights().getTop()).isEqualTo(50.0);
        assertThat(properties.getWeights().getTimeDecayPerDay()).isEqualTo(1.5);
    }
}
```

- [ ] **Step 2: Run test and verify it fails**

Run:

```powershell
mvn -Dtest=ArticleHotScorePropertiesTest test
```

Expected: FAIL because `ArticleHotScoreProperties` does not exist.

- [ ] **Step 3: Create properties class**

Create `src/main/java/com/ican/cache/ArticleHotScoreProperties.java`:

```java
package com.ican.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "article.hot-score")
public class ArticleHotScoreProperties {

    private int topLimit = 20;
    private int warmupLimit = 10;
    private int detailCacheTtlMinutes = 30;
    private Weights weights = new Weights();

    @Data
    public static class Weights {
        private double view = 1.0;
        private double like = 5.0;
        private double comment = 8.0;
        private double favorite = 10.0;
        private double recommend = 30.0;
        private double top = 50.0;
        private double timeDecayPerDay = 1.5;
    }
}
```

- [ ] **Step 4: Add Redis constants**

Modify `RedisConstant.java`:

```java
/**
 * 文章详情缓存前缀
 */
public static final String ARTICLE_DETAIL_PREFIX = "article:";

/**
 * 文章热度分 ZSet
 */
public static final String ARTICLE_HOT_SCORE = "blog:article:hot_score";

/**
 * 文章热度分刷新锁
 */
public static final String ARTICLE_HOT_REFRESH_LOCK = "lock:article:hot:refresh";

/**
 * 热点文章预热锁
 */
public static final String ARTICLE_HOT_WARMUP_LOCK = "lock:article:hot:warmup";
```

- [ ] **Step 5: Create hot score VOs**

Create `ArticleHotScoreSourceVO.java`:

```java
package com.ican.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleHotScoreSourceVO {
    private Integer articleId;
    private String articleTitle;
    private Integer views;
    private Integer isTop;
    private Integer isRecommend;
    private LocalDateTime createTime;
}
```

Create `ArticleHotScoreVO.java`:

```java
package com.ican.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleHotScoreVO {
    private Integer articleId;
    private String articleTitle;
    private Double hotScore;
}
```

Create `ArticleHotScoreRefreshResultVO.java`:

```java
package com.ican.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ArticleHotScoreRefreshResultVO {
    private Integer refreshedCount;
    private Long costMillis;
}
```

Create `HotArticleWarmupResultVO.java`:

```java
package com.ican.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HotArticleWarmupResultVO {
    private Integer requestedCount;
    private Integer warmedCount;
    private Integer skippedCount;
    private Integer failedCount;
    private Long costMillis;
}
```

- [ ] **Step 6: Add YAML configuration**

Add to `application.yml`:

```yaml
article:
  hot-score:
    top-limit: ${BLOG_ARTICLE_HOT_TOP_LIMIT:20}
    warmup-limit: ${BLOG_ARTICLE_HOT_WARMUP_LIMIT:10}
    detail-cache-ttl-minutes: ${BLOG_ARTICLE_HOT_DETAIL_CACHE_TTL_MINUTES:30}
    weights:
      view: ${BLOG_ARTICLE_HOT_WEIGHT_VIEW:1.0}
      like: ${BLOG_ARTICLE_HOT_WEIGHT_LIKE:5.0}
      comment: ${BLOG_ARTICLE_HOT_WEIGHT_COMMENT:8.0}
      favorite: ${BLOG_ARTICLE_HOT_WEIGHT_FAVORITE:10.0}
      recommend: ${BLOG_ARTICLE_HOT_WEIGHT_RECOMMEND:30.0}
      top: ${BLOG_ARTICLE_HOT_WEIGHT_TOP:50.0}
      time-decay-per-day: ${BLOG_ARTICLE_HOT_WEIGHT_DECAY_PER_DAY:1.5}
```

- [ ] **Step 7: Run test**

Run:

```powershell
mvn -Dtest=ArticleHotScorePropertiesTest test
```

Expected: PASS.

- [ ] **Step 8: Commit**

```powershell
git add src/main/java/com/ican/cache/ArticleHotScoreProperties.java src/main/java/com/ican/constant/RedisConstant.java src/main/java/com/ican/model/vo/ArticleHotScoreSourceVO.java src/main/java/com/ican/model/vo/ArticleHotScoreVO.java src/main/java/com/ican/model/vo/ArticleHotScoreRefreshResultVO.java src/main/java/com/ican/model/vo/HotArticleWarmupResultVO.java src/main/resources/application.yml src/test/java/com/ican/cache/ArticleHotScorePropertiesTest.java
git commit -m "feat: 增加文章热度分配置"
```

---

### Task 3: Add Redis ZSet Hot Ranking Helpers

**Files:**
- Modify: `src/main/java/com/ican/service/RedisService.java`
- Modify: `src/main/java/com/ican/service/impl/RedisServiceImpl.java`
- Create: `src/test/java/com/ican/service/impl/RedisServiceImplTest.java`

- [ ] **Step 1: Write failing Redis service test**

Create `RedisServiceImplTest.java`:

```java
package com.ican.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @InjectMocks
    private RedisServiceImpl redisService;

    @Test
    void setZsetScore_shouldWriteExactScore() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        redisService.setZsetScore("blog:article:hot_score", 1, 88.5);

        verify(zSetOperations).add("blog:article:hot_score", 1, 88.5);
    }

    @Test
    void zReverseRangeWithScore_shouldKeepRedisOrder() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        Set<ZSetOperations.TypedTuple<Object>> tuples = new LinkedHashSet<>(List.of(
                new TestTuple(2, 91.0),
                new TestTuple(1, 88.5)
        ));
        when(zSetOperations.reverseRangeWithScores("blog:article:hot_score", 0, 1)).thenReturn(tuples);

        Map<Object, Double> result = redisService.zReverseRangeWithScore("blog:article:hot_score", 0, 1);

        assertThat(result).containsExactly(Map.entry(2, 91.0), Map.entry(1, 88.5));
    }

    private record TestTuple(Object value, Double score) implements ZSetOperations.TypedTuple<Object> {
        @Override
        public int compareTo(ZSetOperations.TypedTuple<Object> other) {
            return Double.compare(other.getScore(), this.score);
        }

        @Override
        public Object getValue() {
            return value;
        }

        @Override
        public Double getScore() {
            return score;
        }
    }
}
```

- [ ] **Step 2: Run test and verify it fails**

Run:

```powershell
mvn -Dtest=RedisServiceImplTest test
```

Expected: FAIL because `setZsetScore` does not exist and existing reverse range uses unordered collector.

- [ ] **Step 3: Add service contract**

Modify `RedisService.java`:

```java
<T> Boolean setZsetScore(String key, T value, Double score);
```

- [ ] **Step 4: Implement exact score write and ordered map**

Modify `RedisServiceImpl.java`:

```java
@Override
public <T> Boolean setZsetScore(String key, T value, Double score) {
    return redisTemplate.opsForZSet().add(key, value, score);
}

@Override
public Map<Object, Double> zReverseRangeWithScore(String key, long start, long end) {
    Set<ZSetOperations.TypedTuple<Object>> tuples =
            redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
    if (Objects.isNull(tuples)) {
        return Collections.emptyMap();
    }
    return tuples.stream()
            .collect(Collectors.toMap(
                    ZSetOperations.TypedTuple::getValue,
                    ZSetOperations.TypedTuple::getScore,
                    (oldValue, newValue) -> oldValue,
                    LinkedHashMap::new));
}
```

Add imports:

```java
import java.util.LinkedHashMap;
```

- [ ] **Step 5: Run Redis service tests**

Run:

```powershell
mvn -Dtest=RedisServiceImplTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add src/main/java/com/ican/service/RedisService.java src/main/java/com/ican/service/impl/RedisServiceImpl.java src/test/java/com/ican/service/impl/RedisServiceImplTest.java
git commit -m "feat: 增加热榜ZSet写入能力"
```

---

### Task 4: Implement Article Hot Score Service

**Files:**
- Modify: `src/main/java/com/ican/mapper/ArticleMapper.java`
- Modify: `src/main/resources/mapper/ArticleMapper.xml`
- Modify: `src/main/java/com/ican/mapper/CommentMapper.java`
- Modify: `src/main/resources/mapper/CommentMapper.xml`
- Create: `src/main/java/com/ican/service/ArticleHotScoreService.java`
- Create: `src/main/java/com/ican/service/impl/ArticleHotScoreServiceImpl.java`
- Create: `src/test/java/com/ican/service/impl/ArticleHotScoreServiceImplTest.java`

- [ ] **Step 1: Write failing hot score service tests**

Create `ArticleHotScoreServiceImplTest.java`:

```java
package com.ican.service.impl;

import com.ican.cache.ArticleHotScoreProperties;
import com.ican.mapper.ArticleMapper;
import com.ican.mapper.CommentMapper;
import com.ican.mapper.UserFavoriteMapper;
import com.ican.metrics.BlogMetrics;
import com.ican.model.vo.ArticleFavoriteCountVO;
import com.ican.model.vo.ArticleHotScoreSourceVO;
import com.ican.service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.ican.constant.RedisConstant.ARTICLE_HOT_SCORE;
import static com.ican.constant.RedisConstant.ARTICLE_LIKE_COUNT;
import static com.ican.constant.RedisConstant.ARTICLE_VIEW_COUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleHotScoreServiceImplTest {

    @Mock
    private ArticleMapper articleMapper;
    @Mock
    private CommentMapper commentMapper;
    @Mock
    private UserFavoriteMapper userFavoriteMapper;
    @Mock
    private RedisService redisService;
    @Mock
    private BlogMetrics blogMetrics;

    private ArticleHotScoreServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ArticleHotScoreServiceImpl(
                articleMapper,
                commentMapper,
                userFavoriteMapper,
                redisService,
                new ArticleHotScoreProperties(),
                blogMetrics
        );
    }

    @Test
    void calculateHotScore_shouldUseConfiguredWeightsAndTimeDecay() {
        ArticleHotScoreSourceVO source = source(1, "Redis 缓存", 100, 1, 1,
                LocalDateTime.now().minusDays(2));

        double score = service.calculateHotScore(source, 20, 3, 2, 5);

        assertThat(score).isEqualTo(100 * 1.0 + 20 * 5.0 + 3 * 8.0 + 2 * 10.0 + 30.0 + 50.0 - 2 * 1.5);
    }

    @Test
    void refreshHotScores_shouldAggregateSignalsAndWriteExactZsetScores() {
        ArticleHotScoreSourceVO source = source(1, "Redis 缓存", 100, 1, 0,
                LocalDateTime.now().minusDays(1));
        ArticleFavoriteCountVO favoriteCount = new ArticleFavoriteCountVO();
        favoriteCount.setArticleId(1);
        favoriteCount.setFavoriteCount(2);

        when(articleMapper.selectHotScoreSourceArticles()).thenReturn(List.of(source));
        when(redisService.getZsetAllScore(ARTICLE_VIEW_COUNT)).thenReturn(Map.of(1, 120.0));
        when(redisService.getHashAll(ARTICLE_LIKE_COUNT)).thenReturn(Map.of("1", 4));
        when(commentMapper.selectArticleCommentCount()).thenReturn(List.of());
        when(userFavoriteMapper.selectArticleFavoriteCount()).thenReturn(List.of(favoriteCount));

        var result = service.refreshHotScores();

        assertThat(result.getRefreshedCount()).isEqualTo(1);
        verify(redisService).deleteObject(ARTICLE_HOT_SCORE);
        verify(redisService).setZsetScore(ARTICLE_HOT_SCORE, 1, 120 * 1.0 + 4 * 5.0 + 0 * 8.0 + 2 * 10.0 + 0 + 50.0 - 1 * 1.5);
    }

    private ArticleHotScoreSourceVO source(Integer id, String title, Integer views, Integer isTop,
                                           Integer isRecommend, LocalDateTime createTime) {
        ArticleHotScoreSourceVO source = new ArticleHotScoreSourceVO();
        source.setArticleId(id);
        source.setArticleTitle(title);
        source.setViews(views);
        source.setIsTop(isTop);
        source.setIsRecommend(isRecommend);
        source.setCreateTime(createTime);
        return source;
    }
}
```

- [ ] **Step 2: Run test and verify it fails**

Run:

```powershell
mvn -Dtest=ArticleHotScoreServiceImplTest test
```

Expected: FAIL because service and mapper methods do not exist.

- [ ] **Step 3: Add mapper contracts**

Modify `ArticleMapper.java`:

```java
List<ArticleHotScoreSourceVO> selectHotScoreSourceArticles();
```

Add XML:

```xml
<select id="selectHotScoreSourceArticles" resultType="com.ican.model.vo.ArticleHotScoreSourceVO">
    SELECT id AS articleId,
           article_title AS articleTitle,
           views,
           is_top AS isTop,
           is_recommend AS isRecommend,
           create_time AS createTime
    FROM t_article
    WHERE is_delete = 0
      AND `status` = 1
</select>
```

Modify `CommentMapper.java`:

```java
List<CommentCountVO> selectArticleCommentCount();
```

Add XML:

```xml
<select id="selectArticleCommentCount" resultType="com.ican.model.vo.CommentCountVO">
    SELECT type_id AS id,
           COUNT(1) AS comment_count
    FROM t_comment
    WHERE comment_type = 1
      AND is_check = 1
    GROUP BY type_id
</select>
```

- [ ] **Step 4: Add service interface**

Create `ArticleHotScoreService.java`:

```java
package com.ican.service;

import com.ican.model.vo.ArticleHotScoreRefreshResultVO;
import com.ican.model.vo.ArticleHotScoreVO;

import java.util.List;

public interface ArticleHotScoreService {

    ArticleHotScoreRefreshResultVO refreshHotScores();

    List<ArticleHotScoreVO> listTopHotScores(int limit);
}
```

- [ ] **Step 5: Implement service**

Create `ArticleHotScoreServiceImpl.java`:

```java
package com.ican.service.impl;

import com.ican.cache.ArticleHotScoreProperties;
import com.ican.mapper.ArticleMapper;
import com.ican.mapper.CommentMapper;
import com.ican.mapper.UserFavoriteMapper;
import com.ican.metrics.BlogMetrics;
import com.ican.model.vo.ArticleFavoriteCountVO;
import com.ican.model.vo.ArticleHotScoreRefreshResultVO;
import com.ican.model.vo.ArticleHotScoreSourceVO;
import com.ican.model.vo.ArticleHotScoreVO;
import com.ican.model.vo.CommentCountVO;
import com.ican.service.ArticleHotScoreService;
import com.ican.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.ican.constant.CommonConstant.TRUE;
import static com.ican.constant.RedisConstant.ARTICLE_HOT_SCORE;
import static com.ican.constant.RedisConstant.ARTICLE_LIKE_COUNT;
import static com.ican.constant.RedisConstant.ARTICLE_VIEW_COUNT;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleHotScoreServiceImpl implements ArticleHotScoreService {

    private final ArticleMapper articleMapper;
    private final CommentMapper commentMapper;
    private final UserFavoriteMapper userFavoriteMapper;
    private final RedisService redisService;
    private final ArticleHotScoreProperties properties;
    private final BlogMetrics blogMetrics;

    @Override
    public ArticleHotScoreRefreshResultVO refreshHotScores() {
        long start = System.currentTimeMillis();
        List<ArticleHotScoreSourceVO> articles = articleMapper.selectHotScoreSourceArticles();
        Map<Object, Double> redisViews = redisService.getZsetAllScore(ARTICLE_VIEW_COUNT);
        Map<String, Integer> likes = redisService.getHashAll(ARTICLE_LIKE_COUNT);
        Map<Integer, Integer> comments = commentMapper.selectArticleCommentCount().stream()
                .collect(Collectors.toMap(CommentCountVO::getId, CommentCountVO::getCommentCount));
        Map<Integer, Integer> favorites = userFavoriteMapper.selectArticleFavoriteCount().stream()
                .collect(Collectors.toMap(ArticleFavoriteCountVO::getArticleId, ArticleFavoriteCountVO::getFavoriteCount));

        redisService.deleteObject(ARTICLE_HOT_SCORE);
        for (ArticleHotScoreSourceVO article : articles) {
            Integer articleId = article.getArticleId();
            int viewCount = Optional.ofNullable(redisViews.get(articleId))
                    .map(Double::intValue)
                    .orElse(Optional.ofNullable(article.getViews()).orElse(0));
            int likeCount = Optional.ofNullable(likes.get(articleId.toString())).orElse(0);
            int commentCount = Optional.ofNullable(comments.get(articleId)).orElse(0);
            int favoriteCount = Optional.ofNullable(favorites.get(articleId)).orElse(0);
            double score = calculateHotScore(article, viewCount, likeCount, commentCount, favoriteCount);
            redisService.setZsetScore(ARTICLE_HOT_SCORE, articleId, score);
        }

        long costMillis = System.currentTimeMillis() - start;
        blogMetrics.recordArticleHotScoreRefresh(articles.size(), costMillis);
        log.info("文章热度分刷新完成: count={}, cost={}ms", articles.size(), costMillis);
        return new ArticleHotScoreRefreshResultVO(articles.size(), costMillis);
    }

    @Override
    public List<ArticleHotScoreVO> listTopHotScores(int limit) {
        int safeLimit = limit <= 0 ? properties.getTopLimit() : limit;
        Map<Object, Double> topScores = redisService.zReverseRangeWithScore(ARTICLE_HOT_SCORE, 0, safeLimit - 1L);
        return topScores.entrySet().stream()
                .map(entry -> new ArticleHotScoreVO((Integer) entry.getKey(), null, entry.getValue()))
                .toList();
    }

    public double calculateHotScore(ArticleHotScoreSourceVO article,
                                    int viewCount,
                                    int likeCount,
                                    int commentCount,
                                    int favoriteCount) {
        ArticleHotScoreProperties.Weights weights = properties.getWeights();
        long ageDays = Optional.ofNullable(article.getCreateTime())
                .map(time -> ChronoUnit.DAYS.between(time.toLocalDate(), LocalDate.now()))
                .orElse(0L);
        double score = viewCount * weights.getView()
                + likeCount * weights.getLike()
                + commentCount * weights.getComment()
                + favoriteCount * weights.getFavorite()
                + (TRUE.equals(article.getIsRecommend()) ? weights.getRecommend() : 0)
                + (TRUE.equals(article.getIsTop()) ? weights.getTop() : 0)
                - Math.max(ageDays, 0) * weights.getTimeDecayPerDay();
        return Math.max(score, 0);
    }
}
```

- [ ] **Step 6: Run service tests**

Run:

```powershell
mvn -Dtest=ArticleHotScoreServiceImplTest test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add src/main/java/com/ican/mapper/ArticleMapper.java src/main/resources/mapper/ArticleMapper.xml src/main/java/com/ican/mapper/CommentMapper.java src/main/resources/mapper/CommentMapper.xml src/main/java/com/ican/service/ArticleHotScoreService.java src/main/java/com/ican/service/impl/ArticleHotScoreServiceImpl.java src/test/java/com/ican/service/impl/ArticleHotScoreServiceImplTest.java
git commit -m "feat: 增加文章热度分计算"
```

---

### Task 5: Implement Hot Article Warmup Service

**Files:**
- Create: `src/main/java/com/ican/service/HotArticleWarmupService.java`
- Create: `src/main/java/com/ican/service/impl/HotArticleWarmupServiceImpl.java`
- Modify: `src/main/java/com/ican/service/impl/ArticleServiceImpl.java`
- Modify: `src/test/java/com/ican/service/impl/ArticleServiceImplTest.java`
- Create: `src/test/java/com/ican/service/impl/HotArticleWarmupServiceImplTest.java`

- [ ] **Step 1: Write failing warmup tests**

Create `HotArticleWarmupServiceImplTest.java`:

```java
package com.ican.service.impl;

import com.ican.cache.ArticleHotScoreProperties;
import com.ican.cache.MultiLevelCacheManager;
import com.ican.mapper.ArticleMapper;
import com.ican.metrics.BlogMetrics;
import com.ican.model.vo.ArticleHotScoreVO;
import com.ican.model.vo.ArticleVO;
import com.ican.service.ArticleHotScoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.function.Function;

import static com.ican.constant.RedisConstant.ARTICLE_DETAIL_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HotArticleWarmupServiceImplTest {

    @Mock
    private ArticleHotScoreService hotScoreService;
    @Mock
    private ArticleMapper articleMapper;
    @Mock
    private MultiLevelCacheManager cacheManager;
    @Mock
    private BlogMetrics blogMetrics;

    private HotArticleWarmupServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new HotArticleWarmupServiceImpl(
                hotScoreService,
                articleMapper,
                cacheManager,
                new ArticleHotScoreProperties(),
                blogMetrics
        );
    }

    @Test
    void warmupTopHotArticles_shouldWarmArticleDetailCaches() {
        ArticleVO article = new ArticleVO();
        article.setId(1);
        when(hotScoreService.listTopHotScores(2))
                .thenReturn(List.of(new ArticleHotScoreVO(1, null, 99.0)));
        when(cacheManager.get(eq(ARTICLE_DETAIL_PREFIX + 1), any(Function.class), eq(30)))
                .thenReturn(article);

        var result = service.warmupTopHotArticles(2);

        assertThat(result.getRequestedCount()).isEqualTo(1);
        assertThat(result.getWarmedCount()).isEqualTo(1);
        verify(cacheManager).get(eq(ARTICLE_DETAIL_PREFIX + 1), any(Function.class), eq(30));
    }
}
```

- [ ] **Step 2: Run test and verify it fails**

Run:

```powershell
mvn -Dtest=HotArticleWarmupServiceImplTest test
```

Expected: FAIL because warmup service does not exist.

- [ ] **Step 3: Create warmup service interface**

Create `HotArticleWarmupService.java`:

```java
package com.ican.service;

import com.ican.model.vo.HotArticleWarmupResultVO;

import java.util.Collection;

public interface HotArticleWarmupService {

    HotArticleWarmupResultVO warmupTopHotArticles(int limit);

    HotArticleWarmupResultVO warmupArticleIds(Collection<Integer> articleIds);
}
```

- [ ] **Step 4: Implement warmup service**

Create `HotArticleWarmupServiceImpl.java`:

```java
package com.ican.service.impl;

import com.ican.cache.ArticleHotScoreProperties;
import com.ican.cache.MultiLevelCacheManager;
import com.ican.mapper.ArticleMapper;
import com.ican.metrics.BlogMetrics;
import com.ican.model.vo.ArticleHotScoreVO;
import com.ican.model.vo.ArticleVO;
import com.ican.model.vo.HotArticleWarmupResultVO;
import com.ican.service.ArticleHotScoreService;
import com.ican.service.HotArticleWarmupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import static com.ican.constant.RedisConstant.ARTICLE_DETAIL_PREFIX;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotArticleWarmupServiceImpl implements HotArticleWarmupService {

    private final ArticleHotScoreService hotScoreService;
    private final ArticleMapper articleMapper;
    private final MultiLevelCacheManager cacheManager;
    private final ArticleHotScoreProperties properties;
    private final BlogMetrics blogMetrics;

    @Override
    public HotArticleWarmupResultVO warmupTopHotArticles(int limit) {
        int safeLimit = limit <= 0 ? properties.getWarmupLimit() : limit;
        List<Integer> articleIds = hotScoreService.listTopHotScores(safeLimit).stream()
                .map(ArticleHotScoreVO::getArticleId)
                .toList();
        return warmupArticleIds(articleIds);
    }

    @Override
    public HotArticleWarmupResultVO warmupArticleIds(Collection<Integer> articleIds) {
        long start = System.currentTimeMillis();
        Collection<Integer> distinctIds = new LinkedHashSet<>(articleIds);
        int warmed = 0;
        int skipped = 0;
        int failed = 0;
        for (Integer articleId : distinctIds) {
            if (Objects.isNull(articleId)) {
                skipped++;
                continue;
            }
            try {
                ArticleVO article = cacheManager.get(ARTICLE_DETAIL_PREFIX + articleId,
                        key -> articleMapper.selectArticleHomeById(articleId),
                        properties.getDetailCacheTtlMinutes());
                if (Objects.isNull(article)) {
                    skipped++;
                } else {
                    warmed++;
                }
            } catch (Exception e) {
                failed++;
                log.warn("热点文章缓存预热失败: articleId={}, message={}", articleId, e.getMessage());
            }
        }
        long costMillis = System.currentTimeMillis() - start;
        blogMetrics.recordHotArticleWarmup(warmed, failed, costMillis);
        log.info("热点文章缓存预热完成: requested={}, warmed={}, skipped={}, failed={}, cost={}ms",
                distinctIds.size(), warmed, skipped, failed, costMillis);
        return new HotArticleWarmupResultVO(distinctIds.size(), warmed, skipped, failed, costMillis);
    }
}
```

- [ ] **Step 5: Delegate old homepage candidate warmup**

Modify `ArticleServiceImpl` constructor dependencies:

```java
private final HotArticleWarmupService hotArticleWarmupService;
```

Replace `batchProcessHotArticles(hotCandidates)` call with:

```java
hotArticleWarmupService.warmupArticleIds(hotCandidates);
```

Delete the private `batchProcessHotArticles` method if it becomes unused.

- [ ] **Step 6: Update `ArticleServiceImplTest` constructor**

Add mock:

```java
@Mock
private HotArticleWarmupService hotArticleWarmupService;
```

Pass it into constructor after `cacheManager` and before `rabbitTemplate`, matching actual constructor order.

- [ ] **Step 7: Run warmup and article tests**

Run:

```powershell
mvn -Dtest=HotArticleWarmupServiceImplTest,ArticleServiceImplTest test
```

Expected: PASS.

- [ ] **Step 8: Commit**

```powershell
git add src/main/java/com/ican/service/HotArticleWarmupService.java src/main/java/com/ican/service/impl/HotArticleWarmupServiceImpl.java src/main/java/com/ican/service/impl/ArticleServiceImpl.java src/test/java/com/ican/service/impl/ArticleServiceImplTest.java src/test/java/com/ican/service/impl/HotArticleWarmupServiceImplTest.java
git commit -m "feat: 增加热点文章缓存预热"
```

---

### Task 6: Add Admin Endpoints, Timed Tasks And Metrics

**Files:**
- Modify: `src/main/java/com/ican/controller/CacheController.java`
- Modify: `src/main/java/com/ican/quartz/task/TimedTask.java`
- Modify: `src/main/java/com/ican/metrics/BlogMetrics.java`
- Modify: `src/test/java/com/ican/metrics/BlogMetricsTest.java`
- Create: `src/test/java/com/ican/controller/CacheControllerTest.java`

- [ ] **Step 1: Write failing metrics test**

Append to `BlogMetricsTest.java`:

```java
@Test
void hotScoreAndWarmupMetrics_shouldBeRecorded() {
    MeterRegistry registry = new SimpleMeterRegistry();
    BlogMetrics metrics = new BlogMetrics(registry);

    metrics.recordArticleHotScoreRefresh(3, 25);
    metrics.recordHotArticleWarmup(2, 1, 30);

    assertThat(registry.find("blog.article.hot_score.refresh").counter().count()).isEqualTo(3.0);
    assertThat(registry.find("blog.article.hot_cache.warmup").counter().count()).isEqualTo(2.0);
    assertThat(registry.find("blog.article.hot_cache.warmup.failures").counter().count()).isEqualTo(1.0);
    assertThat(registry.find("blog.article.hot_score.refresh.duration").timer().count()).isEqualTo(1);
    assertThat(registry.find("blog.article.hot_cache.warmup.duration").timer().count()).isEqualTo(1);
}
```

- [ ] **Step 2: Run metrics test and verify it fails**

Run:

```powershell
mvn -Dtest=BlogMetricsTest test
```

Expected: FAIL because new metric methods do not exist.

- [ ] **Step 3: Implement metrics methods**

Modify `BlogMetrics.java`:

```java
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
```

Add methods:

```java
public void recordArticleHotScoreRefresh(int articleCount, long costMillis) {
    Counter.builder("blog.article.hot_score.refresh")
            .description("文章热度分刷新文章数")
            .register(registry)
            .increment(articleCount);
    Timer.builder("blog.article.hot_score.refresh.duration")
            .description("文章热度分刷新耗时")
            .register(registry)
            .record(costMillis, TimeUnit.MILLISECONDS);
}

public void recordHotArticleWarmup(int warmedCount, int failedCount, long costMillis) {
    Counter.builder("blog.article.hot_cache.warmup")
            .description("热点文章缓存预热成功数")
            .register(registry)
            .increment(warmedCount);
    Counter.builder("blog.article.hot_cache.warmup.failures")
            .description("热点文章缓存预热失败数")
            .register(registry)
            .increment(failedCount);
    Timer.builder("blog.article.hot_cache.warmup.duration")
            .description("热点文章缓存预热耗时")
            .register(registry)
            .record(costMillis, TimeUnit.MILLISECONDS);
}
```

- [ ] **Step 4: Write failing controller test**

Create `CacheControllerTest.java`:

```java
package com.ican.controller;

import com.ican.cache.MultiLevelCacheManager;
import com.ican.model.vo.ArticleHotScoreRefreshResultVO;
import com.ican.model.vo.ArticleHotScoreVO;
import com.ican.model.vo.HotArticleWarmupResultVO;
import com.ican.service.ArticleHotScoreService;
import com.ican.service.HotArticleWarmupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(CacheController.class)
@AutoConfigureMockMvc(addFilters = false)
class CacheControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MultiLevelCacheManager cacheManager;
    @MockBean
    private ArticleHotScoreService hotScoreService;
    @MockBean
    private HotArticleWarmupService warmupService;

    @Test
    void refreshHotScores_shouldReturnResult() throws Exception {
        when(hotScoreService.refreshHotScores()).thenReturn(new ArticleHotScoreRefreshResultVO(3, 25L));

        mockMvc.perform(post("/admin/cache/hot-scores/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshedCount").value(3));
    }

    @Test
    void listHotScores_shouldReturnTopScores() throws Exception {
        when(hotScoreService.listTopHotScores(2))
                .thenReturn(List.of(new ArticleHotScoreVO(1, "Redis 缓存", 99.0)));

        mockMvc.perform(get("/admin/cache/hot-scores").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].articleId").value(1));
    }

    @Test
    void warmupHotArticles_shouldReturnResult() throws Exception {
        when(warmupService.warmupTopHotArticles(2))
                .thenReturn(new HotArticleWarmupResultVO(2, 2, 0, 0, 30L));

        mockMvc.perform(post("/admin/cache/hot-articles/warmup").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warmedCount").value(2));
    }
}
```

- [ ] **Step 5: Add controller endpoints**

Modify `CacheController.java`:

```java
private final ArticleHotScoreService hotScoreService;
private final HotArticleWarmupService warmupService;

@Operation(summary = "刷新文章热度分")
@PostMapping("/hot-scores/refresh")
public ArticleHotScoreRefreshResultVO refreshHotScores() {
    return hotScoreService.refreshHotScores();
}

@Operation(summary = "查询文章热榜")
@GetMapping("/hot-scores")
public List<ArticleHotScoreVO> listHotScores(@RequestParam(defaultValue = "20") Integer limit) {
    return hotScoreService.listTopHotScores(limit);
}

@Operation(summary = "预热热点文章缓存")
@PostMapping("/hot-articles/warmup")
public HotArticleWarmupResultVO warmupHotArticles(@RequestParam(defaultValue = "10") Integer limit) {
    return warmupService.warmupTopHotArticles(limit);
}
```

Add imports for `List`, hot score service, warmup service and VO classes.

- [ ] **Step 6: Add timed task entrypoints**

Modify `TimedTask.java`:

```java
@Autowired
private ArticleHotScoreService articleHotScoreService;

@Autowired
private HotArticleWarmupService hotArticleWarmupService;

/**
 * 刷新文章热度分
 */
public void refreshArticleHotScore() {
    articleHotScoreService.refreshHotScores();
}

/**
 * 预热热点文章缓存
 */
public void warmupHotArticles() {
    hotArticleWarmupService.warmupTopHotArticles(0);
}
```

- [ ] **Step 7: Run metrics and controller tests**

Run:

```powershell
mvn -Dtest=BlogMetricsTest,CacheControllerTest test
```

Expected: PASS.

- [ ] **Step 8: Commit**

```powershell
git add src/main/java/com/ican/controller/CacheController.java src/main/java/com/ican/quartz/task/TimedTask.java src/main/java/com/ican/metrics/BlogMetrics.java src/test/java/com/ican/metrics/BlogMetricsTest.java src/test/java/com/ican/controller/CacheControllerTest.java
git commit -m "feat: 增加热榜缓存管理接口"
```

---

### Task 7: Stage Summary Documentation And Full Verification

**Files:**
- Create: `docs/03-缓存体系/02-Phase2-文章热度分与热点预热总结.md`

- [ ] **Step 1: Write stage summary document**

Create `docs/03-缓存体系/02-Phase2-文章热度分与热点预热总结.md` with these sections:

```markdown
# Phase2 文章热度分与热点缓存预热总结

## 阶段目标

## 升级内容

## 修改文件清单

## 缓存体系变化

## 热度分模型

## 热点缓存预热流程

## 测试结果

## Git 记录

## 后续建议
```

Fill each section after implementation with actual files, commits and test output.

- [ ] **Step 2: Run focused tests**

Run:

```powershell
mvn -Dtest=UserFavoriteMapperContractTest,ArticleHotScorePropertiesTest,RedisServiceImplTest,ArticleHotScoreServiceImplTest,HotArticleWarmupServiceImplTest,BlogMetricsTest,CacheControllerTest,ArticleServiceImplTest test
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 3: Run full test suite**

Run:

```powershell
$env:JAVA_HOME='C:\Users\IT074\.jdks\ms-21.0.10'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; & 'D:\IntelliJ IDEA 2025.2.4\plugins\maven\lib\maven3\bin\mvn.cmd' test
```

Expected:

```text
BUILD SUCCESS
Failures: 0
Errors: 0
```

- [ ] **Step 4: Run whitespace check**

Run:

```powershell
git diff --check
```

Expected: no whitespace errors.

- [ ] **Step 5: Commit documentation**

```powershell
git add docs/03-缓存体系/02-Phase2-文章热度分与热点预热总结.md
git commit -m "docs: 补充第二阶段缓存总结"
```

- [ ] **Step 6: Merge after tests pass**

From `D:\IdeaProjects\blog`, check user worktree first:

```powershell
git status --short --branch
```

If only unrelated user files remain, merge:

```powershell
git merge --no-ff codex/phase2-cache-audit -m "merge: 第二阶段缓存热度分开发"
```

Run full tests again in `D:\IdeaProjects\blog\blog-springboot` before final response.

---

## Self-Review

1. **Spec coverage**
   - 缓存文档分类已由 previous design commit 覆盖。
   - 文章热度分模型由 Task 2 and Task 4 覆盖。
   - Redis ZSet 热榜由 Task 3 and Task 4 覆盖。
   - 热点缓存预热由 Task 5 覆盖。
   - 手动管理和定时入口由 Task 6 覆盖。
   - 指标和测试由 Task 4, Task 5, Task 6 覆盖。
   - 阶段总结文档由 Task 7 覆盖。

2. **Placeholder scan**
   - No `TODO`, `TBD`, or `implement later`.
   - Every code-changing task includes concrete code snippets and commands.

3. **Type consistency**
   - Hot score classes consistently use `ArticleHotScore*`.
   - Warmup classes consistently use `HotArticleWarmup*`.
   - Redis hot score key consistently uses `ARTICLE_HOT_SCORE`.
   - Favorite table contract consistently uses `favorite_id`, not `doc_id`.
