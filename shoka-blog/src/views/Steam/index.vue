<template>
  <div class="page-header">
    <div class="header-overlay">
      <h1 class="page-title">我的游戏库</h1>
      <div class="filter-tag">已下载游戏</div>
    </div>
    <img class="page-cover" src="../../assets/images/bg.jpg" alt="游戏库背景"/>
    <Waves></Waves>
  </div>

  <div class="bg">
    <div class="page-container">
      <div class="title-bar">
        <div class="game-title">
          游戏总览
          <span class="count-badge">({{ count }})</span>
          <img :src="steamIcon" alt="Steam" style="width: 25px; vertical-align: middle; margin-left: 20px"/>
        </div>

        <div class="sort-dropdown">
          <span class="sort-label">排序：</span>
          <el-select v-model="sortType" placeholder="排序方式" size="default" @change="changeSort(sortType)">
            <el-option label="游玩时间" value="play_time"/>
            <el-option label="发布时间" value="release_date"/>
            <el-option label="游戏评分" value="rating"/>
          </el-select>
        </div>
      </div>

      <div class="steam-single-list">
        <div
            v-for="(game, index) in gameList"
            :key="game.id"
            class="steam-single-card"
            @click="openDetail(game)"
            :style="`animation-delay: ${index * 0.1}s`"
        >
          <div class="card-content">
            <div class="card-cover">
              <img
                  :src="game.coverUrl"
                  :alt="game.name"
                  @error="handleCoverError"
                  class="cover-img"
              />
              <div class="installed-tag" v-if="game.isInstalled">
                <i class="tag-icon">✓</i> 已下载
              </div>
            </div>

            <!-- 截图区域 -->
            <div class="card-img-area">
              <el-tooltip
                  v-for="(img, idx) in (game.screenshotUrl || [game.coverUrl, game.coverUrl])"
                  :key="idx"
                  effect="dark"
                  placement="top"
                  :hide-after="0"
                  :enterable="false"
                  :trigger="'hover'"
                  :show-after="400"
              >
                <template #content>
                  <img :src="img" alt="截图大图" style="max-width:400px; max-height:300px;"/>
                </template>
                <img
                    :src="img"
                    :alt="`${game.name}截图${idx+1}`"
                    @error="handleCoverError"
                    class="game-screenshot"
                />
              </el-tooltip>
            </div>


            <div class="card-info">
              <div class="info-header">
                <h3 class="game-title">{{ game.name }}</h3>
                <div class="game-tags" v-if="game.tags">
                  <span class="tag" v-for="(tag, i) in game.tags.slice(0, 3)" :key="i">{{ tag }}</span>
                </div>
              </div>

              <div class="game-meta">
                <div class="meta-item">
                  <i class="meta-icon">⏱️</i>
                  <span class="meta-text">{{ game.playTime + '小时' || '0小时' }}</span>
                </div>
                <div class="meta-item">
                  <i class="meta-icon">📅</i>
                  <span class="meta-text">{{ formatDateTime(game.lastPlayed) || '未游玩' }}</span>
                </div>
                <div class="meta-item">
                  <i class="meta-icon">⭐</i>
                  <span class="meta-text">{{ game.rating || '暂无评分' }}</span>
                </div>
              </div>

              <!-- 简介引导点击看详情 -->
              <p class="game-desc-preview">
                {{ game.description ? game.description.slice(0, 80) + '...' : '暂无游戏简介' }}
                <span class="view-detail"> 点击查看详情 </span>
              </p>
            </div>
          </div>
        </div>

        <div class="empty-single" v-if="count === 0">
          <div class="empty-wrapper">
            <img
                src="https://shared.cdn.queniuqe.com/store_item_assets/steam/apps/413150/capsule_616x353.jpg?t=1710874434"
                alt="空游戏库" class="empty-icon"/>
            <p class="empty-text">你的游戏库还是空的</p>
            <p class="empty-subtext">添加喜欢的游戏到收藏吧～</p>
          </div>
        </div>
      </div>

      <Pagination
          v-if="count > 0"
          v-model:current="queryParams.current"
          :total="Math.ceil(count / 5)"
          class="steam-pagination"
      />
    </div>
  </div>

  <Teleport to="body">
    <div class="steam-detail-modal" v-if="showDetail">
      <div class="modal-overlay" @click="closeDetail"></div>
      <div class="modal-content" @click.stop>
        <button class="modal-close" @click="closeDetail">×</button>

        <div class="detail-header">
          <div class="header-top">
            <h2 class="detail-title">{{ currentGame.articleTitle }}</h2>
            <div class="detail-rating" v-if="currentGame.rating">
              <i class="rating-icon">⭐</i>
              <span class="rating-value">{{ currentGame.rating }}</span>
            </div>
          </div>

          <div class="detail-tags" v-if="currentGame.tags">
            <span class="tag" v-for="(tag, i) in currentGame.tags" :key="i">{{ tag }}</span>
          </div>
        </div>

        <div class="detail-middle">
          <div class="middle-cover">
            <img
                :src="currentGame.coverUrl || steamIcon"
                :alt="currentGame.name"
                class="cover-large"
            />
          </div>
          <div class="middle-meta">
            <div class="meta-group">
              <span class="meta-label">游玩时长</span>
              <span class="meta-value">{{ currentGame.playTime || '0小时' }}</span>
            </div>
            <div class="meta-group">
              <span class="meta-label">最近游玩</span>
              <span class="meta-value">{{ formatDateTime(currentGame.lastPlayed) || '未游玩' }}</span>
            </div>
            <div class="meta-group">
              <span class="meta-label">发行日期</span>
              <span class="meta-value">{{ currentGame.releaseDate || '未知' }}</span>
            </div>
            <div class="meta-group">
              <span class="meta-label">开发商</span>
              <span class="meta-value">{{ currentGame.developer || '未知' }}</span>
            </div>
          </div>
        </div>

        <div class="detail-desc">
          <h3 class="desc-heading">游戏简介</h3>
          <div class="desc-content">
            {{ currentGame.description || '暂无详细游戏简介' }}
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import {getGameList} from '@/api/game';
import Pagination from '@/components/Pagination/index.vue';
import {reactive, toRefs, onMounted, watch, ref} from 'vue';
import {PageQuery} from '@/model';
import {Teleport} from 'vue';
import {ElMessage} from "element-plus";
import {formatDateTime} from "@/utils/date";
import steamIcon from '@/assets/icons/steam (1).svg';
import Waves from "@/components/Waves.vue";
const data = reactive({
  count: 0,
  queryParams: {
    current: 1,
    size: 5,
    sortType: 'play_time'
  } as PageQuery,

  gameList: [] as Array<{
    id: number;
    name: string;
    coverUrl: string;
    description?: string;
    isInstalled?: boolean;
    playTime?: string;
    lastPlayed?: string;
    tags?: string[];
    rating?: number;
    releaseDate?: string;
    developer?: string;
    screenshotUrl?: string[];
  }>
});

