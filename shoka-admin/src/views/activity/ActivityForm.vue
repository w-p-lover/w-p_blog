<script setup lang="ts">
import { onBeforeUnmount, reactive, ref } from "vue";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import { createActivity, updateActivity } from "@/api/activity";
import { activityTime, type Activity, type ActivityForm } from "@/api/activity/types";
const props = defineProps<{ activity?: Activity }>();
const emit = defineEmits<{ (e: "saved"): void; (e: "close"): void }>();
const formRef = ref<FormInstance>();
const saving = ref(false);
const error = ref("");
const uncertain = ref(false);
let active = true;
onBeforeUnmount(() => { active = false; });
const form = reactive<ActivityForm>({
  title: props.activity?.title || "", description: props.activity?.description || "",
  capacity: props.activity?.capacity || 100,
  startsAt: props.activity?.startsAt.replace(" ", "T") || "",
  endsAt: props.activity?.endsAt.replace(" ", "T") || "",
});
const rules: FormRules = {
  title: [{ required: true, whitespace: true, message: "请输入活动标题", trigger: "blur" }],
  description: [{ required: true, whitespace: true, message: "请输入活动说明", trigger: "blur" }],
  capacity: [{ required: true, type: "number", min: 1, max: 100000, message: "名额应为1—100000", trigger: "change" }],
  startsAt: [{ required: true, message: "请选择报名开始时间", trigger: "change" }],
  endsAt: [{ required: true, message: "请选择报名结束时间", trigger: "change" }],
};
async function save() {
  if (saving.value || uncertain.value || !await formRef.value?.validate().catch(() => false)) return;
  if (!Number.isFinite(activityTime(form.startsAt)) || !Number.isFinite(activityTime(form.endsAt)) || activityTime(form.startsAt) <= Date.now() || activityTime(form.endsAt) <= activityTime(form.startsAt)) {
    error.value = "开始时间须在未来，结束时间须晚于开始时间（北京时间）。"; return;
  }
  saving.value = true; error.value = "";
  try {
    const payload = { ...form, title: form.title.trim() };
    const { data } = props.activity ? await updateActivity(props.activity.id, payload) : await createActivity(payload);
    if (!active) return;
    if (!data.flag) { error.value = data.msg || "保存失败"; return; }
    ElMessage.success(props.activity ? "活动已保存" : "草稿已创建");
    emit("saved");
  } catch { if (!active) return; uncertain.value = true; error.value = "保存响应未确认，请关闭后刷新列表核对，避免重复创建。"; }
  finally { saving.value = false; }
}
</script>

<template>
  <el-dialog :model-value="true" :title="activity ? '编辑活动' : '创建活动草稿'" width="min(600px, 94vw)" :close-on-click-modal="false" :close-on-press-escape="!saving" :show-close="!saving" @close="emit('close')">
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon class="mb15" />
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" :disabled="saving" @submit.prevent="save">
      <el-form-item label="活动标题" prop="title"><el-input v-model="form.title" maxlength="128" show-word-limit /></el-form-item>
      <el-form-item label="活动说明" prop="description"><el-input v-model="form.description" type="textarea" :rows="5" maxlength="4096" show-word-limit /></el-form-item>
      <el-form-item label="名额" prop="capacity"><el-input-number v-model="form.capacity" :min="1" :max="100000" :precision="0" /></el-form-item>
      <el-form-item label="报名开始（北京时间）" prop="startsAt"><el-date-picker v-model="form.startsAt" type="datetime" value-format="YYYY-MM-DD[T]HH:mm:ss" style="width: 100%" /></el-form-item>
      <el-form-item label="报名结束（北京时间）" prop="endsAt"><el-date-picker v-model="form.endsAt" type="datetime" value-format="YYYY-MM-DD[T]HH:mm:ss" style="width: 100%" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="saving" @click="emit('close')">取消</el-button>
      <el-button type="primary" :loading="saving" :disabled="uncertain" @click="save">{{ activity ? '保存修改' : '创建草稿' }}</el-button>
    </template>
  </el-dialog>
</template>
