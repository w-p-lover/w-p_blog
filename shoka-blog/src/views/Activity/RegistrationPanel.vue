<script setup lang="ts">
import { computed } from "vue";
import type { Activity } from "@/api/activity/types";
import { activityTime, registrationLabels } from "@/api/activity/types";
import { useRegistration } from "./useRegistration";
const props = defineProps<{ activity: Activity; now: number }>();
const id = computed(() => props.activity.id);
const { registration, loggedIn, busy, ready, notice, submit, cancel, refresh } = useRegistration(id);
const open = computed(() => props.now >= activityTime(props.activity.startsAt) && props.now < activityTime(props.activity.endsAt));
</script>

<template>
  <section class="registration-panel" aria-label="我的报名" :aria-busy="busy">
    <h2>我的报名</h2>
    <div aria-live="polite">
      <p v-if="registration" class="registration-status" :class="`status-${registration.status}`">{{ registrationLabels[registration.status] }}</p>
      <p v-if="registration?.status === 'PENDING'">受理成功，名额正在确认。</p>
      <p v-if="registration?.status === 'CANCELED' || registration?.status === 'SOLD_OUT'">本次报名已结束，无法重新报名。</p>
      <p v-if="notice" role="status">{{ notice }}</p>
    </div>
    <div class="registration-actions">
      <button v-if="!loggedIn || (!registration && ready)" class="activity-button" type="button"
              :disabled="busy || (loggedIn && !open)" @click="submit">
        {{ !loggedIn ? '登录报名' : busy ? '提交中…' : open ? '立即报名' : '当前不在报名时间内' }}
      </button>
      <button v-if="loggedIn" class="activity-button secondary" type="button" :disabled="busy" @click="refresh">{{ busy ? '查询中…' : '刷新结果' }}</button>
      <button v-if="registration && ['PENDING', 'CONFIRMED'].includes(registration.status)" class="activity-button secondary" type="button" :disabled="busy" @click="cancel">取消报名</button>
    </div>
  </section>
</template>

<style scoped>
.registration-panel { padding-top: 1.5rem; border-top: 1px solid var(--home-border); }
.registration-panel h2 { font-size: 1.25rem; margin-bottom: 1rem; }
.registration-panel p { margin: .75rem 0; }
.registration-status { font-weight: 600; color: var(--home-accent-cool); }
.status-FAILED, .status-SOLD_OUT { color: var(--home-accent-warm); }
.registration-actions { display: flex; gap: .75rem; flex-wrap: wrap; margin-top: 1rem; }
</style>
