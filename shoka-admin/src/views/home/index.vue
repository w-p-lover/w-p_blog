<template>
  <div class="dashboard-container">
    <section class="dashboard-hero">
      <div class="hero-copy">
        <span class="hero-kicker">Shoka Admin</span>
        <h1>创作与运营概览</h1>
        <p>把文章、访客、留言和分类数据收在一个更安静的工作台里，打开首页就知道今天该先看哪里。</p>
      </div>

      <div class="hero-actions">
        <el-button class="ghost-btn" @click="refreshDashboard" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="primary" class="primary-btn" @click="goWrite">
          <el-icon><EditPen /></el-icon>
          写文章
        </el-button>
      </div>
    </section>

    <el-alert
        v-if="errorText"
        :title="errorText"
        type="warning"
        show-icon
        :closable="false"
        class="dashboard-alert"
    />

    <section class="metric-grid" v-loading="loading">
      <article v-for="item in metricCards" :key="item.label" class="metric-card">
        <div class="metric-icon" :class="item.className">
          <el-icon>
            <component :is="item.icon" />
          </el-icon>
        </div>
        <div class="metric-content">
          <span>{{ item.label }}</span>
          <strong>{{ formatNumber(item.value) }}</strong>
        </div>
      </article>
    </section>

    <section class="dashboard-section contribution-section">
      <div class="section-head">
        <div>
          <span class="section-kicker">Contribution</span>
          <h2>文章贡献统计</h2>
        </div>
        <span class="section-note">近一年发布节奏</span>
      </div>
      <div class="heatmap-wrap">
        <calendar-heatmap
            :values="articleStatisticsList"
            :end-date="new Date()"
            tooltip-unit="篇文章"
            :tooltip-formatter="formatContributionTooltip"
            :round="2"
            :range-color="['#eee7d9', '#d8e7d7', '#9fc7b8', '#5f8f86', '#42766e']"
        />
      </div>
    </section>

    <section class="insight-grid">
      <article class="dashboard-section">
        <div class="section-head">
          <div>
            <span class="section-kicker">Popular</span>
            <h2>文章浏览排行</h2>
          </div>
        </div>
        <Echarts :options="articleRank" height="340px" />
      </article>

      <article class="dashboard-section">
        <div class="section-head">
          <div>
            <span class="section-kicker">Category</span>
            <h2>文章分类统计</h2>
          </div>
        </div>
        <Echarts :options="category" height="340px" />
      </article>

      <article class="dashboard-section tag-section">
        <div class="section-head">
          <div>
            <span class="section-kicker">Tags</span>
            <h2>标签云</h2>
          </div>
        </div>
        <TagCloud v-if="tagLoad && tagList.length" :tag-list="tagList" />
        <el-empty v-else description="暂无标签数据" :image-size="96" />
      </article>
    </section>

    <section class="dashboard-section visit-section">
      <div class="section-head">
        <div>
          <span class="section-kicker">Traffic</span>
          <h2>一周访问趋势</h2>
        </div>
        <span class="section-note">PV / UV</span>
      </div>
      <Echarts :options="userView" height="360px" />
    </section>
  </div>
</template>

<script setup lang="ts">
import { TagVO } from "@/api/article/types";
import { getBlogInfo } from "@/api/blog";
import { ArticleStatisticsVO } from "@/api/blog/types";
import TagCloud from "@/components/TagCloud/index.vue";
import {
  ChatDotRound,
  EditPen,
  Refresh,
  User,
  View,
} from "@element-plus/icons-vue";
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";

const router = useRouter();

const tagList = ref<TagVO[]>([]);
const viewCount = ref(0);
const messageCount = ref(0);
const userCount = ref(0);
const tagLoad = ref(false);
const articleCount = ref(0);
const articleStatisticsList = ref<ArticleStatisticsVO[]>([]);
const loading = ref(false);
const errorText = ref("");

const chartTextColor = "#6f6252";
const chartMutedColor = "#a09380";
const chartLineColor = "rgba(126, 111, 88, 0.16)";
const chartPrimary = "#5f8f86";
const chartPrimaryDeep = "#42766e";
const chartAccent = "#ba6a58";

