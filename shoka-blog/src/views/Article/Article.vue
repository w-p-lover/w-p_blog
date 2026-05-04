<template>
  <div class="bg article-page">
    <div class="main-container article-layout" v-if="article">
      <div class="left-container" :class="app.sideFlag ? 'test' : ''">
        <div class="article-container">
          <header class="article-head">
            <h1 class="article-title">{{ article.articleTitle }}</h1>
            <div class="article-byline">
              <div class="author-block">
                <img
                    class="article-avatar"
                    v-if="blog.blogInfo.siteConfig.authorAvatar"
                    :src="blog.blogInfo.siteConfig.authorAvatar"
                    alt=""
                >
                <span class="article-avatar article-avatar--text" v-else>{{ blog.blogInfo.siteConfig.siteAuthor?.slice(0, 1) || "博" }}</span>
                <div class="author-copy">
                  <span class="author-name">{{ blog.blogInfo.siteConfig.siteAuthor }}</span>
                  <span class="publish-date">{{ formatDate(article.createTime) }}</span>
                </div>
              </div>
              <div class="article-stats">
                <span class="meta-item" v-if="article.updateTime"><svg-icon icon-class="update" size="0.85rem"></svg-icon>更新 {{ formatDate(article.updateTime) }}</span>
                <span class="meta-item"><svg-icon icon-class="edit" size="0.85rem"></svg-icon>本文字数 {{ count(wordNum) }}</span>
                <span class="meta-item"><svg-icon icon-class="clock" size="0.85rem"></svg-icon>阅读时间约为 {{ readTime }} 分钟</span>
                <span class="meta-item"><svg-icon icon-class="eye" size="0.85rem"></svg-icon>浏览量 {{ article.views }}</span>
              </div>
            </div>
            <div class="article-actions">
              <button class="action-pill" :class="isLike(article.id)" @click="like">
                <svg-icon icon-class="like" size="0.9rem"></svg-icon>
                {{ article.likeCount }}
              </button>
              <span class="action-pill">
                <svg-icon icon-class="category" size="0.9rem"></svg-icon>
                {{ article.category.categoryName }}
              </span>
            </div>
          </header>
          <img class="article-cover-image" v-if="article.articleCover" :src="article.articleCover" alt="">
          <v-md-preview ref="articleRef" class="md" v-viewer :text="article.articleContent" :anchor-heading="{ enable: true }"></v-md-preview>
          <div class="article-post">
            <div class="tag-share">
              <router-link :to="`/tag/${tag.id}`" class="article-tag" v-for="tag in article.tagVOList" :key="tag.id">
                <svg-icon icon-class="tag" size="0.8rem"></svg-icon>
                {{ tag.tagName }}
              </router-link>
              <Share class="share-info" :url="articleHref" :title="article.articleTitle"></Share>
            </div>
            <div class="reward">
              <button class="btn" :class="isLike(article.id)" @click="like">
                <svg-icon icon-class="like" size="0.9rem"></svg-icon>
                点赞
                <span>{{ article.likeCount }}</span>
              </button>
              <n-popover trigger="click" v-if="blog.blogInfo.siteConfig.isReward">
                <template #trigger>
                  <button class="btn reward-btn">
                    <svg-icon icon-class="qr_code" size="0.9rem"></svg-icon>
                    打赏
                  </button>
                </template>
                <div class="reward-all">
                  <span>
                    <img class="reward-img" v-lazy="blog.blogInfo.siteConfig.weiXinCode"/>
                    <div class="reward-desc">微信</div>
                  </span>
                  <span style="margin-left: 0.3rem;">
                    <img class="reward-img" v-lazy="blog.blogInfo.siteConfig.aliCode"/>
                    <div class="reward-desc">支付宝</div>
                  </span>
                </div>
              </n-popover>
              <p class="tea" v-if="blog.blogInfo.siteConfig.isReward">请我喝[茶]~(￣▽￣)~*</p>
            </div>
            <div class="copyright">
              <ul>
                <li class="author">
                  <svg-icon icon-class="author" size="0.9rem" style="margin-right:0.3rem"></svg-icon>
                  <strong>本文作者： </strong>{{ blog.blogInfo.siteConfig.siteAuthor }}
                </li>
                <li class="link">
                  <svg-icon icon-class="article_link" size="0.9rem" style="margin-right:0.3rem"></svg-icon>
                  <strong>本文链接：</strong>
                  <a :href="articleHref">{{ articleHref }}</a>
                </li>
                <li class="license">
                  <svg-icon icon-class="article_share" size="0.8rem" style="margin-right:0.3rem"></svg-icon>
                  <strong>版权声明： </strong>本站所有文章除特别声明外，均采用
                  <a href="https://creativecommons.org/licenses/by-nc-sa/4.0/deed.zh" target="_blank">CC
                    BY-NC-SA 4.0</a>
                  许可协议。转载请注明文章出处！
                </li>
              </ul>
            </div>
            <!-- 上下文 -->
            <div class="post-nav">
              <div class="item" v-if="article.lastArticle">
                <router-link :to="`/article/${article.lastArticle?.id}`" class="post-cover"
                             :style="articleCover(article.lastArticle.articleCover)">
                  <span class="post-last-next">上一篇</span>
                  <h3 class="post-title">{{ article.lastArticle.articleTitle }}</h3>
                </router-link>
              </div>
              <div class="item" v-if="article.nextArticle">
                <router-link :to="`/article/${article.nextArticle?.id}`" class="post-cover"
                             :style="articleCover(article.nextArticle.articleCover)">
                  <span class="post-last-next">下一篇</span>
                  <h3 class="post-title">{{ article.nextArticle.articleTitle }}</h3>
                </router-link>
              </div>
            </div>
            <CommentList :comment-type="commentType"></CommentList>
          </div>
        </div>
      </div>
      <div class="right-container" :class="app.sideFlag ? 'temp' : ''">
        <div class="side-card">
          <Catalog v-if="articleLoaded && articleRef" :domRef="articleRef"></Catalog>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {getArticle, likeArticle} from "@/api/article";
