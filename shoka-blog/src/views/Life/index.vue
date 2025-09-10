<template>
  <div class="page-header">
    <h1 class="page-title">关于</h1>
    <img class="page-cover"
         src="https://wangyoupeng-penghong.oss-cn-beijing.aliyuncs.com/avatar/wallhaven-q21drl_2560x1440.png"
         alt=""/>
    <Waves></Waves>
  </div>
  <div class="bg">
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
          width="50%"
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
</template>

<script lang="ts" setup name="LifePathFlow">
import '@vue-flow/core/dist/style.css';
import '@vue-flow/core/dist/theme-default.css';
import {Background, Panel, PanelPosition, Controls} from '@vue-flow/additional-components';
import {VueFlow, useVueFlow, Elements} from '@vue-flow/core';
import {onMounted,ref, watch, nextTick} from 'vue';
import {ElMessage, ElDialog, ElIcon, ElForm, ElFormItem, ElInput, ElSelect, ElOption, ElButton} from 'element-plus';
import {Check, RefreshRight, Briefcase, Coffee} from "@element-plus/icons-vue"
import type {FormInstance, FormRules} from 'element-plus';
import {FlowNode} from "@/api/life/types";
import {getFlowList} from "@/api/life";

const elements = ref<FlowNode[]>([]);
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
  nodes.value.forEach((node, index) => {
    const original = elements.value.find(el => el.id === node.id);
    if (original) {
      // 使用动画过渡位置
      const startX = node.position.x;
      const startY = node.position.y;
      const endX = original.positionX;
      const endY = original.positionY;

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
      console.log('animate', node.label);
      animate();
      node.class = original.elemClass;
    }
  });

  setTransform({x: 210, y: 180, zoom: 1});
  ElMessage.success({message: '视图已重置', duration: 1500});
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
  ElMessage.info({
    message: `节点数：${flowData.nodes.length}，边数：${flowData.edges.length},内容：${flowData.nodes.map(node => node.label)}`,
    duration: 1500
  });
  console.log('完整数据：', flowData);
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
    type: 'default', // 非首节点用default类型
    label: addNodeForm.value.label,
    position: newNodePosition,
    class: addNodeForm.value.stageClass,
    desc: addNodeForm.value.desc,
    keyPoints: newKeyPoints,
  };

  const newEdge = {
    id: `e${addNodeForm.value.parentNodeId}-${newNodeId}`,
    source: addNodeForm.value.parentNodeId,
    target: newNodeId,
    animated: true,
    color: getEdgeColorByStage(addNodeForm.value.stageClass),
    class: `edge-${addNodeForm.value.stageClass.split('-')[1]}`,
  };

  elements.value = [...elements.value,newNode, newEdge];

  // 提示与关闭弹窗
  console.log('添加的节点和连线：', elements.value);
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
  const { data } = await getFlowList();
  console.log('数据：', data);
  elements.value = data.map((item: any) => {
    console.log('item:', item);
    if (item.elemType === 'node') {
      return {
        id: item.id,
        label: item.label,
        type: 'default',
        positionX: item.positionX,
        positionY: item.positionY,
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
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.container {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

/* 4. 分割线：自定义样式 */
.custom-divider {
  margin: 16px 0;
  border-color: #e5e7eb;
}


.btn-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.custom-btn {
  border-radius: 8px !important;
  padding: 8px 16px !important;
  font-size: 14px !important;
  transition: all 0.2s ease !important;
  border: none !important;
}

.custom-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1) !important;
}

/* 按钮阶段配色 */
.btn-birth {
  background-color: #409EFF !important;
}

.btn-birth:hover {
  background-color: #2563eb !important;
}

.btn-study {
  background-color: #67C23A !important;
}

.btn-study:hover {
  background-color: #52C41A !important;
}

.btn-work {
  background-color: #FAAD14 !important;
}

.btn-work:hover {
  background-color: #FA8C16 !important;
}

.btn-retire {
  background-color: #9254DE !important;
}

.btn-retire:hover {
  background-color: #722ED1 !important;
}

/* 6. Vue Flow容器：优化质感与节点/边样式 */
.my-flow {
  margin: 16px 0;
  height: 650px;
  border-radius: 12px;
  background-color: #fff;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

/* 节点样式：新增hover缩放+渐变背景 */
:deep(.stage-birth) {
  background: linear-gradient(135deg, #F0F9FF, #E0F2FE);
  border: 2px solid #409EFF;
  color: #333;
  border-radius: 10px;
  padding: 14px;
  min-width: 130px;
  font-size: 15px;
  font-weight: 500;
  transition: all 0.2s ease;
}

:deep(.stage-study) {
  background: linear-gradient(135deg, #F0FFF4, #ECFDF5);
  border: 2px solid #67C23A;
  color: #333;
  border-radius: 10px;
  padding: 14px;
  min-width: 130px;
  font-size: 15px;
  font-weight: 500;
  transition: all 0.2s ease;
}

:deep(.stage-work) {
  background: linear-gradient(135deg, #FFF7E6, #FFFBEB);
  border: 2px solid #FAAD14;
  color: #333;
  border-radius: 10px;
  padding: 14px;
  min-width: 130px;
  font-size: 15px;
  font-weight: 500;
  transition: all 0.2s ease;
}

:deep(.stage-retire) {
  background: linear-gradient(135deg, #F9F0FF, #F5F3FF);
  border: 2px solid #9254DE;
  color: #333;
  border-radius: 10px;
  padding: 14px;
  min-width: 130px;
  font-size: 15px;
  font-weight: 500;
  transition: all 0.2s ease;
}

/* 节点active状态：强化阴影 */
:deep(.stage-birth-active) {
  background: linear-gradient(135deg, #F0F9FF, #E0F2FE);
  border: 2px solid #409EFF;
  box-shadow: 0 0 15px rgba(82, 196, 26, 0.2);
}

:deep(.stage-study-active) {
  background: linear-gradient(135deg, #E1F3D8, #D1FAE5);
  border-color: #52C41A;
  box-shadow: 0 0 15px rgba(82, 196, 26, 0.2);
}

:deep(.stage-work-active) {
  background: linear-gradient(135deg, #FFF1CC, #FEF3C7);
  border-color: #FA8C16;
  box-shadow: 0 0 15px rgba(250, 140, 22, 0.2);
}

:deep(.stage-retire-active) {
  background: linear-gradient(135deg, #F3E5FF, #EDE9FE);
  border-color: #722ED1;
  box-shadow: 0 0 15px rgba(114, 46, 209, 0.2);
}

:deep(.vue-flow__node) {
  transition: none !important;
  will-change: transform;
}

/* 节点hover：缩放+阴影 */
:deep(.vue-flow__node):hover {
  transform: scale(1.05);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
  z-index: 10;
}

:deep(.vue-flow__edge path) {
  transition: stroke-width 0.2s ease, stroke 0.2s ease;
}

:deep(.edge-birth path) {
  stroke: #3a92c2;
}

:deep(.edge-study path) {
  stroke: #67C23A;
}

:deep(.edge-work path) {
  stroke: #FAAD14;
}

:deep(.edge-retire path) {
  stroke: #9254DE;
}

:deep(.vue-flow__edge:hover path) {
  stroke-opacity: 0.9;
}

.keyPoint-badge {
  display: inline-block;
  padding: 4px 8px;
  margin: 2px;
  font-size: 12px;
  border-radius: 12px;
  background: rgba(100, 100, 100, 0.1);
  color: #333;
}


/* 7. 右上角面板：美化复选框 */
.custom-panel {
  z-index: 100;
}

.panel-control {
  background: #fff;
  padding: 10px 16px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  font-size: 14px;
}

.hidden-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #666;
}

.label-text {
  transition: color 0.2s ease;
}

.hidden-label:hover .label-text {
  color: #333;
}

/* 自定义复选框 */
.custom-checkbox {
  display: none;
}

.checkbox-icon {
  width: 16px;
  height: 16px;
  border: 2px solid #ddd;
  border-radius: 4px;
  position: relative;
  transition: all 0.2s ease;
}

.custom-checkbox:checked + .checkbox-icon {
  background-color: #409EFF;
  border-color: #409EFF;
}

.custom-checkbox:checked + .checkbox-icon::after {
  content: '';
  position: absolute;
  top: 2px;
  left: 5px;
  width: 4px;
  height: 8px;
  border: solid #fff;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

/* 8. 底部控制栏：半透明背景 */
:deep(.custom-controls) {
  display: flex;
  align-items: center;
  background-color: rgba(255, 255, 255, 0.8) !important;
  border-radius: 8px !important;
  padding: 8px !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05) !important;
  gap: 10px;
}

:deep(.vue-flow__controls-button) {
  border-radius: 6px !important;
  width: 32px !important;
  height: 32px !important;
  margin: 0 4px !important;
  transition: all 0.2s ease !important;
}

:deep(.vue-flow__controls-button:hover) {
  background-color: #f3f4f6 !important;
  transform: translateY(-1px);
}

.custom-dialog {
  border-radius: 16px !important;
  overflow: hidden;
  background: linear-gradient(135deg, #ffffff, #f7f9fc);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.15);
}

.dialog-content {
  display: flex;
  gap: 24px;
  align-items: flex-start;
  padding: 24px;
}

.stage-icon {
  min-width: 60px;
  min-height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}


:deep(.el-dialog__header) {
  padding: 16px 24px !important;
  background-color: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
}

:deep(.el-dialog__title) {
  font-size: 18px !important;
  font-weight: 600 !important;
  color: #333 !important;
}

:deep(.el-dialog__body) {
  padding: 24px !important;
}

/* 阶段图标：与阶段配色联动 */
.stage-icon {
  margin-top: 4px;
}

.stage-icon i {
  font-size: 24px;
}

.dialog-content--birth {
  background: linear-gradient(135deg, #E0F2FE, #F0F9FF);
  border-left: 6px solid #409EFF;
}

.dialog-content--study {
  background: linear-gradient(135deg, #ECFDF5, #F0FFF4);
  border-left: 6px solid #67C23A;
}

.dialog-content--work {
  background: linear-gradient(135deg, #FFFBEB, #FFF7E6);
  border-left: 6px solid #FAAD14;
}

.dialog-content--retire {
  background: linear-gradient(135deg, #F5F3FF, #F9F0FF);
  border-left: 6px solid #9254DE;
}


/* 弹窗内容样式 */
.stage-info {
  flex: 1;
}

.info-label {
  color: #666;
  font-weight: 500;
  margin-right: 4px;
}

.stage-time {
  color: #333;
  font-size: 15px;
  margin-bottom: 12px;
}

.stage-desc {
  color: #333;
  font-size: 15px;
  line-height: 1.6;
  margin-bottom: 16px;
}

/* 关键事项列表 */
.keyPoints-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.keyPoints-title i {
  color: #666;
  font-size: 14px;
}

.keyPoint-item i {
  color: #67C23A;
  margin-top: 4px;
  flex-shrink: 0;
}

/* 新增：添加节点按钮与表单样式 */
/* 1. 添加节点按钮样式 */
.btn-add {
  background-color: #13C2C2 !important; /* 青色区分现有按钮 */
}

.btn-add:hover {
  background-color: #0FB8B8 !important;
}

/* 2. 表单弹窗适配样式 */
:deep(.el-form) {
  margin-top: 10px;
}

:deep(.el-form-item) {
  margin-bottom: 16px;
}

:deep(.el-input__inner), :deep(.el-select__inner) {
  border-radius: 6px;
  border-color: #e5e7eb;
}

:deep(.el-form-item__label) {
  color: #666;
  font-weight: 500;
}

/* 10. 响应式适配：小屏幕优化 */
@media (max-width: 768px) {
  .page-header {
    height: 180px;
  }

  .page-title {
    font-size: 24px;
  }

  .container {
    padding: 16px;
  }

  .my-flow {
    height: 400px;
  }

  .btn-group {
    justify-content: center;
  }

  .custom-btn {
    width: 100%;
  }

  .dialog-content {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
}
</style>