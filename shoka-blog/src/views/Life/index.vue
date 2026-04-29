<template>
  <div class="page-header">
    <h1 class="page-title">路线</h1>
    <img
      class="page-cover"
      src="https://wangyoupeng-penghong.oss-cn-beijing.aliyuncs.com/avatar/wallhaven-q21drl_2560x1440.png"
      alt=""
    />
    <Waves />
  </div>

  <div class="bg">
    <div :class="['life-surface', isDarkTheme ? 'dark-theme' : 'light-theme']">
      <div class="container">
        <section class="route-dashboard">
          <div class="route-copy">
            <p class="route-kicker">Growth Route</p>
            <h2>把走过的阶段连成一张可以回放的地图</h2>
            <p>每个节点都是一次学习、项目或复盘。你可以按阶段筛选，也可以顺着路线看完整成长轨迹。</p>
          </div>

          <div class="route-stats" aria-label="路线统计">
            <div class="stat-item">
              <span>{{ routeStats.total }}</span>
              <small>节点</small>
            </div>
            <div class="stat-item">
              <span>{{ routeStats.done }}</span>
              <small>完成</small>
            </div>
            <div class="stat-item">
              <span>{{ routeStats.doing }}</span>
              <small>进行中</small>
            </div>
            <div class="stat-item">
              <span>{{ routeStats.linked }}</span>
              <small>关联</small>
            </div>
          </div>
        </section>

        <section class="route-toolbar">
          <div class="playback-controls">
            <el-button type="primary" class="toolbar-btn btn-play" @click="togglePlayback">
              <el-icon><VideoPause v-if="isPlaying" /><VideoPlay v-else /></el-icon>
              {{ isPlaying ? '暂停路线' : '播放路线' }}
            </el-button>
            <el-button class="toolbar-btn" @click="goPrevNode">
              <el-icon><ArrowLeft /></el-icon>
              上一步
            </el-button>
            <el-button class="toolbar-btn" @click="goNextNode">
              <el-icon><ArrowRight /></el-icon>
              下一步
            </el-button>
            <el-button class="toolbar-btn" @click="resetRouteView">
              <el-icon><RefreshRight /></el-icon>
              重置
            </el-button>
          </div>

          <div class="route-filters">
            <el-radio-group v-model="activeFilter" size="large" @change="handleFilterChange">
              <el-radio-button v-for="item in routeFilters" :key="item.value" :value="item.value">
                {{ item.label }}
              </el-radio-button>
            </el-radio-group>
            <el-button class="toolbar-btn" @click="toggleTheme">
              <el-icon><Moon v-if="!isDarkTheme" /><Sunny v-else /></el-icon>
              {{ isDarkTheme ? '白天' : '黑夜' }}
            </el-button>
            <el-button class="toolbar-btn btn-add" @click="showAddNodeDialog">
              <el-icon><Plus /></el-icon>
              添加节点
            </el-button>
          </div>
        </section>

        <section class="route-stage" v-if="currentRouteNode">
          <div>
            <span class="stage-status" :class="`status-${currentRouteNode.data?.status}`">
              {{ getStatusText(currentRouteNode.data?.status) }}
            </span>
            <strong>{{ currentRouteNode.data?.label }}</strong>
          </div>
          <p>{{ currentRouteNode.data?.desc }}</p>
        </section>

        <VueFlow
          fit-view-on-init
          class="my-flow"
          :class="isDarkTheme ? 'dark-flow' : ''"
          v-model="elements"
          @node-click="handleNodeClick"
          @node-double-click="handleNodeDoubleClick"
          ref="vueFlowRef"
        >
          <Background type="dots" gap="60" size="2" :color="isDarkTheme ? '#444' : '#d8dee8'" />
          <Panel :position="PanelPosition.TopRight" class="custom-panel" :class="isDarkTheme ? 'dark-panel' : ''">
            <label for="ishidden" class="hidden-label">
              <span class="label-text" :class="isDarkTheme ? 'text-white' : ''">隐藏节点</span>
              <input id="ishidden" v-model="isHidden" type="checkbox" class="custom-checkbox" />
              <span class="checkbox-icon"></span>
            </label>
          </Panel>
          <Controls position="bottom-center" class="custom-controls" :class="isDarkTheme ? 'dark-controls' : ''" />
        </VueFlow>

        <el-dialog
          v-model="dialogVisible"
          :title="selectedNode?.data?.label || '路线节点'"
          width="36%"
          class="custom-dialog"
          :class="isDarkTheme ? 'dark-dialog' : ''"
          center
        >
          <div v-if="selectedNode" class="dialog-content">
            <div class="stage-icon" :class="selectedNode.class">
              <el-icon>
                <component :is="getStageIcon(selectedNode.class || '')" />
              </el-icon>
            </div>
            <div class="stage-info">
              <div class="detail-meta">
                <span>{{ selectedNode.data?.time }}</span>
                <span class="stage-status" :class="`status-${selectedNode.data?.status}`">
                  {{ getStatusText(selectedNode.data?.status) }}
                </span>
              </div>
              <p class="stage-desc">{{ selectedNode.data?.desc }}</p>
              <div class="tag-row" v-if="selectedNode.data?.tags.length">
                <span class="keyPoint-badge" v-for="tag in selectedNode.data.tags" :key="tag">{{ tag }}</span>
              </div>
              <div class="key-points" v-if="selectedNode.data?.keyPoints.length">
                <h4>关键收获</h4>
                <ul>
                  <li v-for="item in selectedNode.data.keyPoints" :key="item">{{ item }}</li>
                </ul>
              </div>
              <div class="related-links" v-if="selectedNode.data?.links.length">
                <h4>关联内容</h4>
                <a v-for="link in selectedNode.data.links" :key="link.title" :href="link.url">
                  {{ link.title }}
                </a>
              </div>
            </div>
          </div>
        </el-dialog>

        <Teleport to="body">
          <transition name="route-modal">
            <div
              v-if="addNodeDialogVisible"
              class="route-node-modal"
              :class="{ 'route-node-modal--dark': isDarkTheme }"
              @click.self="closeAddNodeDialog"
            >
              <section class="route-node-panel" role="dialog" aria-modal="true" aria-labelledby="add-node-title">
                <header class="route-node-panel__header">
                  <div>
                    <p class="route-node-panel__kicker">Route Editor</p>
                    <h3 id="add-node-title">添加路线节点</h3>
                    <span>把新的阶段接到现有路线后面，形成一段可以回放的成长记录。</span>
                  </div>
                  <button class="route-node-close" type="button" aria-label="关闭添加节点弹窗" @click="closeAddNodeDialog">
                    <el-icon><Close /></el-icon>
                  </button>
                </header>

                <div class="route-node-panel__body">
                  <label class="route-field">
                    <span>节点名称</span>
                    <input
                      v-model.trim="addNodeForm.label"
                      class="route-input"
                      type="text"
                      placeholder="例如：Vue3 项目实践"
                      @input="clearAddNodeError('label')"
                    />
                    <small v-if="addNodeErrors.label">{{ addNodeErrors.label }}</small>
                  </label>

                  <div class="route-field">
                    <span>阶段类型</span>
                    <div class="stage-picker">
                      <button
                        v-for="stage in stageOptions"
                        :key="stage.value"
                        type="button"
                        class="stage-choice"
                        :class="[`stage-choice--${stage.tone}`, { active: addNodeForm.stageClass === stage.value }]"
                        @click="selectAddNodeStage(stage.value)"
                      >
                        <b>{{ stage.label }}</b>
                        <em>{{ stage.desc }}</em>
                      </button>
                    </div>
                    <small v-if="addNodeErrors.stageClass">{{ addNodeErrors.stageClass }}</small>
                  </div>

                  <label class="route-field">
                    <span>阶段描述</span>
                    <textarea
                      v-model.trim="addNodeForm.desc"
                      class="route-input route-textarea"
                      placeholder="写一句这个阶段最重要的复盘"
                      rows="3"
                      @input="clearAddNodeError('desc')"
                    ></textarea>
                    <small v-if="addNodeErrors.desc">{{ addNodeErrors.desc }}</small>
                  </label>

                  <label class="route-field">
                    <span>关键事项</span>
                    <textarea
                      v-model.trim="addNodeForm.keyPoints"
                      class="route-input route-textarea"
                      placeholder="用逗号分隔，例如：Vue3,组件设计,项目部署"
                      rows="3"
                      @input="clearAddNodeError('keyPoints')"
                    ></textarea>
                    <small v-if="addNodeErrors.keyPoints">{{ addNodeErrors.keyPoints }}</small>
                  </label>

                  <label class="route-field">
                    <span>连接到</span>
                    <select
                      v-model="addNodeForm.parentNodeId"
                      class="route-input route-select"
                      @change="clearAddNodeError('parentNodeId')"
                    >
                      <option value="" disabled>选择前置节点</option>
                      <option v-for="node in sourceNodes" :key="node.id" :value="node.id">
                        {{ node.data?.label || `节点${node.id}` }}
                      </option>
                    </select>
                    <small v-if="addNodeErrors.parentNodeId">{{ addNodeErrors.parentNodeId }}</small>
                  </label>
                </div>

                <footer class="route-node-panel__footer">
                  <button class="route-secondary-btn" type="button" @click="resetAddNodeForm">重置</button>
                  <button class="route-primary-btn" type="button" @click="submitAddNode">添加节点</button>
                </footer>
              </section>
            </div>
          </transition>
        </Teleport>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup name="LifePathFlow">
