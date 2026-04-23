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
          <el-option
              v-for="(item, index) in trendingLangList"
              :key="index"
              :label="item"
              :value="item"
          />
        </el-select>
        <el-select v-model="trendingType" placeholder="选择方向" @change="fetchTrending" style="width: 200px;">
          <el-option label="全部" value=""></el-option>
          <el-option
              v-for="(item, index) in trendingTypeList.filter( i  => i !== '')"
              :key="index"
              :label="item"
              :value="item"
          />
        </el-select>
        <el-select v-model="trendingOrder" placeholder="选择排序" @change="fetchTrending" style="width: 200px;">
          <el-option label="抓取时间" value="scraped_at"></el-option>
          <el-option label="项目Star" value="stars"></el-option>
        </el-select>
        <el-button type="primary" @click="dialogVisible = true">提交爬虫任务</el-button>
      </div>

      <!-- 弹出表单 -->
      <el-dialog title="提交爬虫任务" v-model="dialogVisible" width="400px">
        <el-form :model="form" label-width="100px">
          <el-form-item label="编程语言">
            <el-select v-model="form.language" placeholder="选择编程语言">
              <el-option label="全部" value=""></el-option>
              <el-option v-for="(item, index) in trendingLangList" :key="index" :label="item" :value="item"/>
            </el-select>
          </el-form-item>
          <el-form-item label="爬取分类">
            <el-select v-model="form.category" placeholder="选择分类">
              <el-option v-for="(item, index) in categories" :key="index" :label="item" :value="item"/>
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="runSpider">提交</el-button>
        </template>
      </el-dialog>

      <!-- 标题 -->
      <div class="archive-title">项目总览 - {{ count }}</div>

      <!-- 列表 -->
      <div class="archive-list">
        <div class="archive-item" v-for="repo in trendingList" :key="repo.repoName">
          <a class="article-cover" :href="repo.url" target="_blank">
            <img class="cover" :src="repo.projectImage || defaultImg" alt=""/>
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
import {ref, reactive, toRefs, onMounted, watch} from 'vue'
import axios from 'axios'
import Pagination from '@/components/Pagination/index.vue'
import {ElNotification} from 'element-plus/dist/index.full.mjs'
import Waves from "@/components/Waves/index.vue";

const showSpiderNotification = (type: 'success' | 'error' | 'warning' | 'info', message: string) => {
  ElNotification({
    title: '爬虫状态',
    message,
    type,
    duration: 5000, // 显示 5 秒后自动关闭
    offset: 60,     // 距离顶部 60px
    dangerouslyUseHTMLString: true
  })
}

const dialogVisible = ref(false)
const defaultImg = '/default-cover.png'

const data = reactive({
  count: 0,
  language: '',
  trendingType: '',
  trendingOrder: 'stars',
  trendingTypeList: [] as any[],
  trendingLangList: [] as any[],
  queryParams: {
    current: 1,
    size: 6
  },
  trendingList: [] as any[]
})
const loading = ref(false)
const {
  count,
  language,
  trendingType,
  trendingOrder,
  queryParams,
  trendingList,
  trendingTypeList,
  trendingLangList
} = toRefs(data)

const form = reactive({language: '', category: ''})
const categories = [
  '前沿技术', 'OpenHarmony', '程序开发', '人工智能', '区块链', '微信开发', '企业应用',
  '建站系统', '应用工具', 'WEB应用开发', 'DevOps/运维/网管',
  '手机/移动开发', '开发工具', '服务器应用', '数据库相关', '游戏/娱乐', '插件和扩展', '其他开源'
]

const runSpider = async () => {
  if (loading.value) return;
  loading.value = true;

  try {
    await axios.post('http://localhost:8080/gitee/trending/run', null, {
      params: {
        language: form.language,
        category: form.category
      }
    });
    showSpiderNotification('info',
        `<div style="text-align: left; line-height: 1.6; margin-left: 40px">
                    <strong>🚀 爬虫任务已启动</strong><br>
                            📝 编程语言: <span style="color:#409EFF;">${form.language}</span><br>
                            📂 爬取分类: <span style="color:#67C23A;">${form.category}</span>
                            </div>`);


    const startTime = Date.now();

    const pollStatus = async () => {
      try {
        const {data} = await axios.get('http://localhost:8080/gitee/trending/status');
        const status = data.data?.trim()?.toUpperCase();
        console.log('爬虫状态:', status);

        // 完成/失败或超时
        if (['COMPLETED', 'FAILED'].includes(status) || Date.now() - startTime > 60000) {
          loading.value = false;
          await fetchTrending();

          if (status === 'COMPLETED') {
            showSpiderNotification('success', '爬虫任务完成！');
          } else if (status === 'FAILED') {
            showSpiderNotification('error', '爬虫任务失败！');
          } else {
            showSpiderNotification('warning', '爬虫任务超时！');
          }

        } else {
          // 每次轮询间隔 5 秒
          setTimeout(pollStatus, 5000);
        }

      } catch (err) {
        console.error('轮询失败:', err);
        loading.value = false;
        showSpiderNotification('error', '轮询失败，请重试！');
      }
    };

    // 启动第一次轮询
    await pollStatus();

  } catch (err) {
    console.error('启动爬虫失败:', err);
    loading.value = false;
    showSpiderNotification('error', '启动爬虫失败！');
  }
};


const fetchTrending = async () => {
  try {
    const {data: res} = await axios.get('http://localhost:8080/gitee/trending/list', {
      params: {
        language: language.value,
        trendingType: trendingType.value,
        sortType: trendingOrder.value,
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
  } catch (err) {
    console.error(err)
    trendingList.value = []
    count.value = 0
  }
}
const getTypeList = async () => {
  try {
    const {data: res} = await axios.get('http://localhost:8080/gitee/trending/type')
    if (res.flag && res.data) {
      trendingTypeList.value = res.data
    }
  } catch (err) {
    console.error(err)
  }
}

const getLangList = async () => {
  try {
    const {data: res} = await axios.get('http://localhost:8080/gitee/trending/language')
    if (res.flag && res.data) {
      trendingLangList.value = res.data
    }
  } catch (err) {
    console.error(err)
  }
}
watch(
    () => queryParams.value.current,
    () => {
      fetchTrending()
    }
)
onMounted(getLangList)
onMounted(fetchTrending)
onMounted(getTypeList)
</script>

<style lang="scss" scoped>

:deep(.el-form) {
  padding-top: 15px;
}

:deep(.el-form-item__content) {
  padding-right: 20px;
}

:deep(.el-dialog) {
  margin-top: 300px;
}

.filter-bar {
  display: flex;
  gap: 20px; /* 每个子元素水平间隔 */
  margin-bottom: 15px;
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

.archive-item::before {
  position: absolute;
  left: -36px;
  width: 10px;
  height: 10px;
  border: 3px solid var(--color-blue);
  border-radius: 6px;
  background: var(--grey-0);
  content: '';
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