const metricCards = computed(() => [
  {
    label: "访问量",
    value: viewCount.value,
    icon: View,
    className: "is-view",
  },
  {
    label: "文章数",
    value: articleCount.value,
    icon: EditPen,
    className: "is-article",
  },
  {
    label: "用户数",
    value: userCount.value,
    icon: User,
    className: "is-user",
  },
  {
    label: "留言数",
    value: messageCount.value,
    icon: ChatDotRound,
    className: "is-message",
  },
]);

const userView = reactive({
  color: [chartPrimary, chartAccent],
  tooltip: {
    trigger: "axis",
    axisPointer: {
      type: "line",
      lineStyle: {
        color: "rgba(95, 143, 134, 0.28)",
      },
    },
    padding: [8, 12],
    backgroundColor: "rgba(255, 253, 248, 0.96)",
    borderColor: chartLineColor,
    textStyle: {
      color: chartTextColor,
    },
  },
  legend: {
    top: 0,
    right: 4,
    textStyle: {
      color: chartMutedColor,
    },
    data: ["访问量(PV)", "独立访客(UV)"],
  },
  grid: {
    left: 8,
    right: 20,
    bottom: 0,
    top: 44,
    containLabel: true,
  },
  xAxis: {
    type: "category",
    data: [] as string[],
    boundaryGap: false,
    axisTick: {
      show: false,
    },
    axisLine: {
      lineStyle: {
        color: chartLineColor,
      },
    },
    axisLabel: {
      color: chartMutedColor,
    },
  },
  yAxis: {
    type: "value",
    axisTick: {
      show: false,
    },
    splitLine: {
      lineStyle: {
        color: chartLineColor,
      },
    },
    axisLabel: {
      color: chartMutedColor,
    },
  },
  series: [
    {
      name: "访问量(PV)",
      type: "line",
      smooth: true,
      symbolSize: 7,
      data: [] as number[],
      lineStyle: {
        width: 3,
      },
      areaStyle: {
        color: {
          type: "linear",
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: "rgba(95, 143, 134, 0.24)" },
            { offset: 1, color: "rgba(95, 143, 134, 0.02)" },
          ],
        },
      },
      animationDuration: 1400,
      animationEasing: "cubicOut",
    },
    {
      name: "独立访客(UV)",
      type: "line",
      smooth: true,
      symbolSize: 7,
      data: [] as number[],
      lineStyle: {
        width: 3,
      },
      animationDuration: 1400,
      animationEasing: "quadraticOut",
    },
  ],
});

const category = reactive({
  color: ["#5f8f86", "#ba6a58", "#d6a85f", "#7b8fa1", "#8f755f", "#74a68f"],
  tooltip: {
    trigger: "item",
    formatter: "{b}<br/>文章：{c} 篇 ({d}%)",
    backgroundColor: "rgba(255, 253, 248, 0.96)",
    borderColor: chartLineColor,
    textStyle: {
      color: chartTextColor,
    },
  },
  legend: {
    bottom: 0,
    type: "scroll",
    textStyle: {
      color: chartMutedColor,
    },
  },
  series: [
    {
      name: "分类统计",
      type: "pie",
      radius: ["36%", "68%"],
      center: ["50%", "43%"],
      avoidLabelOverlap: true,
      itemStyle: {
        borderRadius: 5,
        borderColor: "#fffdf8",
        borderWidth: 2,
      },
      label: {
        color: chartTextColor,
      },
      data: [] as {
        value: number;
        name: string;
      }[],
    },
  ],
});