import '@vue-flow/core/dist/style.css';
import '@vue-flow/core/dist/theme-default.css';
import { Background, Panel, PanelPosition, Controls } from '@vue-flow/additional-components';
import { VueFlow, useVueFlow } from '@vue-flow/core';
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { ElMessage } from 'element-plus/dist/index.full.mjs';
import { ArrowLeft, ArrowRight, Briefcase, Check, Close, Coffee, Moon, Plus, RefreshRight, Sunny, VideoPause, VideoPlay } from "@element-plus/icons-vue";
import { addFlowElement, getFlowList } from "@/api/life";
import Waves from "@/components/Waves/index.vue";
import {
  enrichRouteNodes,
  getFilteredRouteElements,
  getPlaybackSequence,
  getRouteStats,
  RouteElement,
  RouteFilter,
  RouteStatus,
} from "@/views/Life/routeModel";

const isDarkTheme = ref(false);
const elements = ref<RouteElement[]>([]);
const sourceElements = ref<RouteElement[]>([]);
const isHidden = ref(false);
const dialogVisible = ref(false);
const selectedNode = ref<RouteElement | null>(null);
const vueFlowRef = ref<any | null>(null);
const addNodeDialogVisible = ref(false);
const activeFilter = ref<RouteFilter>("all");
const activeRouteNodeId = ref("");
const activeRouteEdgeId = ref("");
const isPlaying = ref(false);
const playbackTimer = ref<number | null>(null);
const { onPaneReady, setTransform, nodes, edges } = useVueFlow();

