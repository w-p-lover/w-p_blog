<template>
  <div class="page-header">
    <h1 class="page-title">文章归档</h1>
    <img class="page-cover" src="../../assets/images/bg.jpg" alt="背景图">
    <Waves></Waves>
  </div>
  <div class="bg">
    <div class="page-container">
      <!-- 年份导航栏 - 固定在顶部 -->
      <div class="year-nav">
        <div v-for="year in allYears"
            :key="year"
            @click="scrollToYear(year)"
            :class="['year-item', currentYear === year ? 'active' : '']"
        >
          {{ year }}年
        </div>
      </div>
      <div
          v-for="(yearGroup, year) in groupedArchives"
          :key="year"
          :id="`year-${year}`"
          class="year-section"
      >
        <!-- 年份分隔带 -->
        <div class="year-divider">
          <span class="year-label">{{ year }}年</span>
          <div class="divider-line"></div>
        </div>

        <div class="archive-list">
          <div
              class="archive-item"
              v-for="archive in yearGroup"
              :key="archive.id"
              :data-time="archive.createTime"
          >
            <!-- 时间轴节点 -->
            <div class="timeline-dot" :data-year="year"></div>

            <router-link class="article-cover" :to="`/article/${archive.id}`">
              <div class="cover-wrapper">
                <img class="cover" v-lazy="archive.articleCover" :alt="archive.articleTitle">
                <div class="cover-mask">查看全文</div>
              </div>
            </router-link>

            <div class="article-info">
              <div class="article-time">
                <svg-icon icon-class="calendar" style="margin-right:0.4rem;"></svg-icon>
                <time>{{ formatDate(archive.createTime) }}</time>
                <span v-if="archive.tags && archive.tags.length" class="article-tags">
                  <span v-for="tag in archive.tags" :key="tag.id" class="tag-item">{{ tag.name }}</span>
                </span>
              </div>
              <router-link class="article-title" :to="`/article/${archive.id}`">
                {{ archive.articleTitle }}
              </router-link>
              <p class="article-excerpt" :class="{ show: hoveredId === archive.id }">
                {{ archive.excerpt || '点击查看文章详情...' }}
              </p>
            </div>
          </div>
        </div>
      </div>

      <Pagination
          v-if="count > 0"
          v-model:current="queryParams.current"
          :total="Math.ceil(count / 5)"
          class="pagination"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { getArchivesList } from '@/api/archives';
import { Archives } from '@/api/archives/types';
import Pagination from '@/components/Pagination/index.vue';
import { PageQuery } from '@/model';
import { formatDate } from '@/utils/date';
import Waves from "@/components/Waves/index.vue";
import { onMounted, reactive, toRefs, watch, ref } from 'vue';

// 响应式数据
const data = reactive({
  count: 0,
  queryParams: {
    current: 1,
    size: 10,
  } as PageQuery,
  archivesList: [] as Archives[],
  groupedArchives: {} as Record<string, Archives[]>, // 按年份分组的归档数据
  allYears: [] as number[], // 所有存在的年份
  currentYear: 0, // 当前活跃的年份
});

const { count, queryParams, archivesList, groupedArchives, allYears, currentYear } = toRefs(data);
const hoveredId = ref<number | null>(null); // 用于控制摘要显示的ID

// 按年份分组归档数据
const groupByYear = (list: Archives[]) => {
  const groups: Record<string, Archives[]> = {};
  list.forEach(item => {
    const year = new Date(item.createTime).getFullYear();
    if (!groups[year]) {
      groups[year] = [];
    }
    groups[year].push(item);
  });
  console.log(groups);
  // 按年份倒序排列
  const sortedYears = Object.keys(groups).sort((a, b) => Number(a) - Number(b));
  const sortedGroups: Record<string, Archives[]> = {};
  sortedYears.forEach(year => {
    // 同一年的文章按时间倒序排列
    sortedGroups[year] = groups[year].sort((a, b) =>
        new Date(b.createTime).getTime() - new Date(a.createTime).getTime()
    );
  });
  return { groups: sortedGroups, years: sortedYears.map(Number) };
};

// 滚动到指定年份区域
const scrollToYear = (year: number) => {
  const element = document.getElementById(`year-${year}`);
  if (element) {
    window.scrollTo({
      top: element.offsetTop - 80, // 减去导航栏高度
      behavior: 'smooth'
    });
  }
};