// 修改排序
const changeSort = (type: string) => {
  queryParams.value.sortType = type;
  fetchGames();
};
// 弹窗相关数据
const showDetail = ref(false);
const currentGame = ref<typeof data.gameList[0]>({} as any);
const sortType = ref('play_time');
const {count, queryParams, gameList} = toRefs(data);

function fetchGames() {
  getGameList(queryParams.value)
      .then(({data}) => {
        const realData = data.data.recordList;
        const totalCount = data.data.count;

        gameList.value = realData;
        count.value = totalCount;
      })
      .catch((error) => {
        console.error("获取游戏列表失败：", error);
        ElMessage.error("加载游戏失败，请重试")
      });
}

// 生命周期与监听
watch(() => queryParams.value.current, fetchGames, {immediate: false});
onMounted(fetchGames);

// 封面兜底
function handleCoverError(e: Event) {
  const img = e.target as HTMLImageElement;
  img.src = steamIcon;
  img.alt = '游戏封面占位';
}

// 弹窗逻辑
function openDetail(game: typeof data.gameList[0]) {
  currentGame.value = game;
  showDetail.value = true;
  document.body.style.overflow = 'hidden';
}

function closeDetail() {
  showDetail.value = false;
  currentGame.value = {} as any;
  document.body.style.overflow = '';
}
</script>