import {ArticleInfo, ArticlePagination} from "@/api/article/types";
import {CategoryVO} from "@/api/category/types";
import useStore from "@/store";
import {formatDate} from "@/utils/date";
import {Share} from 'vue3-social-share';
import 'vue3-social-share/lib/index.css';
const {app, blog, user} = useStore();
const articleRef = ref();
const route = useRoute();
const articleHref = window.location.href;
const data = reactive({
  articleLoaded: false,
  wordNum: 0,
  readTime: 0,
  commentType: 1,
  article: {
    id: 0,
    articleCover: "",
    articleTitle: "",
    articleContent: "",
    articleType: 0,
    views: 0,
    likeCount: 0,
    category: {} as CategoryVO,
    tagVOList: [],
    createTime: "",
    lastArticle: {} as ArticlePagination,
    nextArticle: {} as ArticlePagination,
    updateTime: ""
  } as ArticleInfo,
});
const {articleLoaded, wordNum, readTime, commentType, article} = toRefs(data);
const articleCover = computed(() => (cover: string) => 'background-image:url(' + cover + ')');
const isLike = computed(() => (id: number) => user.articleLikeSet.indexOf(id) != -1 ? "like-btn-active" : "like-btn");
const count = (value: number) => {
  if (value >= 1000) {
    return (value / 1000).toFixed(1) + "k";
  }
  return value;
};
const deleteHTMLTag = (content: string) => {
  return content
      .replace(/<\/?[^>]*>/g, "")
      .replace(/[|]*\n/, "")
      .replace(/&npsp;/gi, "");
};
const like = () => {
  if (!user.id) {
    app.setLoginFlag(true);
    return;
  }
  let id = article.value.id;
  likeArticle(id).then(({data}) => {
    if (data.flag) {
      //判断是否点赞
      if (user.articleLikeSet.indexOf(id) != -1) {
        article.value.likeCount -= 1;
      } else {
        article.value.likeCount += 1;
      }
      user.articleLike(id);
    }
  });
};
onMounted(() => {
  getArticle(Number(route.params.id)).then(({data}) => {
    article.value = data.data;
    document.title = article.value.articleTitle;
    wordNum.value = deleteHTMLTag(article.value.articleContent).length;
    readTime.value = Math.round(wordNum.value / 400);
    articleLoaded.value = true;
  })
})
</script>

<style lang="scss" scoped>
@import "@/assets/styles/mixin.scss";

.article-container {
  border: 1px solid var(--home-border);
  border-radius: 0.65rem;
  overflow: hidden;
  background: var(--home-surface-strong);
  box-shadow: var(--home-shadow), var(--home-glow);
}

.article-post {
  margin: 0 2.25rem 2rem;
}

.article-page {
  min-height: 100vh;
  padding-top: 5.2rem;
}

.article-layout {
  align-items: flex-start;
}

