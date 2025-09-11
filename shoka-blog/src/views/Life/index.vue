<template>
  <div class="page-header">
    <h1 class="page-title">关于</h1>
    <img class="page-cover"
         src="https://wangyoupeng-penghong.oss-cn-beijing.aliyuncs.com/avatar/wallhaven-q21drl_2560x1440.png"
         alt=""/>
    <Waves></Waves>
  </div>
  <div class="bg">
    <div :class="isDarkTheme ? 'dark-theme' : 'light-theme'">
      <div class="container">
        <!-- 分割线：自定义颜色与文字样式 -->
        <el-divider
            content-position="left"
            class="custom-divider"
        >
        </el-divider>
        <el-row class="btn-group mb-4" :gutter="16">
          <el-col :md="2">
            <el-button
                type="primary"
                @click="resetTransform"
                class="custom-btn"
                :class="{ 'btn-birth': true }"
            >
              <i class="el-icon-refresh-right mr-2"></i>重置视图
            </el-button>
          </el-col>
          <el-col :md="2">
            <el-button
                type="primary"
                @click="updatePos"
                class="custom-btn"
                :class="{ 'btn-study': true }"
            >
              <i class="el-icon-random mr-2"></i>随机位置
            </el-button>
          </el-col>
          <el-col :md="2">
            <el-button
                type="primary"
                @click="toggleStageStyle"
                class="custom-btn"
                :class="{ 'btn-work': true }"
            >
              <i class="el-icon-paint-brush mr-2"></i>切换样式
            </el-button>
          </el-col>
          <el-col :md="2">
            <el-button
                type="primary"
                @click="logToObject"
                class="custom-btn"
                :class="{ 'btn-retire': true }"
            >
              <i class="el-icon-s-data mr-2"></i>查看数据
            </el-button>
          </el-col>
          <!-- 新增：添加节点按钮 -->
          <el-col :md="2">
            <el-button
                type="primary"
                @click="showAddNodeDialog"
                class="custom-btn btn-add"
            >
              <i class="el-icon-plus mr-2"></i>添加节点
            </el-button>
          </el-col>
          <el-col :md="2">
            <el-button
                type="primary"
                @click="toggleTheme"
                class="custom-btn btn-theme"
            >
              <i class="el-icon-moon mr-2"></i>{{ isDarkTheme ? '切换白天' : '切换黑夜' }}
            </el-button>
          </el-col>

        </el-row>

        <VueFlow
            fit-view-on-init
            class="my-flow"
            v-model="elements"
            @node-click="handleNodeClick"
            ref="vueFlowRef"
        >
          <Background type="dots" gap="60" size="2" color="#e5e7eb"/>
          <Panel :position="PanelPosition.TopRight" class="custom-panel">
            <div class="panel-control">
              <label for="ishidden" class="hidden-label">
                <span class="label-text">隐藏所有节点</span>
                <input
                    id="ishidden"
                    v-model="isHidden"
                    type="checkbox"
                    class="custom-checkbox"
                />
                <span class="checkbox-icon"></span>
              </label>
            </div>
          </Panel>
          <Controls position="bottom-center" class="custom-controls"/>
        </VueFlow>
        <el-dialog
            v-model="dialogVisible"
            :title="selectedNode?.label || '人生阶段详情'"
            width="30%"
            max-width="500px"
            class="custom-dialog"
            :before-enter="handleDialogEnter"
            :before-leave="handleDialogLeave"
            center
        >
          <div class="dialog-content" :class="`dialog-content--${selectedNode?.class.split('-')[1] || ''}`">
            <div class="stage-icon">
              <div class="stage-icon">
                <el-icon v-if="selectedNode">
                  <component :is="getStageIcon(selectedNode.class)"/>
                </el-icon>
              </div>
            </div>
            <div class="stage-info">
              <p class="stage-time" v-if="selectedNode.time">
                <span class="info-label">时间：</span>
                {{ selectedNode?.time || '——' }}
              </p>
              <p class="stage-desc">
                <span class="info-label">描述：</span>
                {{ selectedNode?.desc || '——' }}
              </p>
              <div class="stage-keyPoints" v-if="selectedNode?.keyPoints">
                  <span class="keyPoint-badge" v-for="(item, idx) in selectedNode.keyPoints" :key="idx">
                      {{ item }}
                  </span>
              </div>
            </div>
          </div>
        </el-dialog>
        <!-- 新增：添加节点表单弹窗 -->
        <el-dialog
            v-model="addNodeDialogVisible"
            title="添加学习阶段节点"
            width="30%"
            max-width="500px"
            class="custom-dialog"
            center
        >
          <el-form
              :model="addNodeForm"
              :rules="addNodeRules"
              ref="addNodeFormRef"
              label-width="100px"
          >
            <!-- 节点名称 -->
            <el-form-item label="节点名称" prop="label">
              <el-input
                  v-model="addNodeForm.label"
                  placeholder="输入阶段名称（如：框架学习）"
              />
            </el-form-item>
            <!-- 阶段类型 -->
            <el-form-item label="阶段类型" prop="stageClass">
              <el-select
                  v-model="addNodeForm.stageClass"
                  placeholder="选择阶段类型"
              >
                <el-option label="基础阶段" value="stage-birth"></el-option>
                <el-option label="进阶阶段" value="stage-study"></el-option>
                <el-option label="实战阶段" value="stage-work"></el-option>
                <el-option label="提升阶段" value="stage-retire"></el-option>
              </el-select>
            </el-form-item>
            <!-- 阶段描述 -->
            <el-form-item label="阶段描述" prop="desc">
              <el-input
                  v-model="addNodeForm.desc"
                  placeholder="输入阶段描述（如：掌握Vue3框架核心用法）"
                  type="textarea"
                  :rows="2"
              />
            </el-form-item>
            <!-- 关键事项 -->
            <el-form-item label="关键事项" prop="keyPoints">
              <el-input
                  v-model="addNodeForm.keyPoints"
                  placeholder="输入关键事项，用逗号分隔（如：组件通信,状态管理）"
                  type="textarea"
                  :rows="2"
              />
            </el-form-item>
            <!-- 新增：选择父节点（要连接的前置节点） -->
            <el-form-item label="连接到" prop="parentNodeId">
              <el-select
                  v-model="addNodeForm.parentNodeId"
                  placeholder="选择新节点要连接的前置节点"
              >
                <el-option
                    v-for="node in nodes"
                    :key="node.id"
                    :label="node.data?.label || `节点${node.id}`"
                    :value="node.id"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="resetAddNodeForm">重置表单</el-button>
            <el-button type="primary" @click="submitAddNode">确认添加</el-button>
          </template>
        </el-dialog>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup name="LifePathFlow">
