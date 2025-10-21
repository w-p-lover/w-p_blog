<template>
  <div class="page-header">
    <h1 class="page-title">说说</h1>
    <img class="page-cover" src="../../assets/images/bg.jpg" alt="">
    <Waves></Waves>
  </div>

  <div class="bg">
    <div class="page-container">
      <div class="timeline-container">
        <div class="timeline-line"></div>

        <router-link
            v-for="(talk, index) in talkList"
            :key="talk.id"
            :to="`/talk/${talk.id}`"
            class="talk-item"
            v-animate="['slideUpBigIn']"
            :class="{ 'left-item': index % 2 === 0, 'right-item': index % 2 !== 0 }">

          <!-- 时间点标记 -->
          <div class="timeline-dot">
            <div class="dot-inner"></div>
          </div>

          <!-- 时间标签装饰（对侧） -->
          <div class="timeline-deco" :class="{ 'deco-left': index % 2 !== 0, 'deco-right': index % 2 === 0 }">
            {{ formatDateTime(talk.createTime) }}
          </div>

          <!-- 主体内容 -->
          <div class="timeline-content">
            <div class="talk-meta">
              <img class="user-avatar" :src="talk.avatar">
              <div class="talk-info">
                <span class="talk-user-name">
                  {{ talk.nickname }}
                  <svg-icon icon-class="badge" style="margin-left: 0.4rem;"></svg-icon>
                </span>
                <span class="talk-time">{{ formatDateTime(talk.createTime) }}</span>
              </div>
            </div>

            <div class="talk-content" v-html="talk.talkContent"></div>

            <div class="talk-image" v-viewer>
              <img
                  v-for="(img, idx) in talk.imgList"
                  :key="idx"
                  v-lazy="img"
                  class="image"
                  @click.prevent
              />
            </div>

            <div class="info-bar">
              <span class="talk-like info-item">
                <svg-icon icon-class="like" size="0.8rem" style="margin-right: 5px;"></svg-icon>
                {{ talk.likeCount }}
              </span>
              <span class="talk-comment info-item">
                <svg-icon icon-class="comment" size="0.9rem" style="margin-right: 5px;"></svg-icon>
                {{ talk.commentCount }}
              </span>
            </div>
          </div>
        </router-link>
      </div>

      <div class="loading-warp" v-if="talkList && count > talkList.length">
        <n-button class="btn" color="#e9546b" @click="getList">加载更多...</n-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {getTalkList} from "@/api/talk";
import {Talk} from "@/api/talk/types";
import {PageQuery} from "@/model";
import {formatDateTime} from "@/utils/date";
import {reactive, toRefs, onMounted} from "vue";
import Waves from "@/components/Waves/index.vue";

const data = reactive({
  count: 0,
  queryParams: {current: 1, size: 5} as PageQuery,
  talkList: [] as Talk[],
});
const {count, queryParams, talkList} = toRefs(data);

const getList = () => {
  getTalkList(queryParams.value).then(({data}) => {
    if (queryParams.value.current == 1) {
      talkList.value = data.data.recordList;
    } else {
      talkList.value.push(...data.data.recordList);
    }
    queryParams.value.current++;
    count.value = data.data.count;
  });
};
onMounted(() => getList());
</script>

<style lang="scss" scoped>
.timeline-container {
  position: relative;
  max-width: 1200px;
  margin: 2rem auto;
  padding: 0 2rem;

  /* 保留粉色背景装饰 */
  &::before {
    content: "";
    position: absolute;
    left: 0;
    top: -25px;
    width: 100%;
    height: 106%;
    background: radial-gradient(
            circle at 50% 10%,
            rgba(233, 84, 107, 0.05),
            transparent 60%
    ),
    radial-gradient(
            circle at 20% 80%,
            rgba(233, 84, 107, 0.05),
            transparent 60%
    );
    z-index: 0;
  }
}

.timeline-line {
  position: absolute;
  left: 50%;
  top: 0;
  bottom: 0;
  width: 2px;
  background: linear-gradient(to bottom, rgba(233, 84, 107, 0.2) 0%, rgba(233, 84, 107, 0.6) 100%);
  transform: translateX(-50%);
  z-index: 1;
}

.talk-item {
  display: block;
  position: relative;
  margin-bottom: 4rem;
  width: 45%;
  visibility: hidden;
  z-index: 2;

  &.left-item {
    left: 0;
  }

  &.right-item {
    left: 55%;
  }

  /* 保留时间轴虚线连接 */
  &::after {
    content: "";
    position: absolute;
    top: 2.5rem;
    width: 50px;
    height: 2px;
    background: repeating-linear-gradient(
            to right,
            rgba(233, 84, 107, 0.6),
            rgba(233, 84, 107, 0.6) 6px,
            transparent 6px,
            transparent 12px
    );
    opacity: 0.4;
  }

  &.left-item::after {
    right: -50px;
  }

  &.right-item::after {
    left: -50px;
  }

  &:hover {
    .timeline-dot .dot-inner {
      transform: scale(1.2);
      box-shadow: 0 0 0 6px rgba(233, 84, 107, 0.2);
    }
    .timeline-content {
      transform: translateY(-5px);
      box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
    }
  }
}

