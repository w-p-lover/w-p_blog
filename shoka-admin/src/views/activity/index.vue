<script setup lang="ts">
import { onActivated, onDeactivated, onBeforeUnmount, onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { getActivities, publishActivity } from "@/api/activity";
import { activityTime, formatActivityTime, type Activity } from "@/api/activity/types";
import ActivityForm from "./ActivityForm.vue";
import RegistrationDrawer from "./RegistrationDrawer.vue";
const activities = ref<Activity[]>([]);
const page = ref(1);
const count = ref(0);
const loading = ref(false);
const error = ref("");
const formOpen = ref(false);
const editing = ref<Activity>();
const registrationsFor = ref<Activity>();
const publishing = ref<number>();
const now = ref(Date.now());
let sequence = 0;
let active = true;
let clock: ReturnType<typeof setInterval>;
let activatedOnce = false;
function startClock() { clearInterval(clock); now.value = Date.now(); clock = setInterval(() => { now.value = Date.now(); }, 1000); }
startClock();
onActivated(() => {
  startClock();
  if (activatedOnce) { active = true; void load(); }
  activatedOnce = true;
});
onDeactivated(() => { active = false; formOpen.value = false; registrationsFor.value = undefined; sequence++; clearInterval(clock); });
async function load() {
  const ticket = ++sequence;
  loading.value = true; error.value = "";
  try {
    const { data } = await getActivities(page.value);
    if (ticket !== sequence) return;
    if (!data.flag) { error.value = data.msg || "活动列表加载失败"; return; }
    activities.value = data.data.recordList; count.value = data.data.count;
  } catch { if (ticket === sequence) error.value = "活动列表暂时无法加载"; }
  finally { if (ticket === sequence) loading.value = false; }
}
function openForm(activity?: Activity) { editing.value = activity; formOpen.value = true; }
function saved() { formOpen.value = false; void load(); }
async function publish(activity: Activity) {
  if (publishing.value) return;
  try { await ElMessageBox.confirm(`确认发布“${activity.title}”？`, "发布活动", { type: "warning" }); }
  catch { return; }
  if (!active) return;
  publishing.value = activity.id;
  try {
    const { data } = await publishActivity(activity.id);
    if (!active) return;
    if (!data.flag) { error.value = data.msg || "发布失败"; return; }
    ElMessage.success("活动已发布"); await load();
  } catch { if (active) error.value = "发布响应未确认，请刷新核对当前状态。"; }
  finally { if (active) publishing.value = undefined; }
}
onMounted(load);
onBeforeUnmount(() => { active = false; sequence++; clearInterval(clock); });
</script>

<template>
  <main class="app-container activity-admin">
    <header class="activity-toolbar">
      <h2>技术活动</h2>
      <div>
        <el-button v-hasPerm="['activity:event:add']" type="primary" @click="openForm()">创建活动</el-button>
        <el-button :disabled="loading || !!publishing" @click="load">刷新</el-button>
      </div>
    </header>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon class="mb15" />
    <el-table v-else v-loading="loading" :data="activities" border empty-text="暂无活动，先创建一份草稿">
      <el-table-column prop="title" label="活动标题" min-width="200" show-overflow-tooltip />
      <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'">{{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}</el-tag></template></el-table-column>
      <el-table-column label="报名时间（北京时间）" min-width="300"><template #default="{ row }">{{ formatActivityTime(row.startsAt) }} — {{ formatActivityTime(row.endsAt) }}</template></el-table-column>
      <el-table-column label="剩余 / 总名额" width="140"><template #default="{ row }">{{ row.remaining }} / {{ row.capacity }}</template></el-table-column>
      <el-table-column label="操作" width="245" fixed="right"><template #default="{ row }">
        <el-button v-hasPerm="['activity:event:update']" type="primary" link :disabled="now >= activityTime(row.startsAt) || !!publishing" @click="openForm(row)">编辑</el-button>
        <el-button v-if="row.status === 'DRAFT'" v-hasPerm="['activity:event:publish']" type="primary" link :loading="publishing === row.id" :disabled="now >= activityTime(row.startsAt) || !!publishing" @click="publish(row)">发布</el-button>
        <el-button v-hasPerm="['activity:registration:list']" type="primary" link @click="registrationsFor = row">报名记录</el-button>
      </template></el-table-column>
    </el-table>
    <el-pagination v-if="!error && count > 10" v-model:current-page="page" :page-size="10" :total="count" layout="total, prev, pager, next" :disabled="loading" @current-change="load" />
    <ActivityForm v-if="formOpen" :activity="editing" @saved="saved" @close="formOpen = false" />
    <RegistrationDrawer v-if="registrationsFor" :key="registrationsFor.id" :activity="registrationsFor" @close="registrationsFor = undefined" />
  </main>
</template>

<style scoped>
.activity-admin { font-size: 15px; }
.activity-toolbar { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-bottom: 20px; flex-wrap: wrap; }
.activity-toolbar h2 { font-size: 21px; margin: 0; }
.el-pagination { margin-top: 20px; }
</style>