import '@vue-flow/core/dist/style.css';
import '@vue-flow/core/dist/theme-default.css';
import {Background, Panel, PanelPosition, Controls} from '@vue-flow/additional-components';
import {VueFlow, useVueFlow, Elements} from '@vue-flow/core';
import {onMounted, ref, watch, nextTick} from 'vue';
import {ElMessage} from 'element-plus';
import {Check, RefreshRight, Briefcase, Coffee} from "@element-plus/icons-vue"
import type {FormInstance, FormRules} from 'element-plus';
import {addFlowElement, getFlowList} from "@/api/life";

const isDarkTheme = ref(false); // 默认白天主题
const elements = ref<any[]>([]);
const isHidden = ref(false);
const dialogVisible = ref(false);
const selectedNode = ref<any>(null);
const vueFlowRef = ref<any | null>(null);
const {onPaneReady, setTransform, toObject, nodes, edges} = useVueFlow();
const addNodeDialogVisible = ref(false); // 添加节点弹窗控制
const addNodeFormRef = ref<FormInstance | null>(null);

const addNodeForm = ref({
  label: '',
  stageClass: 'stage-birth',
  desc: '',
  parentNodeId: '',
  keyPoints: '',
});
const addNodeRules = ref<FormRules>({
  label: [{required: true, message: '请输入节点名称', trigger: 'blur'}],
  parentNodeId: [{required: true, message: '请选择要连接的前置节点', trigger: 'change'}],
  stageClass: [{required: true, message: '请选择阶段类型', trigger: 'change'}],
  desc: [{required: true, message: '请输入阶段描述', trigger: 'blur'}],
  keyPoints: [{required: true, message: '请输入关键事项', trigger: 'blur'}],
});