const routeFilters: { label: string; value: RouteFilter }[] = [
  { label: "全部", value: "all" },
  { label: "基础", value: "stage-birth" },
  { label: "进阶", value: "stage-study" },
  { label: "实战", value: "stage-work" },
  { label: "沉淀", value: "stage-retire" },
];

const stageOptions = [
  { label: "基础", value: "stage-birth", tone: "birth", desc: "补齐概念与语法" },
  { label: "进阶", value: "stage-study", tone: "study", desc: "形成稳定能力" },
  { label: "实战", value: "stage-work", tone: "work", desc: "用项目验证想法" },
  { label: "沉淀", value: "stage-retire", tone: "retire", desc: "复盘、整理、输出" },
];

const addNodeForm = ref({
  label: '',
  stageClass: 'stage-work',
  desc: '',
  parentNodeId: '',
  keyPoints: '',
});

const addNodeErrors = ref({
  label: '',
  stageClass: '',
  desc: '',
  keyPoints: '',
  parentNodeId: '',
});

const sourceNodes = computed(() => sourceElements.value.filter((item) => item.elemType === "node"));
const routeStats = computed(() => getRouteStats(sourceElements.value));
const playbackSequence = computed(() => getPlaybackSequence(getFilteredRouteElements(sourceElements.value, activeFilter.value)));
const currentRouteNode = computed(() => sourceNodes.value.find((item) => item.id === activeRouteNodeId.value));

watch(isHidden, () => {
  nodes.value.forEach((n) => (n.hidden = isHidden.value));
  edges.value.forEach((e) => (e.hidden = isHidden.value));
});

onPaneReady(({ fitView }) => {
  nextTick(() => {
    setTransform({ x: 210, y: 180, zoom: 1 });
    fitView({ padding: 60 });
  });
});

