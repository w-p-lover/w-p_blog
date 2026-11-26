<script setup lang="ts">
import { onMounted, onActivated, onDeactivated, onBeforeUnmount, ref } from "vue";
import Waves from "@/components/Waves/index.vue";
import { getActivities } from "@/api/activity";
import { activityPhase, formatActivityTime, type Activity } from "@/api/activity/types";
import "./activity.scss";
const activities = ref<Activity[]>([]);
const count = ref(0);
const page = ref(1);
const loading = ref(false);
const error = ref("");
const now = ref(Date.now());
let sequence = 0;
let clock: ReturnType<typeof setInterval>;
let activatedOnce = false;
function startClock() { clearInterval(clock); now.value = Date.now(); clock = setInterval(() => { now.value = Date.now(); }, 1000); }
startClock();
onActivated(() => {
  startClock();
  if (activatedOnce) {  void load(); }
  activatedOnce = true;
});
onDeactivated(() => {  sequence++; clearInterval(clock); });
async function load() {
  const ticket = ++sequence;
  loading.value = true; error.value = "";
  try {
    const { data } = await getActivities(page.value);
    if (ticket !== sequence) return;
    if (!data.flag) { error.value = data.msg || "活动加载失败"; return; }
    activities.value = data.data.recordList; count.value = data.data.count;
  } catch { if (ticket === sequence) error.value = "活动暂时无法加载"; }
  finally { if (ticket === sequence) loading.value = false; }
}
function changePage(value: number) { page.value = value; void load(); }
onMounted(load);
onBeforeUnmount(() => { sequence++; clearInterval(clock); });
</script>

<template>
  <div class="page-header">
    <h1 class="page-title">技术活动</h1>
    <img class="page-cover" src="../../assets/images/bg.jpg" alt="">
    <Waves />
  </div>
  <div class="bg">
    <main class="page-container activity-page" :aria-busy="loading">
      <div v-if="loading" class="activity-state" role="status">正在加载活动…</div>
      <div v-else-if="error" class="activity-state" role="alert">
        <p>{{ error }}</p><button class="activity-button" @click="load">重新加载</button>
      </div>
      <div v-else-if="!activities.length" class="activity-state">暂时没有已发布的活动</div>
      <div v-else class="activity-list">
        <article v-for="activity in activities" :key="activity.id" class="activity-list-item">
          <div class="activity-item-meta">
            <span class="activity-phase">{{ activityPhase(activity, now) }}</span>
            <span>余量 {{ activity.remaining }} / {{ activity.capacity }}</span>
          </div>
          <h2><router-link :to="`/activities/${activity.id}`">{{ activity.title }}</router-link></h2>
          <p class="activity-excerpt">{{ activity.description }}</p>
          <div class="activity-item-footer">
            <span>{{ formatActivityTime(activity.startsAt) }} — {{ formatActivityTime(activity.endsAt) }}（北京时间）</span>
            <router-link class="activity-link" :to="`/activities/${activity.id}`">查看活动 →</router-link>
          </div>
        </article>
      </div>
      <n-pagination v-if="!error && count > 12" class="pagination" :page="page" :page-size="12" :item-count="count" :disabled="loading" @update:page="changePage" />
    </main>
  </div>
</template>