// 监听滚动，更新当前活跃年份
const handleScroll = () => {
  const scrollTop = window.scrollY;
  const yearSections = document.querySelectorAll('.year-section');

  yearSections.forEach(section => {
    const rect = section.getBoundingClientRect();
    const sectionTop = rect.top + scrollTop;
    const sectionBottom = sectionTop + rect.height;

    // 当区域进入视口（顶部在视口内或底部在视口内）
    if (scrollTop >= sectionTop - 200 && scrollTop < sectionBottom - 200) {
      const year = Number((section as HTMLElement).id.split('-')[1]);
      currentYear.value = year;
      // 高亮当前年份的时间轴节点
      document.querySelectorAll('.timeline-dot').forEach(dot => {
        dot.classList.toggle('active', (dot as HTMLElement).dataset.year === year.toString());
      });
    }
  });
};

// 监听分页变化
watch(
    () => queryParams.value.current,
    () => {
      getArchivesList(queryParams.value).then(({ data }) => {
        archivesList.value = data.data.recordList;
        count.value = data.data.count;
        const { groups, years } = groupByYear(archivesList.value);
        groupedArchives.value = groups;
        allYears.value = years;
      });
    }
);

// 初始化
onMounted(() => {
  getArchivesList(queryParams.value).then(({ data }) => {
    archivesList.value = data.data.recordList;
    count.value = data.data.count;
    const { groups, years } = groupByYear(archivesList.value);
    groupedArchives.value = groups;
    allYears.value = years;
  });

  // 监听滚动事件
  window.addEventListener('scroll', handleScroll);
  // 初始触发一次滚动检查
  setTimeout(handleScroll, 100);
});

// 清理滚动监听
onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll);
});
</script>

<style lang="scss" scoped>
// 页面头部样式
.page-header {
  position: relative;
  margin-left: 10px;
  padding-bottom: 20px;
  padding-left: 20px;

  &::before {
    position: absolute;
    top: 45%;
    left: 50%;
    transform: translate(-50%, -50%);
    color: var(--grey-0);
    font-size: 3.8rem;
    font-weight: 700;
    text-shadow: 0 3px 15px rgba(0, 0, 0, 0.3);
    z-index: 2;
    margin: 0;
    padding: 0 20px;
    text-align: center;
    opacity: 0;
    animation: fadeUp 1s ease-out 0.3s forwards;
  }

  .page-subtitle {
    position: absolute;
    top: 60%;
    left: 50%;
    transform: translate(-50%, -50%);
    color: var(--grey-0);
    font-size: 1.2rem;
    z-index: 2;
    margin: 0;
    opacity: 0;
    animation: fadeUp 1s ease-out 0.6s forwards;
  }
}

@keyframes fadeUp {
  from {
    opacity: 0;
    transform: translate(-50%, 20px);
  }
  to {
    opacity: 1;
    transform: translate(-50%, -50%);
  }
}

// 年份导航栏
.year-nav {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  gap: 1rem;
  padding: 0.8rem 0;
  background: var(--grey-0);;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  overflow-x: auto;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }

  .year-item {
    padding: 0.4rem 1rem;
    border-radius: 20px;
    font-size: 1rem;
    color: #666;
    cursor: pointer;
    white-space: nowrap;
    transition: all 0.3s ease;

    &:hover {
      color: var(--color-blue);
      background: rgba(103, 161, 223, 0.1);
    }

    &.active {
      color: rgb(123, 223, 223);
      background: var(--color-blue);
      font-weight: 500;
      box-shadow: 0 3px 8px rgba(107, 156, 209, 0.3);
    }
  }
}

// 年份分隔带
.year-section {
  margin-bottom: 3rem;
  padding-top: 1rem;
}

.year-divider {
  display: flex;
  align-items: center;
  margin: 2rem 0;
  position: relative;

  .year-label {
    position: relative;
    z-index: 2;
    padding: 0.5rem 1.5rem;
    background: var(--color-blue);
    color: rgba(255, 255, 255, 0.79);
    font-size: 1.3rem;
    font-weight: 600;
    border-radius: 6px;
    box-shadow: 0 3px 10px rgba(45, 140, 240, 0.2);
  }

  .divider-line {
    flex: 1;
    height: 2px;
    background: linear-gradient(90deg, rgba(170, 218, 250, 0) 0%, rgba(170, 218, 250, 1) 100%);
    position: absolute;
    top: 50%;
    left: 0;
    right: 0;
    z-index: 1;
  }
}