const decorateVisibleElements = (visibleElements: RouteElement[]) => {
  return visibleElements.map((item) => {
    const extraClasses: string[] = [];
    if (item.elemType === "node" && activeRouteNodeId.value) {
      extraClasses.push(item.id === activeRouteNodeId.value ? "route-node-active" : "route-node-muted");
    }
    if (item.elemType === "edge" && activeRouteEdgeId.value) {
      extraClasses.push(item.id === activeRouteEdgeId.value ? "route-edge-active" : "route-edge-muted");
    }
    return {
      ...item,
      class: [item.class, ...extraClasses].filter(Boolean).join(" "),
    };
  });
};

const refreshVisibleElements = () => {
  elements.value = decorateVisibleElements(getFilteredRouteElements(sourceElements.value, activeFilter.value));
};

const setActiveRouteNode = (nodeId: string) => {
  const sequence = playbackSequence.value;
  const currentIndex = sequence.findIndex((node) => node.id === nodeId);
  const previousNode = sequence[currentIndex - 1];
  const activeEdge = previousNode
    ? sourceElements.value.find((item) => item.elemType === "edge" && item.source === previousNode.id && item.target === nodeId)
    : null;
  activeRouteNodeId.value = nodeId;
  activeRouteEdgeId.value = activeEdge?.id || "";
  selectedNode.value = sourceNodes.value.find((item) => item.id === nodeId) || null;
  refreshVisibleElements();
};

const clearPlaybackTimer = () => {
  if (playbackTimer.value) {
    window.clearInterval(playbackTimer.value);
    playbackTimer.value = null;
  }
};

const togglePlayback = () => {
  if (isPlaying.value) {
    clearPlaybackTimer();
    isPlaying.value = false;
    return;
  }

  const sequence = playbackSequence.value;
  if (!sequence.length) {
    ElMessage.warning("当前筛选下没有可播放的路线节点");
    return;
  }

  let index = Math.max(sequence.findIndex((node) => node.id === activeRouteNodeId.value), 0);
  setActiveRouteNode(sequence[index].id);
  isPlaying.value = true;
  playbackTimer.value = window.setInterval(() => {
    index += 1;
    if (index >= sequence.length) {
      clearPlaybackTimer();
      isPlaying.value = false;
      return;
    }
    setActiveRouteNode(sequence[index].id);
  }, 1600);
};

const goPrevNode = () => {
  const sequence = playbackSequence.value;
  if (!sequence.length) return;
  const currentIndex = sequence.findIndex((node) => node.id === activeRouteNodeId.value);
  const nextIndex = currentIndex <= 0 ? 0 : currentIndex - 1;
  setActiveRouteNode(sequence[nextIndex].id);
};

const goNextNode = () => {
  const sequence = playbackSequence.value;
  if (!sequence.length) return;
  const currentIndex = sequence.findIndex((node) => node.id === activeRouteNodeId.value);
  const nextIndex = currentIndex < 0 ? 0 : Math.min(currentIndex + 1, sequence.length - 1);
  setActiveRouteNode(sequence[nextIndex].id);
};

const handleFilterChange = () => {
  clearPlaybackTimer();
  isPlaying.value = false;
  const firstNode = playbackSequence.value[0];
  activeRouteNodeId.value = "";
  activeRouteEdgeId.value = "";
  selectedNode.value = null;
  refreshVisibleElements();
  if (firstNode) setActiveRouteNode(firstNode.id);
};

const handleNodeClick = ({ node }: { node: RouteElement }) => {
  setActiveRouteNode(node.id);
};

const handleNodeDoubleClick = ({ node }: { node: RouteElement }) => {
  setActiveRouteNode(node.id);
  dialogVisible.value = true;
};

const toggleTheme = () => {
  isDarkTheme.value = !isDarkTheme.value;
  ElMessage.info(`已切换至${isDarkTheme.value ? '黑夜' : '白天'}模式`);
};

const resetRouteView = () => {
  clearPlaybackTimer();
  isPlaying.value = false;
  activeRouteNodeId.value = "";
  activeRouteEdgeId.value = "";
  selectedNode.value = null;
  activeFilter.value = "all";
  refreshVisibleElements();
  nextTick(() => setTransform({ x: 210, y: 180, zoom: 1 }));
  ElMessage.success({ message: '路线视图已重置', duration: 1500 });
};