.article-head {
  padding: 2.35rem 2.45rem 1rem;
  border-bottom: 1px solid var(--home-border);
  background:
      radial-gradient(circle at 8% 0, rgba(85, 162, 160, 0.08), transparent 18rem),
      linear-gradient(180deg, #fffefa 0, #fdfbf7 100%);
}

.article-title {
  max-width: 48rem;
  margin: 0 0 1.2rem;
  font-weight: 800;
  font-size: clamp(1.65rem, 2.8vw, 2.2rem);
  letter-spacing: 0;
  text-align: left;
  color: #333;
  line-height: 1.32;
}

.article-byline {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1.2rem;
  color: var(--home-muted);
  font-size: 0.86rem;
  line-height: 1.5;
}

.author-block {
  display: inline-flex;
  align-items: center;
  min-width: 12rem;
  gap: 0.75rem;
}

.article-avatar {
  @include flex;
  width: 2.55rem;
  height: 2.55rem;
  border: 1px solid rgba(85, 162, 160, 0.35);
  border-radius: 50%;
  background: var(--home-accent-soft);
  color: var(--home-accent);
  font-weight: 800;
  object-fit: cover;
}

.article-avatar--text {
  flex-shrink: 0;
}

.author-copy {
  display: flex;
  flex-direction: column;
  gap: 0.05rem;
}

.author-name {
  margin: 0;
  color: var(--grey-7);
  font-weight: 700;
}

.publish-date {
  color: var(--home-muted);
  font-size: 0.82rem;
}

.article-stats {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 0.35rem 0.8rem;
  max-width: 36rem;

  .meta-item {
    display: inline-flex;
    align-items: center;
    gap: 0.25rem;
    color: var(--home-muted);
    white-space: nowrap;
  }
}

.article-actions {
  display: flex;
  justify-content: space-between;
  gap: 0.6rem;
  margin-top: 1rem;
  padding-top: 0.8rem;
  border-top: 1px solid var(--home-border);
}

.action-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  min-height: 1.9rem;
  padding: 0 0.85rem;
  border: 1px solid var(--home-border-strong);
  border-radius: 999px;
  color: var(--grey-6);
  background: rgba(255, 255, 255, 0.42);
  font: inherit;
  font-size: 0.86rem;
}

.article-cover-image {
  display: block;
  max-width: min(34rem, calc(100% - 3rem));
  max-height: 32rem;
  margin: 2.2rem auto 1.2rem;
  border-radius: 0.3rem;
  object-fit: contain;
  box-shadow: 0 8px 22px rgba(89, 78, 56, 0.12);
}

.tag-share {
  display: flex;
  align-items: center;

  .share-info {
    margin-left: auto;
  }
}

.reward {
  margin: 1.25rem auto;
  padding: 0.625rem 0;
  text-align: center;

  .btn {
    border-radius: 999px;
    color: var(--grey-0);
    cursor: pointer !important;
    padding: 0.12rem 0.9375rem;
    font: inherit;
  }

  .like-btn-active {
    background: var(--home-accent-warm);
  }

  .like-btn {
    background: var(--home-accent);
  }

  .reward-btn {
    position: relative;
    margin-left: 1rem;
    background: var(--primary-color);
  }

  .tea {
    font-size: 0.8125em;
    color: var(--grey-5);
    margin-top: 0.5rem;
  }
}

.reward-all {
  display: flex;
  align-items: center;
}

.reward-img {
  width: 130px;
  height: 130px;
  display: block;
}

.reward-desc {
  margin: -5px 0;
  color: #858585;
  text-align: center;
}

.copyright {
  font-size: 0.75em;
  padding: 1rem 2rem;
  margin-bottom: 2.5rem;
  border: 1px solid var(--home-border);
  border-radius: 0.55rem;
  background: var(--grey-2);
  color: var(--grey-6);
}

.post-nav {
  display: flex;
  margin-bottom: 2.5rem;
  border-radius: 0.55rem;
  overflow: hidden;
  border: 1px solid var(--home-border);

  .item {
    width: 50%;
  }

  .post-cover {
    display: flex;
    flex-direction: column;
    color: var(--header-text-color);
    padding: 1.25rem 2.5rem;
    background-size: cover;
    animation: blur 0.8s ease-in-out forwards;

    &:before {
      content: "";
      position: absolute;
      width: 100%;
      height: 100%;
      background: linear-gradient(135deg, #434343, #000);
      opacity: 0.5;
      transition: all 0.2s ease-in-out 0s;
      z-index: -1;
      top: 0;
      left: 0;
    }
  }

  .post-last-next {
    font-size: 0.8125rem;
  }
}

.post-cover:hover::before {
  opacity: 0.4;
}

@media (max-width: 767px) {
  .article-page {
    padding-top: 4.3rem;
  }

  .article-head {
    padding: 1.35rem 1rem 0.85rem;
  }

  .article-title {
    font-size: 1.45rem;
  }

  .article-byline {
    align-items: flex-start;
    flex-direction: column;
    gap: 0.8rem;
  }

  .article-stats {
    justify-content: flex-start;
    gap: 0.35rem 0.65rem;
  }

  .article-post {
    margin: 0 0.85rem 1.5rem;
  }

  .article-cover-image {
    max-width: calc(100% - 1.5rem);
    margin-top: 1.2rem;
  }

  .post-nav {
    flex-direction: column;
  }

  .post-nav .item {
    width: 100%;
  }

  .reward-img {
    width: 105px;
    height: 105px;
  }

}
</style>