<style scoped lang="scss">

$steam-bg: #171a21; // Steam主背景色
$steam-card-bg: #2a475e; // 卡片背景色
$steam-highlight: #66c0f4; // Steam高亮蓝（边框/按钮）
$steam-text-primary: #ffffff; // 主文字色
$steam-text-secondary: #8f98a0; // 辅助文字色（元数据/简介）
$steam-shadow: 0 0 12px rgba(102, 192, 244, 0.4); // 高亮阴影

:deep(.el-select__wrapper) {
  width: 120px;
}

.count-badge {
  color: $steam-highlight;
  font-weight: 700;
}

.steam-single-list {
  display: flex;
  flex-direction: column;
  gap: 25px;
  margin: 20px 0;
}

.steam-single-card {
  background-color: $steam-card-bg;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid transparent;
  transition: transform 0.3s ease;
  opacity: 0;
  transform: translateY(20px);
  animation: fadeInUp 0.6s ease forwards;

  .card-img-area {
    height: 140px;
    display: flex;
    margin-left: 15px;
    gap: 12px;
    opacity: 0;
    transform: translateY(50%);
    transition: transform 0.5s ease;

    .screenshot-group {
      width: 100%;
      height: 100%;
      display: flex;
      gap: 8px;
    }

    .game-screenshot {
      width: 100%;
      height: 100%;
      border-radius: 4px;
      overflow: hidden;
      padding: 8px;
      box-shadow: 0 2px 4px rgba(104, 101, 101, 0.39);
      background-color: rgba(42, 71, 94, 0.83); // 背景色变化
      position: relative;
      transition: background-color 0.4s ease;

      img {
        object-fit: cover;
        transition: transform 0.3s ease;
      }

      &:hover {
        background-color: rgba(144, 189, 216, 0.77); // 背景色变化
        img {
          transform: scale(1.03); // 图片缩放
        }
      }
    }
  }

  &:hover {
    .card-info {
      display: none;
    }

    box-shadow: 0 4px 8px rgba(102, 192, 244, 0.3);
    border: 1px solid rgba(102, 192, 244, 0.6);
  }

  .card-content {
    display: flex;
    align-items: stretch;
    height: 100%;
  }

  .card-cover {
    position: relative;
    width: 220px;
    flex-shrink: 0;
    height: 140px;

    .cover-img {
      width: 100%;
      height: 100%; /* 修正：原105%可能溢出，改为100% */
      object-fit: cover;
    }

    &:hover .cover-img {
      filter: brightness(1.15);
    }

    .installed-tag {
      position: absolute;
      top: 8px;
      right: 8px;
      padding: 3px 10px;
      background-color: rgba(102, 192, 244, 0.9);
      color: $steam-bg;
      font-size: 0.75rem;
      font-weight: 600;
      border-radius: 3px;
      display: flex;
      align-items: center;
      gap: 4px;

      .tag-icon {
        font-size: 0.8rem;
      }
    }
  }


  .card-info {
    flex: 1;
    padding: 16px 20px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    position: absolute;
    left: 220px;
    width: calc(100% - 220px);
    height: 100%;


    .info-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 7px;

      .game-title {
        display: flex;
        align-items: center;
        font-size: 1.2rem;
        font-weight: 700;
        color: $steam-text-primary;
        margin: 0;
        max-width: 60%;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        gap: 8px;
      }

      .game-tags {
        display: flex;
        gap: 8px;

        .tag {
          padding: 2px 8px;
          background-color: rgba(143, 152, 160, 0.2);
          color: $steam-text-secondary;
          font-size: 0.7rem;
          border-radius: 2px;
          transition: all 0.2s ease;

          &:hover {
            background-color: rgba(102, 192, 244, 0.3);
            color: $steam-highlight;
          }
        }
      }
    }

    .game-meta {
      display: flex;
      gap: 20px;
      margin-bottom: 10px;

      .meta-item {
        display: flex;
        align-items: center;
        gap: 5px;
        font-size: 0.8rem;
        color: $steam-text-secondary;

        .meta-icon {
          font-size: 0.9rem;
          color: $steam-highlight;
        }
      }
    }

    .game-desc-preview {
      font-size: 0.85rem;
      color: $steam-text-secondary;
      margin: 0;
      line-height: 1.5;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;

      .view-detail {
        color: $steam-highlight;
        font-weight: 500;
        margin-left: 5px;
        transition: color 0.2s ease;
      }
    }
  }

  /* 新增：hover时显示card-info */
  &:hover .card-img-area {
    transform: translateY(0); /* 移回卡片内 */
    opacity: 1; /* 恢复透明度 */
  }

  &:hover .card-cover {
    height: 140px;
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.empty-single {
  padding: 80px 20px;
  display: flex;
  justify-content: center;

  .empty-wrapper {
    text-align: center;
    max-width: 400px;

    .empty-icon {
      width: 100px;
      height: 100px;
      margin-bottom: 20px;
      opacity: 0.4;
      filter: drop-shadow(0 0 10px rgba(102, 192, 244, 0.2));
    }

    .empty-text {
      font-size: 1.2rem;
      color: $steam-text-primary;
      margin-bottom: 8px;
    }

    .empty-subtext {
      font-size: 0.95rem;
      color: $steam-text-secondary;
    }
  }
}

.title-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  .view-tip {
    font-size: 0.8rem;
    color: $steam-text-secondary;
    font-weight: 400;
    margin-left: 8px;
  }

  .sort-dropdown {
    display: flex;
    align-items: center;
    gap: 8px;

    .sort-label {
      width: 40%;
      font-size: 0.90rem;
      color: $steam-text-secondary;
    }

    .sort-select {
      padding: 6px 12px;
      background-color: $steam-card-bg;
      color: $steam-text-primary;
      border: 1px solid rgba(102, 192, 244, 0.3);
      border-radius: 4px;
      font-size: 0.85rem;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        border-color: $steam-highlight;
        background-color: rgba(42, 71, 94, 0.8);
      }

      appearance: none;
      background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='6'%3E%3Cpath d='M0 0l6 6 6-6z' fill='%238f98a0'/%3E%3C/svg%3E");
      background-repeat: no-repeat;
      background-position: right 10px center;
    }
  }
}