// 归档列表
.archive-list {
  position: relative;
  padding-left: 35px;
  margin-left: 10px;

  // 左侧时间轴线
  &::before {
    content: '';
    position: absolute;
    left: 10px;
    top: 0;
    bottom: 0;
    width: 2px;
    background: linear-gradient(to bottom, #aadafa 0%, #aadafa 80%, rgba(170, 218, 250, 0.3) 100%);
  }
}

// 时间轴节点
.timeline-dot {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%) translateX(-30px); // 定位到时间轴上
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--grey-0);;
  border: 3px solid #aadafa;
  transition: all 0.4s cubic-bezier(0.2, 0.8, 0.2, 1);
  z-index: 2;

  &.active {
    width: 16px;
    height: 16px;
    border-color: var(--color-blue);
    box-shadow: 0 0 0 4px rgba(45, 140, 240, 0.15);
    transform: translateY(-50%) translateX(-32px) scale(1.1);
  }
}

// 文章项
.archive-item {
  position: relative;
  display: flex;
  align-items: center;
  margin-bottom: 2.5rem;
  padding: 15px;
  border-radius: 12px;
  background: var(--grey-0);;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.03);
  transition: all 0.4s cubic-bezier(0.2, 0.8, 0.2, 1);
  opacity: 0;
  transform: translateX(-20px);
  animation: fadeInRight 0.6s ease-out forwards;
  animation-delay: calc(var(--index) * 0.1s); // 按顺序延迟出现

  &:hover {
    transform: translateY(-5px) translateX(0);
    box-shadow: 0 10px 25px rgba(0, 0, 0, 0.08);
  }

  // 为每个item设置索引，用于动画延迟
  &:nth-child(1) { --index: 1; }
  &:nth-child(2) { --index: 2; }
  &:nth-child(3) { --index: 3; }
  &:nth-child(4) { --index: 4; }
  &:nth-child(5) { --index: 5; }
}

@keyframes fadeInRight {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

// 文章封面
.article-cover {
  width: 140px;
  height: 140px;
  border-radius: 10px;
  overflow: hidden;
  flex-shrink: 0;
  position: relative;
  transition: transform 0.4s ease;

  &:hover {
    transform: rotate(2deg) scale(1.03);
  }

  .cover-wrapper {
    width: 100%;
    height: 100%;
    overflow: hidden;
  }

  .cover {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.8s cubic-bezier(0.2, 0.8, 0.2, 1);
  }

  .cover:hover {
    transform: scale(1.2);
  }

  .cover-mask {
    position: absolute;
    inset: 0;
    background: rgba(0, 0, 0, 0.4);
    color: var(--grey-0);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 0.9rem;
    opacity: 0;
    transition: opacity 0.3s ease;
  }

  &:hover .cover-mask {
    opacity: 1;
  }
}

// 文章信息
.article-info {
  display: flex;
  flex-direction: column;
  flex: 1;
  margin: 0 1.5rem;
  min-width: 0; // 解决flex子元素溢出问题

  .article-time {
    font-size: 0.9rem;
    color: #888;
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 0.5rem;
    margin-bottom: 0.5rem;
  }

  .article-tags {
    display: flex;
    gap: 0.5rem;
    margin-left: auto;

    .tag-item {
      padding: 0.2rem 0.6rem;
      background: rgba(170, 218, 250, 0.3);
      color: #666;
      font-size: 0.8rem;
      border-radius: 4px;
    }
  }

  .article-title {
    font-size: 1.2rem;
    font-weight: 600;
    color: var(--grey-9);
    margin: 0.3rem 0 0.5rem;
    transition: color 0.3s ease;
    line-height: 1.4;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;

    &:hover {
      color: var(--color-blue);
    }
  }

  .article-excerpt
  {
    font-size: 0.9rem;
    color: #666;
    line-height: 1.6;
    max-height: 0;
    opacity: 0;
    overflow: hidden;
    transition: all 0.4s ease;
    margin: 0;

    &.show {
      max-height: 100px;
      opacity: 1;
      margin-top: 0.8rem;
    } }
  }

// 分页样式
.pagination {
  margin: 3rem auto 2rem;
  text-align: center;
}

// 响应式适配
@media (max-width: 768px) {
  .page-header {
    height: 35vh;
    min-height: 260px;
  }

  .page-subtitle {
    font-size: 1rem;
    top: 65%;
  }

  .archive-item {
    flex-direction: column;
    align-items: flex-start;
    padding: 15px;
  }

  .article-cover {
    width: 100%;
    height: 180px;
    margin-bottom: 1rem;
  }

  .article-info {
    margin: 0;
  }

  .article-title {
    font-size: 1.1rem;
    white-space: normal; // 移动端允许标题换行
  }

  .article-tags {
    margin-left: 0;
    margin-top: 0.5rem;
    width: 100%;
  }

  .timeline-dot {
    top: 30px; // 时间轴节点定位到封面上方
  }
}
</style>