watch(isHidden, () => {
  nodes.value.forEach((n) => (n.hidden = isHidden.value));
  edges.value.forEach((e) => (e.hidden = isHidden.value));
});

onPaneReady(({fitView}) => {
  nextTick(() => {
    // 放大整个画布
    setTransform({x: 210, y: 180, zoom: 1});

    // 如果只放大节点，可以加 class 或修改 style
    nodes.value.forEach(node => {
      node.class = (node.class || '') + ' enlarged';
    });
  });
  fitView({padding: 60});
});

const handleNodeClick = ({node}: { node: any }) => {
  console.log('点击节点：', node);
  selectedNode.value = node;
  dialogVisible.value = true;
};


// 8. 新增：弹窗渐入动画
const handleDialogEnter = (el: HTMLElement) => {
  el.style.opacity = '0';
  el.style.transform = 'scale(0.9)';
  el.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
  nextTick(() => {
    el.style.opacity = '1';
    el.style.transform = 'scale(1)';
  });
};

const handleDialogLeave = (el: HTMLElement) => {
  el.style.opacity = '1';
  el.style.transform = 'scale(1)';
  nextTick(() => {
    el.style.opacity = '0';
    el.style.transform = 'scale(0.9)';
  });
};


const getStageIcon = (className: string) => {
  if (className.includes('birth')) return Check;
  if (className.includes('study')) return RefreshRight;
  if (className.includes('work')) return Briefcase;
  if (className.includes('retire')) return Coffee;
  return null;
};

const resetTransform = () => {
  nodes.value.forEach((node) => {
    const original = elements.value.find(el => el.id === node.id);
    if (original) {
      // 使用动画过渡位置
      const startX = node.position.x ?? 0;
      const startY = node.position.y ?? 0;
      const endX = original.originalPosition.x ?? startX;
      const endY = original.originalPosition.y ?? startY;
      const duration = 500; // 动画时长 ms
      const frameRate = 60;
      const totalFrames = (duration / 1000) * frameRate;
      let frame = 0;

      const animate = () => {
        frame++;
        const progress = frame / totalFrames;
        const easing = progress < 0.5
            ? 2 * progress * progress
            : -1 + (4 - 2 * progress) * progress; // 缓动函数

        node.position.x = startX + (endX - startX) * easing;
        node.position.y = startY + (endY - startY) * easing;

        if (frame < totalFrames) {
          requestAnimationFrame(animate);
        }
      };
      animate();
      node.class = original.class;
    }
  });

  setTransform({x: 210, y: 180, zoom: 1});
  ElMessage.success({message: '视图已重置', duration: 1500});
};


const toggleTheme = () => {
  isDarkTheme.value = !isDarkTheme.value;
  console.log('切换主题：', isDarkTheme.value ? '深色' : '浅色');
};

const updatePos = () => {
  nodes.value.forEach((el) => {
    el.position = {x: Math.random() * 800, y: Math.random() * 300};
  });
};

const toggleStageStyle = () => {
  nodes.value.forEach((el) => {
    const currentClass = el.class;
    if (typeof currentClass === 'string') {
      if (currentClass.includes('stage-birth')) {
        el.class = currentClass === 'stage-birth' ? 'stage-birth-active' : 'stage-birth';
      } else if (currentClass.includes('stage-study')) {
        el.class = currentClass === 'stage-study' ? 'stage-study-active' : 'stage-study';
      } else if (currentClass.includes('stage-work')) {
        el.class = currentClass === 'stage-work' ? 'stage-work-active' : 'stage-work';
      } else if (currentClass.includes('stage-retire')) {
        el.class = currentClass === 'stage-retire' ? 'stage-retire-active' : 'stage-retire';
      }
    }
  });
};