const getStatusText = (status?: RouteStatus) => {
  if (status === "done") return "已完成";
  if (status === "doing") return "进行中";
  return "计划中";
};

const getStageIcon = (className: string) => {
  if (className.includes('birth')) return Check;
  if (className.includes('study')) return RefreshRight;
  if (className.includes('work')) return Briefcase;
  if (className.includes('retire')) return Coffee;
  return Check;
};

const getEdgeColorByStage = (stageClass: string) => {
  if (stageClass.includes('birth')) return '#409EFF';
  if (stageClass.includes('study')) return '#67C23A';
  if (stageClass.includes('work')) return '#FAAD14';
  if (stageClass.includes('retire')) return '#9254DE';
  return '#409EFF';
};

const showAddNodeDialog = () => {
  addNodeDialogVisible.value = true;
  resetAddNodeForm();
};

const closeAddNodeDialog = () => {
  addNodeDialogVisible.value = false;
};

const clearAddNodeError = (field: keyof typeof addNodeErrors.value) => {
  addNodeErrors.value[field] = '';
};

const selectAddNodeStage = (stageClass: string) => {
  addNodeForm.value.stageClass = stageClass;
  clearAddNodeError('stageClass');
};

const resetAddNodeForm = () => {
  addNodeForm.value = {
    label: '',
    stageClass: 'stage-work',
    desc: '',
    parentNodeId: sourceNodes.value.at(-1)?.id || '',
    keyPoints: '',
  };
  addNodeErrors.value = {
    label: '',
    stageClass: '',
    desc: '',
    keyPoints: '',
    parentNodeId: '',
  };
};

const validateAddNodeForm = () => {
  const errors = {
    label: addNodeForm.value.label ? '' : '请输入节点名称',
    stageClass: addNodeForm.value.stageClass ? '' : '请选择阶段类型',
    desc: addNodeForm.value.desc ? '' : '请输入阶段描述',
    keyPoints: addNodeForm.value.keyPoints ? '' : '请输入关键事项',
    parentNodeId: addNodeForm.value.parentNodeId ? '' : '请选择要连接的前置节点',
  };
  addNodeErrors.value = errors;
  return !Object.values(errors).some(Boolean);
};

const submitAddNode = async () => {
  if (!validateAddNodeForm()) return;

  const existingNodeIds = sourceNodes.value.map((node) => Number(node.id)).filter(Number.isFinite);
  const maxId = existingNodeIds.length ? Math.max(...existingNodeIds) : 0;
  const newNodeId = (maxId + 1).toString();
  const parentNode = sourceNodes.value.find((node) => node.id === addNodeForm.value.parentNodeId);
  if (!parentNode) {
    ElMessage.error('未找到选中的前置节点');
    return;
  }

  const newNodePosition = {
    x: parentNode.position?.x || 0,
    y: (parentNode.position?.y || 0) + 120,
  };
  const newKeyPoints = addNodeForm.value.keyPoints.split(',').map((item) => item.trim()).filter(Boolean);
  const newNode = {
    id: newNodeId,
    elemType: 'node' as const,
    label: addNodeForm.value.label,
    positionX: newNodePosition.x,
    positionY: newNodePosition.y,
    elemClass: addNodeForm.value.stageClass,
    description: addNodeForm.value.desc,
    keyPoints: newKeyPoints,
  };
  const newEdge = {
    id: `e${addNodeForm.value.parentNodeId}-${newNodeId}`,
    elemType: 'edge' as const,
    sourceId: addNodeForm.value.parentNodeId,
    targetId: newNodeId,
    animated: true,
    color: getEdgeColorByStage(addNodeForm.value.stageClass),
    elemClass: `edge-${addNodeForm.value.stageClass.split('-')[1]}`,
  };

  await addFlowElement(newNode);
  await addFlowElement(newEdge);
  sourceElements.value = [...sourceElements.value, ...enrichRouteNodes([newNode, newEdge])];
  refreshVisibleElements();
  setActiveRouteNode(newNodeId);
  ElMessage.success(`成功添加节点：${addNodeForm.value.label}`);
  addNodeDialogVisible.value = false;
};

onMounted(async () => {
  const { data } = await getFlowList();
  sourceElements.value = enrichRouteNodes(data as any);
  refreshVisibleElements();
  const firstNode = playbackSequence.value[0];
  if (firstNode) setActiveRouteNode(firstNode.id);
});