const articleRank = reactive({
  tooltip: {
    trigger: "axis",
    axisPointer: {
      type: "shadow",
    },
    backgroundColor: "rgba(255, 253, 248, 0.96)",
    borderColor: chartLineColor,
    textStyle: {
      color: chartTextColor,
    },
  },
  grid: {
    left: 0,
    right: 10,
    bottom: 0,
    top: 18,
    containLabel: true,
  },
  xAxis: {
    type: "category",
    data: [] as string[],
    axisTick: {
      alignWithLabel: true,
    },
    axisLine: {
      lineStyle: {
        color: chartLineColor,
      },
    },
    axisLabel: {
      color: chartMutedColor,
      interval: 0,
      overflow: "truncate",
      width: 72,
    },
  },
  yAxis: {
    type: "value",
    axisTick: {
      show: false,
    },
    splitLine: {
      lineStyle: {
        color: chartLineColor,
      },
    },
    axisLabel: {
      color: chartMutedColor,
    },
  },
  series: [
    {
      name: "浏览量",
      type: "bar",
      barMaxWidth: 28,
      data: [] as number[],
      itemStyle: {
        borderRadius: [5, 5, 0, 0],
        color: {
          type: "linear",
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: chartPrimary },
            { offset: 1, color: chartPrimaryDeep },
          ],
        },
      },
    },
  ],
});

const formatNumber = (value: number) => {
  return new Intl.NumberFormat("zh-CN").format(value || 0);
};

const formatContributionTooltip = (day: { date: Date; count?: number }) => {
  const date = day.date.toLocaleDateString("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });
  return `${date} 发布 ${day.count || 0} 篇文章`;
};

const resetCharts = () => {
  articleRank.xAxis.data = [];
  articleRank.series[0].data = [];
  category.series[0].data = [];
  userView.xAxis.data = [];
  userView.series[0].data = [];
  userView.series[1].data = [];
};

const refreshDashboard = () => {
  loading.value = true;
  errorText.value = "";
  resetCharts();

  getBlogInfo()
      .then(({ data }) => {
        const info = data.data;
        viewCount.value = info?.viewCount || 0;
        messageCount.value = info?.messageCount || 0;
        userCount.value = info?.userCount || 0;
        articleCount.value = info?.articleCount || 0;
        articleStatisticsList.value = info?.articleStatisticsList || [];

        tagList.value = info?.tagVOList || [];
        tagLoad.value = true;

        info?.articleRankVOList?.forEach((item) => {
          articleRank.series[0].data.push(item.viewCount);
          articleRank.xAxis.data.push(item.articleTitle);
        });

        info?.categoryVOList?.forEach((item) => {
          category.series[0].data.push({
            value: item.articleCount,
            name: item.categoryName,
          });
        });

        info?.userViewVOList?.forEach((item) => {
          userView.xAxis.data.push(item.date);
          userView.series[0].data.push(item.pv);
          userView.series[1].data.push(item.uv);
        });
      })
      .catch(() => {
        errorText.value = "首页数据加载失败，请稍后重试。";
      })
      .finally(() => {
        loading.value = false;
      });
};

const goWrite = () => {
  router.push({ path: "/article/write" });
};

onMounted(() => {
  refreshDashboard();
});
</script>

<style lang="scss" scoped>
.dashboard-container {
  --writer-paper: #fffdf8;
  --writer-paper-deep: #f7f1e6;
  --writer-line: rgba(126, 111, 88, 0.18);
  --writer-text: #40382e;
  --writer-muted: #8b7c69;
  --writer-primary: #5f8f86;
  --writer-primary-deep: #42766e;
  --writer-accent: #ba6a58;

  min-height: calc(100vh - 84px);
  padding: 18px 18px 28px;
  overflow-x: hidden;
  background:
      linear-gradient(135deg, rgba(255, 253, 248, 0.98), rgba(247, 241, 230, 0.92)),
      linear-gradient(180deg, rgba(95, 143, 134, 0.06), rgba(186, 106, 88, 0.05));
  color: var(--writer-text);
}

.dashboard-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
  padding: 22px 24px;
  border: 1px solid rgba(126, 111, 88, 0.12);
  border-radius: 8px;
  background:
      linear-gradient(90deg, rgba(255, 253, 248, 0.9), rgba(247, 241, 230, 0.64)),
      rgba(255, 253, 248, 0.74);
  box-shadow: 0 16px 42px rgba(77, 65, 48, 0.07);
}

