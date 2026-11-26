<script setup lang="ts">
import { computed, onActivated, onDeactivated, onBeforeUnmount, ref, watch } from "vue";
import { useRoute } from "vue-router";
import Waves from "@/components/Waves/index.vue";
import RegistrationPanel from "./RegistrationPanel.vue";
import { getActivity } from "@/api/activity";
import { activityPhase, formatActivityTime, type Activity } from "@/api/activity/types";
import "./activity.scss";
const route = useRoute();
const id = computed(() => Number(route.params.id));
const activity = ref<Activity | null>(null);
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
  activity.value = null; loading.value = true; error.value = "";
  try {
    if (!Number.isSafeInteger(id.value) || id.value <= 0) { error.value = "活动地址无效"; return; }
    const { data } = await getActivity(id.value);
    if (ticket !== sequence) return;
    if (!data.flag) { error.value = data.msg || "活动不可用"; return; }
    activity.value = data.data;
  } catch { if (ticket === sequence) error.value = "活动暂时无法加载"; }
  finally { if (ticket === sequence) loading.value = false; }
}
watch(id, load, { immediate: true });
onBeforeUnmount(() => { sequence++; clearInterval(clock); });
</script>

<template>
  <div class="page-header">
    <h1 class="page-title">技术活动</h1>
    <img class="page-cover" src="../../assets/images/bg.jpg" alt=""><Waves />
  </div>
  <div class="bg">
    <main class="page-container activity-page" :aria-busy="loading">
      <router-link class="activity-link" to="/activities">← 全部活动</router-link>
      <div v-if="loading" class="activity-state" role="status">正在加载活动…</div>
      <div v-else-if="error" class="activity-state" role="alert"><p>{{ error }}</p><button class="activity-button" @click="load">重新加载</button></div>
      <template v-else-if="activity">
        <header class="activity-detail-header">
          <span class="activity-phase">{{ activityPhase(activity, now) }}</span>
          <h1>{{ activity.title }}</h1>
          <p>{{ formatActivityTime(activity.startsAt) }} — {{ formatActivityTime(activity.endsAt) }}（北京时间）</p>
          <p>剩余 {{ activity.remaining }} / {{ activity.capacity }} 个名额</p>
        </header>
        <p class="activity-description">{{ activity.description }}</p>
        <RegistrationPanel :key="activity.id" :activity="activity" :now="now" />
      </template>
    </main>
  </div>
</template>
