<script setup lang="ts">
import { type AdminRegistration } from "@/api/activity/types";
defineProps<{ row: AdminRegistration }>();
defineEmits<{ (e: "close"): void }>();
</script>

<template>
  <section :id="`registration-diagnostics-${row.requestId}`" class="registration-diagnostics" aria-label="报名处理记录">
    <header>
      <h3>处理记录</h3>
      <el-button text aria-label="收起处理记录" @click="$emit('close')">收起</el-button>
    </header>
    <p class="request-id">{{ row.requestId }}</p>
    <dl>
      <div>
        <dt>投递预约次数</dt>
        <dd>{{ row.dispatchAttempts }} 次</dd>
      </div>
      <div>
        <dt>确认失败次数</dt>
        <dd>{{ row.failureCount }} 次</dd>
      </div>
      <div class="full-width">
        <dt>下次扫描资格（北京时间）</dt>
        <dd>{{ row.status === 'PENDING' ? (row.nextDispatchAt ? row.nextDispatchAt.replace('T', ' ').slice(0, 19) : '未记录扫描时间') : '当前状态无需自动投递' }}</dd>
      </div>
      <div v-if="row.failureSummary" class="full-width">
        <dt>故障摘要</dt>
        <dd>{{ row.failureSummary }}</dd>
      </div>
    </dl>
    <p class="diagnostic-note">预约不代表消息发送或确认成功；人工恢复后，两项次数重新计数。到期仅表示可被扫描。</p>
  </section>
</template>

<style scoped>
.registration-diagnostics { margin-top: 20px; padding: 16px; background: var(--el-fill-color-light); color: var(--el-text-color-primary); border-radius: 4px; font-size: 15px; }
header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
h3 { font-size: 16px; margin: 0; }
.request-id { margin: 8px 0 16px; overflow-wrap: anywhere; font-family: monospace; }
dl { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; margin: 0; }
dt { color: var(--el-text-color-regular); margin-bottom: 5px; }
dd { margin: 0; overflow-wrap: anywhere; }
.full-width { grid-column: 1 / -1; }
.diagnostic-note { margin: 16px 0 0; color: var(--el-text-color-regular); line-height: 1.6; }
</style>
