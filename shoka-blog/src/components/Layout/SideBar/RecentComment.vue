<template>
  <div v-if="commentList.length" class="side-card">
    <!-- 标题 -->
    <div class="comment-title">
      <svg-icon icon-class="comment" size="1.1875rem"></svg-icon>
      最新评论
    </div>
    <!-- 评论 -->
    <div class="comment-item" v-for="comment in commentList" :key="comment.id">
      <!-- 头像 -->
      <img class="user-avatar" :src="comment.avatar" alt=""/>
      <div class="comment-content">
        <div class="info">
          <!-- 昵称 -->
          <span class="comment-name">{{ comment.nickname }}</span>
          <!-- 时间 -->
          <time :datetime="comment.createTime">{{ formatDate(comment.createTime) }}</time>
        </div>
        <!-- 内容 -->
        <span class="content" v-html="comment.commentContent"></span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {getRecentComment} from "@/api/comment";
import {RecentComment} from "@/api/comment/types";
import {formatDate} from "@/utils/date";

const commentList = ref<RecentComment[]>([]);
onMounted(() => {
  getRecentComment().then(({data}) => {
    commentList.value = data.flag && Array.isArray(data.data) ? data.data : [];
  }).catch(() => { commentList.value = []; });
});
</script>

<style lang="scss" scoped>
.comment-title {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  margin-bottom: 0.35rem;
  font-size: 0.9375rem;
  font-weight: 600;
}

.comment-name {
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 0;
  white-space: nowrap;
  color: var(--text-color);
}

.comment-item {
  display: flex;
  align-items: flex-start;
  gap: 0.6rem;
  padding: 0.65rem 0;

  & + & { border-top: 1px solid var(--home-border); }
  &:last-child { padding-bottom: 0; }
}

.user-avatar {
  flex-shrink: 0;
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  object-fit: cover;
}

.comment-content {
  flex: 1;
  min-width: 0;

  .info {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: 0.4rem;
    font-size: 0.75rem;
    line-height: 1.5;
    color: var(--grey-6);

    time { flex-shrink: 0; font-size: 0.6875rem; }
  }

  .content {
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    margin-top: 0.2rem;
    font-size: 0.8125rem;
    line-height: 1.6;
    overflow-wrap: anywhere;
    overflow: hidden;
  }
}
</style>