onBeforeUnmount(() => {
  clearPlaybackTimer();
});
</script>

<style scoped>
@import "@/views/Life/css/flow.scss";
@import "@/views/Life/css/base.scss";

.life-surface {
  min-height: 45rem;
  padding: 28px 0 9.5rem;
  background: linear-gradient(180deg, rgba(247, 250, 252, 0.96), rgba(238, 244, 249, 0.88));
}

.dark-theme {
  background: linear-gradient(180deg, #171a20, #20242c);
  color: #fff;
}

.route-dashboard {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 24px;
  align-items: stretch;
  margin-bottom: 18px;
}

.route-copy {
  padding: 24px;
  border: 1px solid rgba(64, 158, 255, 0.14);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 12px 34px rgba(31, 45, 61, 0.08);
}

.dark-theme .route-copy,
.dark-theme .route-stats,
.dark-theme .route-toolbar,
.dark-theme .route-stage {
  background: rgba(39, 44, 54, 0.88);
  border-color: rgba(255, 255, 255, 0.08);
}

.route-kicker {
  margin-bottom: 8px;
  color: #409eff;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: uppercase;
}

.route-copy h2 {
  margin: 0 0 10px;
  color: #1f2d3d;
  font-size: 28px;
  line-height: 1.25;
}

.dark-theme .route-copy h2 {
  color: #f5f7fa;
}

.route-copy p:last-child {
  color: #5f6f86;
  line-height: 1.7;
}

.dark-theme .route-copy p:last-child {
  color: #c8d0da;
}

.route-stats {
  display: grid;
  grid-template-columns: repeat(4, 104px);
  gap: 10px;
  padding: 16px;
  border: 1px solid rgba(31, 45, 61, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 12px 34px rgba(31, 45, 61, 0.08);
}

.stat-item {
  display: grid;
  place-items: center;
  min-height: 88px;
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(64, 158, 255, 0.11), rgba(19, 194, 194, 0.08));
}

.stat-item span {
  color: #1f2d3d;
  font-size: 30px;
  font-weight: 800;
}

.dark-theme .stat-item span {
  color: #fff;
}

.stat-item small {
  color: #6b778c;
}

.route-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-bottom: 14px;
  padding: 14px;
  border: 1px solid rgba(31, 45, 61, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.84);
}

.playback-controls,
.route-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.toolbar-btn {
  border-radius: 8px !important;
  min-width: 96px;
  padding: 10px 16px !important;
  justify-content: center;
}

.btn-play,
.btn-add {
  border: none !important;
  background: #1f7a8c !important;
}

:deep(.route-filters .el-radio-button__inner) {
  min-width: 76px;
  padding: 10px 18px;
}

:deep(.playback-controls .el-button),
:deep(.route-filters .el-button) {
  min-height: 40px;
}

.route-stage {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
  margin-bottom: 14px;
  padding: 14px 16px;
  border: 1px solid rgba(31, 45, 61, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.76);
}

.route-stage strong {
  margin-left: 8px;
}

.route-stage p {
  margin: 0;
  color: #5f6f86;
}

.dark-theme .route-stage p {
  color: #c8d0da;
}

.stage-status {
  display: inline-flex;
  align-items: center;
  padding: 3px 8px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 700;
}

.status-done {
  color: #1f7a4d;
  background: rgba(103, 194, 58, 0.14);
}

.status-doing {
  color: #9a6400;
  background: rgba(250, 173, 20, 0.18);
}

.status-planned {
  color: #6b42b8;
  background: rgba(146, 84, 222, 0.16);
}

:deep(.route-node-active) {
  box-shadow: 0 0 0 4px rgba(19, 194, 194, 0.2), 0 14px 28px rgba(31, 45, 61, 0.18) !important;
  transform: scale(1.04);
}

:deep(.route-node-muted) {
  opacity: 0.46;
}

:deep(.route-edge-active path) {
  stroke-width: 4 !important;
  stroke: #1f7a8c !important;
}

:deep(.route-edge-muted path) {
  opacity: 0.28;
}

.dark-flow {
  --vf-background-color: #1e1e1e;
  --vf-node-background: #333;
  --vf-node-border-color: #444;
  --vf-node-text-color: #fff;
  --vf-edge-color: #666;
}