.steam-detail-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 1000;

  .modal-overlay {
    width: 100%;
    height: 100%;
    background-color: rgba(15, 18, 25, 0.9);
    backdrop-filter: blur(4px);
  }

  .modal-content {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 92%;
    max-width: 1100px;
    max-height: 85vh;
    overflow-y: auto;
    background-color: $steam-bg;
    border-radius: 8px;
    box-shadow: 0 0 30px rgba(102, 192, 244, 0.2);
    padding: 30px;
    border: 1px solid rgba(102, 192, 244, 0.2);

    /* 滚动条美化 */
    &::-webkit-scrollbar {
      width: 8px;
    }

    &::-webkit-scrollbar-track {
      background: rgba(42, 71, 94, 0.5);
      border-radius: 4px;
    }

    &::-webkit-scrollbar-thumb {
      background: $steam-highlight;
      border-radius: 4px;

      &:hover {
        background: #87ceeb;
      }
    }

    .modal-close {
      position: absolute;
      top: 20px;
      right: 20px;
      width: 36px;
      height: 36px;
      border-radius: 50%;
      border: none;
      background-color: rgba(42, 71, 94, 0.8);
      color: $steam-text-secondary;
      font-size: 1.2rem;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.2s ease;

      &:hover {
        background-color: $steam-highlight;
        color: $steam-bg;
        transform: rotate(90deg);
      }
    }

    .detail-header {
      margin-bottom: 24px;

      .header-top {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 10px;

        .detail-title {
          font-size: 1.8rem;
          font-weight: 700;
          color: $steam-highlight;
          margin: 0;
          text-shadow: 0 0 10px rgba(102, 192, 244, 0.1);
        }

        .detail-rating {
          display: flex;
          align-items: center;
          gap: 6px;
          background-color: rgba(42, 71, 94, 0.5);
          padding: 6px 14px;
          border-radius: 20px;

          .rating-icon {
            color: #ffcc00;
            font-size: 1.1rem;
          }

          .rating-value {
            font-size: 1rem;
            color: $steam-text-primary;
            font-weight: 600;
          }
        }
      }

      .detail-tags {
        display: flex;
        gap: 10px;
        overflow-x: auto;
        padding-bottom: 8px;

        &::-webkit-scrollbar {
          height: 4px;
        }

        .tag {
          padding: 4px 12px;
          background-color: rgba(143, 152, 160, 0.15);
          color: $steam-text-secondary;
          font-size: 0.8rem;
          border-radius: 4px;
          white-space: nowrap;
          transition: all 0.2s ease;
        }
      }
    }

    .detail-middle {
      display: flex;
      gap: 28px;
      margin-bottom: 28px;
      align-items: center;

      .middle-cover {
        width: 320px;
        flex-shrink: 0;
        border-radius: 6px;
        overflow: hidden;
        box-shadow: 0 0 20px rgba(0, 0, 0, 0.5);

        .cover-large {
          width: 100%;
          height: auto;
          object-fit: cover;
        }
      }

      .middle-meta {
        flex: 1;
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
        gap: 16px;

        .meta-group {
          background-color: rgba(42, 71, 94, 0.3);
          padding: 12px 16px;
          border-radius: 6px;

          .meta-label {
            font-size: 0.8rem;
            color: $steam-text-secondary;
            display: block;
            margin-bottom: 4px;
          }

          .meta-value {
            font-size: 0.95rem;
            color: $steam-text-primary;
            font-weight: 500;
          }
        }
      }
    }

    /* 弹窗底部：简介 */
    .detail-desc {
      background-color: rgba(42, 71, 94, 0.2);
      padding: 20px;
      border-radius: 6px;
      border-left: 3px solid $steam-highlight;

      .desc-heading {
        font-size: 1.1rem;
        font-weight: 600;
        color: $steam-highlight;
        margin-top: 0;
        margin-bottom: 12px;
      }

      .desc-content {
        font-size: 0.95rem;
        color: $steam-text-secondary;
        line-height: 1.7;
        white-space: pre-line;
      }
    }
  }
}