.hero-copy {
  min-width: 0;

  h1 {
    margin: 3px 0 8px;
    color: var(--writer-text);
    font-size: 28px;
    font-weight: 800;
    line-height: 1.25;
    letter-spacing: 0;
  }

  p {
    max-width: 620px;
    margin: 0;
    color: var(--writer-muted);
    font-size: 14px;
    line-height: 1.7;
  }
}

.hero-kicker,
.section-kicker {
  color: var(--writer-primary-deep);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
}

.hero-actions {
  display: flex;
  flex: 0 0 auto;
  gap: 10px;
}

.ghost-btn,
.primary-btn {
  height: 36px;
  border-radius: 18px;
}

.ghost-btn {
  border-color: rgba(95, 143, 134, 0.26);
  background: rgba(255, 253, 248, 0.68);
  color: var(--writer-primary-deep);
}

.primary-btn {
  border-color: var(--writer-primary);
  background: var(--writer-primary);
  color: #fff;
}

.primary-btn:hover,
.primary-btn:focus {
  border-color: var(--writer-primary-deep);
  background: var(--writer-primary-deep);
}

.dashboard-alert {
  margin-bottom: 16px;
  border-radius: 8px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.metric-card {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
  padding: 18px;
  border: 1px solid var(--writer-line);
  border-radius: 8px;
  background: rgba(255, 253, 248, 0.86);
  box-shadow: 0 10px 28px rgba(77, 65, 48, 0.04);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;

  &:hover {
    border-color: rgba(95, 143, 134, 0.34);
    box-shadow: 0 14px 34px rgba(77, 65, 48, 0.08);
    transform: translateY(-2px);
  }
}

.metric-icon {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: 8px;
  font-size: 22px;

  &.is-view {
    background: rgba(95, 143, 134, 0.12);
    color: var(--writer-primary-deep);
  }

  &.is-article {
    background: rgba(186, 106, 88, 0.12);
    color: var(--writer-accent);
  }

  &.is-user {
    background: rgba(123, 143, 161, 0.13);
    color: #627889;
  }

  &.is-message {
    background: rgba(214, 168, 95, 0.15);
    color: #9b6f2f;
  }
}

.metric-content {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 6px;

  span {
    color: var(--writer-muted);
    font-size: 13px;
  }

  strong {
    overflow: hidden;
    color: var(--writer-text);
    font-size: 25px;
    font-weight: 800;
    line-height: 1;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.dashboard-section {
  min-width: 0;
  padding: 18px;
  border: 1px solid var(--writer-line);
  border-radius: 8px;
  background: rgba(255, 253, 248, 0.86);
  box-shadow: 0 10px 28px rgba(77, 65, 48, 0.035);
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;

  h2 {
    margin: 3px 0 0;
    color: var(--writer-text);
    font-size: 18px;
    font-weight: 800;
    line-height: 1.35;
    letter-spacing: 0;
  }
}

.section-note {
  flex: 0 0 auto;
  color: var(--writer-muted);
  font-size: 13px;
}

.contribution-section,
.visit-section {
  margin-bottom: 16px;
}

.heatmap-wrap {
  overflow-x: auto;
  padding: 6px 2px 0;
}

.insight-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.tag-section {
  overflow: hidden;
}

.tag-section :deep(.cloud_wrap) {
  height: 340px;
}

.tag-section :deep(.tagcloud-all .tag) {
  font-weight: 700;
  transition: color 0.2s ease, transform 0.2s ease;
}

:deep(.el-loading-mask) {
  border-radius: 8px;
  background-color: rgba(255, 253, 248, 0.68);
}

@media (max-width: 1200px) {
  .metric-grid,
  .insight-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .dashboard-container {
    padding: 12px;
  }

  .dashboard-hero {
    align-items: flex-start;
    flex-direction: column;
    padding: 18px;
  }

  .hero-copy h1 {
    font-size: 24px;
  }

  .hero-actions {
    width: 100%;

    .el-button {
      flex: 1;
    }
  }

  .metric-grid,
  .insight-grid {
    grid-template-columns: 1fr;
  }

  .metric-card {
    padding: 16px;
  }
}
</style>