.dark-panel,
.dark-controls {
  background-color: #333;
  border-color: #444;
}

.dark-dialog {
  background-color: #333;
  color: #fff;
}

.detail-meta,
.tag-row,
.related-links {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.key-points,
.related-links {
  margin-top: 16px;
}

.key-points h4,
.related-links h4 {
  width: 100%;
  margin: 0;
  color: #1f2d3d;
  font-size: 14px;
}

.dark-theme .key-points h4,
.dark-theme .related-links h4 {
  color: #f5f7fa;
}

.key-points ul {
  margin: 8px 0 0;
  padding-left: 18px;
  color: #4f5f73;
  line-height: 1.8;
}

.related-links a {
  display: inline-flex;
  padding: 6px 10px;
  border-radius: 8px;
  color: #1f7a8c;
  background: rgba(31, 122, 140, 0.1);
  text-decoration: none;
}

.route-node-modal {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.36);
  backdrop-filter: blur(10px);
}

.route-node-panel {
  width: min(720px, 100%);
  max-height: min(86vh, 820px);
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  overflow: hidden;
  border: 1px solid rgba(31, 122, 140, 0.18);
  border-radius: 12px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(246, 251, 252, 0.94)),
    radial-gradient(circle at 12% 0%, rgba(31, 122, 140, 0.14), transparent 34%);
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.28);
}

.route-node-panel__header {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding: 26px 28px 20px;
  border-bottom: 1px solid rgba(31, 45, 61, 0.08);
}