@media (max-width: 768px) {
  .card-content {
    flex-direction: column !important;
    height: 100% !important; /* 占满卡片高度 */
    overflow: hidden !important;
  }

  .card-cover {
    width: 100% !important;
    height: 140px !important; /* 固定封面高度 */
    flex-shrink: 0 !important; /* 防止封面被压缩 */
  }

  .steam-single-card {
    height: 140px !important;
    transition: all 0.4s ease !important;

    &:hover {
      height: auto !important;
      min-height: 280px !important;
    }

    .card-info {
      position: relative !important;
      left: 0 !important;
      width: 100% !important;
      height: auto !important;
      padding: 12px 16px !important;
      margin-top: 0 !important;
      transform: translateY(-100%);
      opacity: 0;
      transition: all 0.4s ease !important;

      .game-title {
        font-size: 1rem !important;
        max-width: 80% !important;
      }

      .game-meta {
        flex-wrap: wrap !important;
        gap: 8px 15px !important;
        margin-bottom: 8px !important;
      }

      .game-desc-preview {
        font-size: 0.8rem !important;
        -webkit-line-clamp: 3 !important;
      }
    }

    &:hover .card-info {
      transform: translateY(0) !important;
      opacity: 1 !important;
    }
  }

  .title-bar {
    flex-direction: column !important;
    align-items: flex-start !important;
    gap: 10px;
  }

  .detail-title {
    font-size: 1.5rem !important;
  }

  .middle-meta {
    grid-template-columns: 1fr !important;
  }
}
</style>