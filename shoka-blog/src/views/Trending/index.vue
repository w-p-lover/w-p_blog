<template>
  <div class="page-header">
    <h1 class="page-title">热门开源项目</h1>
    <img class="page-cover" src="../../assets/images/bg.jpg" alt="">
    <Waves></Waves>
  </div>
  <div class="bg">
    <div class="page-container">
      <!-- 筛选 -->
      <div class="filter-bar">
        <el-select v-model="language" placeholder="选择语言" @change="fetchTrending" style="width: 200px;">
          <el-option label="全部" value=""></el-option>
          <el-option label="Java" value="Java"></el-option>
          <el-option label="Python" value="Python"></el-option>
          <el-option label="Go" value="Go"></el-option>
          <el-option label="JavaScript" value="JavaScript"></el-option>
        </el-select>
      </div>

      <!-- 标题 -->
      <div class="archive-title">项目总览 - {{ count }}</div>

      <!-- 列表 -->
      <div class="archive-list">
        <div class="archive-item" v-for="repo in trendingList" :key="repo.repoName">
          <a class="article-cover" :href="repo.url" target="_blank">
            <img class="cover" v-lazy="repo.projectImage || defaultImg">
          </a>
          <div class="article-info">
            <div class="article-time">
              <svg-icon icon-class="search" style="margin-right:0.4rem;"></svg-icon>
              Stars: {{ repo.stars }}
            </div>
            <a class="article-title" :href="repo.url" target="_blank">
              {{ repo.repoName }} <span style="color:#409eff;">({{ repo.language }})</span>
            </a>
            <div class="article-desc">{{ repo.description || '暂无简介' }}</div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <Pagination v-if="count > 0"
                  v-model:current="queryParams.current"
                  :total="Math.ceil(count / queryParams.size)">
      </Pagination>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, toRefs, onMounted, watch } from 'vue'
import axios from 'axios'
import Pagination from '@/components/Pagination/index.vue'

const defaultImg = '/default-cover.png'

const data = reactive({
  count: 0,
  language: '',
  queryParams: {
    current: 1,
    size: 6
  },
  trendingList: [] as any[]
})

const { count, language, queryParams, trendingList } = toRefs(data)

const fetchTrending = async () => {
  try {
    const { data: res } = await axios.get('http://localhost:8080/gitee/trending/list', {
      params: {
        language: language.value,
        current: queryParams.value.current,
        size: queryParams.value.size,
      }
    })

    if (res.flag && res.data) {
      trendingList.value = res.data.recordList || []
      count.value = res.data.count || 0
    } else {
      trendingList.value = []
      count.value = 0
    }

    console.log(trendingList.value)
  } catch (err) {
    console.error(err)
    trendingList.value = []
    count.value = 0
  }
}


watch(
    () => queryParams.value.current,
    () => {
      fetchTrending()
    }
)

onMounted(fetchTrending)
</script>

<style lang="scss" scoped>
.filter-bar {
  margin-bottom: 20px;
}

.archive-title {
  position: relative;
  margin-left: 10px;
  padding-bottom: 20px;
  padding-left: 20px;
  font-size: 1.5rem;

  &::before {
    position: absolute;
    top: 16px;
    left: -8px;
    z-index: 1;
    width: 18px;
    height: 18px;
    border: 5px solid var(--color-blue);
    border-radius: 10px;
    content: '';
    line-height: 10px;
  }

  &::after {
    position: absolute;
    bottom: 0;
    left: 0;
    z-index: 0;
    width: 2px;
    height: 1.5em;
    background: #aadafa;
    content: '';
  }
}

.archive-list {
  margin-left: 10px;
  padding-left: 20px;
  border-left: 2px solid #aadafa;
}

.archive-item {
  position: relative;
  display: flex;
  /* align-items: center; */
  margin: 0 0 40px 10px;
}

.article-cover {
  width: 120px;
  height: 120px;
  overflow: hidden;
  border-radius: 12px;

  .cover {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: filter 375ms ease-in 0.2s, transform 0.6s;
  }
}

.cover:hover {
  transform: scale(1.1);
}

.article-info {
  display: flex;
  flex-direction: column;
  flex: 1;
  margin: 0 1rem;

  .article-time {
    font-size: 14px;
    font-weight: bold;
  }

  .article-title {
    font-size: 1rem;
    margin: 6px 0;
    font-weight: bold;
    transition: color 0.3s;
  }

  .article-title:hover {
    color: var(--primary-color);
    transform: translateX(8px);
  }

  .article-desc {
    font-size: 14px;
    color: #666;
    margin-top: 4px;
  }
}
</style>