.route-node-panel__kicker {
  margin: 0 0 7px;
  color: #1f7a8c;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}

.route-node-panel__header h3 {
  margin: 0;
  color: #1f2d3d;
  font-size: 25px;
  line-height: 1.2;
}

.route-node-panel__header span {
  display: block;
  margin-top: 8px;
  color: #64748b;
  line-height: 1.7;
}

.route-node-close {
  width: 38px;
  height: 38px;
  display: inline-grid;
  place-items: center;
  flex: 0 0 auto;
  border: 1px solid rgba(31, 45, 61, 0.1);
  border-radius: 10px;
  color: #475569;
  background: rgba(255, 255, 255, 0.78);
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, color 0.2s ease;
}

.route-node-close:hover {
  transform: translateY(-1px);
  border-color: rgba(31, 122, 140, 0.28);
  color: #1f7a8c;
}

.route-node-panel__body {
  display: grid;
  gap: 18px;
  padding: 22px 28px 24px;
  overflow: auto;
}

.route-field {
  display: grid;
  gap: 8px;
}

.route-field > span {
  color: #334155;
  font-size: 14px;
  font-weight: 700;
}

.route-field small {
  color: #d14343;
  font-size: 12px;
}

.route-input {
  width: 100%;
  min-height: 44px;
  padding: 11px 13px;
  border: 1px solid rgba(100, 116, 139, 0.24);
  border-radius: 9px;
  outline: none;
  color: #1f2d3d;
  background: rgba(255, 255, 255, 0.88);
  transition: border-color 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.route-input:focus {
  border-color: #1f7a8c;
  box-shadow: 0 0 0 4px rgba(31, 122, 140, 0.11);
  background: #fff;
}

.route-textarea {
  resize: vertical;
  line-height: 1.7;
}

.route-select {
  appearance: none;
  background-image:
    linear-gradient(45deg, transparent 50%, #64748b 50%),
    linear-gradient(135deg, #64748b 50%, transparent 50%);
  background-position:
    calc(100% - 18px) 19px,
    calc(100% - 13px) 19px;
  background-size: 5px 5px, 5px 5px;
  background-repeat: no-repeat;
}

.stage-picker {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.stage-choice {
  min-height: 78px;
  padding: 12px;
  border: 1px solid rgba(100, 116, 139, 0.18);
  border-radius: 10px;
  text-align: left;
  background: rgba(255, 255, 255, 0.72);
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.stage-choice b,
.stage-choice em {
  display: block;
  font-style: normal;
}

.stage-choice b {
  color: #1f2d3d;
  font-size: 15px;
}

.stage-choice em {
  margin-top: 5px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.45;
}

.stage-choice:hover,
.stage-choice.active {
  transform: translateY(-2px);
  box-shadow: 0 12px 26px rgba(15, 23, 42, 0.1);
}

.stage-choice--birth.active {
  border-color: #409eff;
  background: #eef7ff;
}

.stage-choice--study.active {
  border-color: #67c23a;
  background: #f0fbec;
}

.stage-choice--work.active {
  border-color: #faad14;
  background: #fff8e9;
}

.stage-choice--retire.active {
  border-color: #9254de;
  background: #f8f0ff;
}

.route-node-panel__footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 18px 28px 24px;
  border-top: 1px solid rgba(31, 45, 61, 0.08);
}

.route-primary-btn,
.route-secondary-btn {
  min-width: 112px;
  min-height: 42px;
  padding: 10px 18px;
  border-radius: 9px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.route-primary-btn {
  border: 1px solid #1f7a8c;
  color: #fff;
  background: #1f7a8c;
  box-shadow: 0 12px 24px rgba(31, 122, 140, 0.22);
}

.route-secondary-btn {
  border: 1px solid rgba(100, 116, 139, 0.22);
  color: #475569;
  background: rgba(255, 255, 255, 0.86);
}

.route-primary-btn:hover,
.route-secondary-btn:hover {
  transform: translateY(-1px);
}

.route-node-modal--dark {
  background: rgba(3, 7, 18, 0.58);
}

.route-node-modal--dark .route-node-panel {
  border-color: rgba(148, 163, 184, 0.16);
  background:
    linear-gradient(135deg, rgba(32, 36, 44, 0.98), rgba(23, 26, 32, 0.96)),
    radial-gradient(circle at 12% 0%, rgba(31, 122, 140, 0.22), transparent 36%);
}

.route-node-modal--dark .route-node-panel__header,
.route-node-modal--dark .route-node-panel__footer {
  border-color: rgba(255, 255, 255, 0.08);
}

.route-node-modal--dark .route-node-panel__header h3,
.route-node-modal--dark .route-field > span,
.route-node-modal--dark .stage-choice b {
  color: #f8fafc;
}

.route-node-modal--dark .route-node-panel__header span,
.route-node-modal--dark .stage-choice em {
  color: #cbd5e1;
}

.route-node-modal--dark .route-input,
.route-node-modal--dark .stage-choice,
.route-node-modal--dark .route-node-close,
.route-node-modal--dark .route-secondary-btn {
  color: #f8fafc;
  border-color: rgba(148, 163, 184, 0.2);
  background: rgba(15, 23, 42, 0.62);
}

.route-node-modal--dark .route-input:focus {
  background: rgba(15, 23, 42, 0.82);
}

.route-node-modal--dark .stage-choice--birth.active {
  background: rgba(64, 158, 255, 0.16);
}

.route-node-modal--dark .stage-choice--study.active {
  background: rgba(103, 194, 58, 0.14);
}

.route-node-modal--dark .stage-choice--work.active {
  background: rgba(250, 173, 20, 0.15);
}

.route-node-modal--dark .stage-choice--retire.active {
  background: rgba(146, 84, 222, 0.18);
}

.route-modal-enter-active,
.route-modal-leave-active {
  transition: opacity 0.2s ease;
}

.route-modal-enter-active .route-node-panel,
.route-modal-leave-active .route-node-panel {
  transition: transform 0.2s ease, opacity 0.2s ease;
}

.route-modal-enter-from,
.route-modal-leave-to {
  opacity: 0;
}

.route-modal-enter-from .route-node-panel,
.route-modal-leave-to .route-node-panel {
  opacity: 0;
  transform: translateY(12px) scale(0.98);
}

@media (max-width: 980px) {
  .route-dashboard {
    grid-template-columns: 1fr;
  }

  .route-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .route-toolbar,
  .route-stage {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 768px) {
  .route-copy h2 {
    font-size: 22px;
  }

  .my-flow {
    height: 460px;
  }

  :deep(.el-dialog) {
    width: 92% !important;
  }

  .route-node-modal {
    padding: 14px;
  }

  .route-node-panel__header,
  .route-node-panel__body,
  .route-node-panel__footer {
    padding-left: 18px;
    padding-right: 18px;
  }

  .stage-picker {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .route-node-panel__footer {
    flex-direction: column-reverse;
  }

  .route-primary-btn,
  .route-secondary-btn {
    width: 100%;
  }
}
</style>
