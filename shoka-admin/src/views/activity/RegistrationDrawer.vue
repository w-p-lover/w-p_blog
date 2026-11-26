<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { getRegistrations, retryRegistration } from "@/api/activity";
import { formatActivityTime, registrationLabels, type Activity, type AdminRegistration, type RegistrationStatus } from "@/api/activity/types";
import RegistrationDiagnostics from "./RegistrationDiagnostics.vue";
const props = defineProps<{ activity: Activity }>();
const emit = defineEmits<{ (e: "close"): void }>();
const rows = ref<AdminRegistration[]>([]);
const page = ref(1);
const count = ref(0);
const status = ref<RegistrationStatus | "">("");
const loading = ref(false);
const retrying = ref("");
const error = ref("");
const notice = ref("");
const needsRefresh = ref(false);
const diagnosticId = ref("");
const diagnostic = computed(() => rows.value.find(row => row.requestId === diagnosticId.value));
let sequence = 0;
let active = true;
async function load() {
  const ticket = ++sequence;
  loading.value = true; error.value = "";
  try {
    const { data } = await getRegistrations(props.activity.id, page.value, status.value);
    if (ticket !== sequence) return;
    if (!data.flag) { error.value = data.msg || "报名列表加载失败"; return; }
    rows.value = data.data.recordList; count.value = data.data.count; needsRefresh.value = false;
  } catch { if (ticket === sequence) error.value = "报名列表暂时无法加载"; }
  finally { if (ticket === sequence) loading.value = false; }
}
async function retry(row: AdminRegistration) {
  if (retrying.value || needsRefresh.value || row.status !== "FAILED") return;
  // 确认框期间即占用操作，避免重复弹窗或旧列表请求覆盖恢复结果。
  retrying.value = row.requestId;
  sequence++;
  loading.value = false;
  try {
    try { await ElMessageBox.confirm("将按原报名身份重新确认名额，确认恢复？", "恢复失败报名", { type: "warning" }); }
    catch { return; }
    if (!active) return;
    needsRefresh.value = true;
    notice.value = "";
    try {
      const { data } = await retryRegistration(row.requestId);
      if (!active) return;
      if (data.flag) ElMessage.success("已恢复受理，正在核对当前状态");
      else notice.value = data.msg || "恢复未确认，请核对原报名状态。";
    } catch {
      if (!active) return;
      notice.value = "恢复响应未确认，请刷新核对原报名状态。";
    }
    // 成功、业务拒绝或网络未知都只读取当前事实，不自动重发恢复请求。
    if (active) await load();
  } finally { if (active) retrying.value = ""; }
}
function filter() { page.value = 1; void load(); }
watch(() => props.activity.id, filter, { immediate: true });
onBeforeUnmount(() => { active = false; sequence++; });
</script>

<template>
  <el-drawer :model-value="true" :title="`${activity.title} · 报名记录`" size="min(900px, 96vw)" @close="emit('close')">
    <div class="registration-toolbar">
      <el-select v-model="status" aria-label="报名状态" :disabled="!!retrying" @change="filter">
        <el-option label="全部状态" value="" />
        <el-option v-for="(label, value) in registrationLabels" :key="value" :label="label" :value="value" />
      </el-select>
      <el-button :disabled="loading || !!retrying" @click="load">刷新</el-button>
    </div>
    <el-alert v-if="notice" :title="notice" type="warning" :closable="false" show-icon class="mb15" />
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon class="mb15" />
    <el-table v-else v-loading="loading" :data="rows" border empty-text="暂无报名记录">
      <el-table-column prop="requestId" label="报名请求号" min-width="300" />
      <el-table-column label="状态" min-width="100"><template #default="{ row }"><el-tag :type="row.status === 'FAILED' ? 'danger' : row.status === 'CONFIRMED' ? 'success' : 'info'">{{ registrationLabels[row.status as RegistrationStatus] }}</el-tag></template></el-table-column>
      <el-table-column label="更新时间（北京时间）" min-width="175"><template #default="{ row }">{{ formatActivityTime(row.updateTime) }}</template></el-table-column>
      <el-table-column label="操作" width="190" fixed="right"><template #default="{ row }">
        <el-button type="primary" link :aria-expanded="diagnosticId === row.requestId" :aria-controls="`registration-diagnostics-${row.requestId}`" @click="diagnosticId = diagnosticId === row.requestId ? '' : row.requestId">处理记录</el-button>
        <el-button v-if="row.status === 'FAILED'" v-hasPerm="['activity:registration:retry']" type="primary" link :disabled="!!retrying || needsRefresh || loading" :loading="retrying === row.requestId" @click="retry(row)">恢复受理</el-button>
      </template></el-table-column>
    </el-table>
    <RegistrationDiagnostics v-if="!error && diagnostic" :row="diagnostic" @close="diagnosticId = ''" />
    <el-pagination v-if="!error && count > 10" v-model:current-page="page" :page-size="10" :total="count" layout="prev, pager, next" :disabled="loading || !!retrying" @current-change="load" />
  </el-drawer>
</template>

<style scoped>
.registration-toolbar { display: flex; gap: 12px; margin-bottom: 16px; }
.el-pagination { margin-top: 20px; }
</style>