.timeline-dot {
  position: absolute;
  top: 1.5rem;
  width: 40px;
  height: 40px;
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 3;

  .dot-inner {
    width: 16px;
    height: 16px;
    border-radius: 50%;
    background-color: #e9546b;
    box-shadow: 0 0 0 4px rgba(233, 84, 107, 0.15);
    transition: all 0.3s ease;
  }
}

.left-item .timeline-dot {
  right: -62px;
}

.right-item .timeline-dot {
  left: -62px;
}

/* 时间标签装饰 */
.timeline-deco {
  position: absolute;
  top: 1.3rem;
  font-size: 0.85rem;
  color: rgba(233, 84, 107, 0.8);
  background: rgba(233, 84, 107, 0.08);
  border: 1px solid rgba(233, 84, 107, 0.2);
  border-radius: 20px;
  padding: 0.4rem 0.8rem;
  box-shadow: 0 0 10px rgba(233, 84, 107, 0.15);
  backdrop-filter: blur(4px);
  animation: floatTag 4s ease-in-out infinite;
  opacity: 0.85;
  pointer-events: none;
  z-index: 2;
}

.deco-left {
  top: 25px;
  left: -230px;
}

.deco-right {
  top: 25px;
  right: -230px;
}

@keyframes floatTag {
  0%, 100% {
    transform: translateY(0);
    opacity: 0.8;
  }
  50% {
    transform: translateY(-6px);
    opacity: 1;
  }
}

.timeline-content {
  background: #fff;
  border-radius: 0.75rem;
  padding: 1.5rem;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  width: 100%;
  position: relative;

  /* 卡片顶部波浪装饰 */
  &::before {
    content: "";
    position: absolute;
    top: 0;
    left: 1.5rem;
    width: 130px;
    height: 6px;
    background: repeating-linear-gradient(
            135deg,
            #e9546b 0,
            #e9546b 2px,
            transparent 2px,
            transparent 4px
    );
    border-radius: 3px;
    opacity: 0.7;
  }

}

.talk-meta {
  display: flex;
  align-items: center;
  border-bottom: 1px solid #f5f5f5;
  margin-bottom: 1rem;
  padding-bottom: 0.75rem;
}

.talk-info {
  margin-left: 0.75rem;
  display: flex;
  flex-direction: column;
}

.user-avatar {
  width: 3rem;
  height: 3rem;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #f5f5f5;
}

.talk-user-name {
  font-weight: 600;
  color: #333;
  font-size: 1rem;
  display: flex;
  align-items: center;
}

.talk-time {
  color: #999;
  font-size: 0.85rem;
  margin-top: 0.25rem;
}

.talk-content {
  color: #555;
  line-height: 1.8;
  font-size: 0.95rem;
  margin-bottom: 1rem;
  white-space: pre-line;
}

.talk-image {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-bottom: 1rem;

  .image {
    width: calc(33.333% - 0.5rem);
    height: 8rem;
    object-fit: cover;
    border-radius: 0.5rem;
    transition: all 0.3s ease;
  }

  .image:hover {
    transform: scale(1.03);
  }

  &:has(.image:only-child) .image {
    width: 100%;
    height: 18rem;
  }
}

.info-bar {
  display: flex;
  align-items: center;
  color: #888;
  font-size: 0.85rem;
  padding-top: 0.75rem;
  border-top: 1px solid #f5f5f5;
}

.info-item {
  display: flex;
  align-items: center;

  &:not(:last-child) {
    margin-right: 1.5rem;
  }

  &:hover {
    color: #e9546b;
  }
}

.loading-warp {
  display: flex;
  justify-content: center;
  margin: 2rem 0;

  .btn {
    padding: 0.6rem 2rem;
    border-radius: 30px;
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-3px);
      box-shadow: 0 5px 15px rgba(233, 84, 107, 0.3);
    }
  }
}

@media (max-width: 992px) {
  .talk-item {
    width: 100%;
    left: 0 !important;
    margin-bottom: 2rem;

    &::after {
      display: none;
    }
  }

  .timeline-line {
    left: 30px;
  }

  .left-item .timeline-dot, .right-item .timeline-dot {
    left: 10px;
    right: auto;
  }

  .timeline-deco {
    display: none;
  }
}
</style>