const logToObject = () => {
  const flowData = toObject();
  const fullContent = JSON.stringify(flowData, null, 2);

  const msg = ElMessage({
    dangerouslyUseHTMLString: true,
    message: `
      <div style="max-height:300px;overflow:auto;text-align:left">
        <p>节数：${flowData.nodes.length}</p>
        <p>边数：${flowData.edges.length}</p>
        <pre>${fullContent}</pre>
      </div>
    `,
    duration: 0, // 不自动关闭
    showClose: true
  });

  // 点击空白区域时关闭
  const handler = () => {
    msg.close();
    document.removeEventListener("click", handler);
  };

  // 延迟绑定（避免一点击按钮立刻关闭）
  setTimeout(() => {
    document.addEventListener("click", handler);
  });
};


// 新增：添加节点相关方法
const showAddNodeDialog = () => {
  addNodeDialogVisible.value = true;
  resetAddNodeForm(); // 打开时重置表单
};

// 2. 重置添加节点表单
const resetAddNodeForm = () => {
  addNodeForm.value = {
    label: '',
    stageClass: 'stage-birth',
    desc: '',
    parentNodeId: '',
    keyPoints: '',
  };
  if (addNodeFormRef.value) {
    addNodeFormRef.value.clearValidate(); // 清除验证状态
  }
};

// 3. 提交添加节点（核心逻辑）
const submitAddNode = async () => {
  if (!addNodeFormRef.value) return;
  const valid = await addNodeFormRef.value.validate();
  if (!valid) return;

  const existingNodeIds = nodes.value.map(node => Number(node.id));
  const maxId = existingNodeIds.length ? Math.max(...existingNodeIds) : 0;
  const newNodeId = (maxId + 1).toString();

  const parentNode = nodes.value.find(node => node.id === addNodeForm.value.parentNodeId);
  if (!parentNode) {
    ElMessage.error('未找到选中的前置节点');
    return;
  }

  const newNodePosition = {
    x: parentNode.position.x,
    y: parentNode.position.y + 100
  };

  const newKeyPoints = addNodeForm.value.keyPoints.split(',').map(item => item.trim());

  const newNode = {
    id: newNodeId,
    elemType: 'node',
    label: addNodeForm.value.label,
    positionX: newNodePosition.x,
    positionY: newNodePosition.y,
    elemClass: addNodeForm.value.stageClass,
    description: addNodeForm.value.desc,
    keyPoints: newKeyPoints,
  };

  const newEdge = {
    id: `e${addNodeForm.value.parentNodeId}-${newNodeId}`,
    elemType: 'edge',
    sourceId: addNodeForm.value.parentNodeId,
    targetId: newNodeId,
    animated: true,
    color: getEdgeColorByStage(addNodeForm.value.stageClass),
    elemClass: `edge-${addNodeForm.value.stageClass.split('-')[1]}`,
  };

  await addFlowElement(newNode);
  await addFlowElement(newEdge);
  elements.value = [...elements.value, newNode, newEdge];

  ElMessage.success(`成功添加节点：${addNodeForm.value.label}`);
  addNodeDialogVisible.value = false;
};

// 4. 辅助：根据阶段类型获取边颜色
const getEdgeColorByStage = (stageClass: string) => {
  if (stageClass.includes('birth')) return '#409EFF';
  if (stageClass.includes('study')) return '#67C23A';
  if (stageClass.includes('work')) return '#FAAD14';
  if (stageClass.includes('retire')) return '#9254DE';
  return '#409EFF';
};

onMounted(async () => {
  const {data} = await getFlowList();
  elements.value = data.map((item: any) => {
    if (item.elemType === 'node') {
      return {
        id: item.id,
        label: item.label,
        type: 'default',
        position: {x: item.positionX, y: item.positionY},
        originalPosition: {x: item.positionX, y: item.positionY},
        class: item.elemClass,
        desc: item.description,
        keyPoints: item.keyPoints
      };
    } else if (item.elemType === 'edge') {
      return {
        id: item.id,
        source: item.sourceId,
        target: item.targetId,
        animated: item.animated,
        color: item.color,
        class: item.elemClass,
      };
    }
  });
  console.log('元素：', elements.value);
});
</script>

<style scoped>
@import "@/views/Life/css/flow.scss";
@import "@/views/Life/css/base.scss";
</style>