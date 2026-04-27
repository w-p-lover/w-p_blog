<template>
  <NDialogProvider>
    <NNotificationProvider>
      <NMessageProvider>
        <slot></slot>
        <NaiveProviderContent/>
      </NMessageProvider>
    </NNotificationProvider>
  </NDialogProvider>
</template>

<script setup lang="ts">
import {
  NDialogProvider,
  NMessageProvider,
  NNotificationProvider,
  useDialog,
  useMessage,
  useNotification,
} from "naive-ui";
import {defineComponent, h} from 'vue';

const registerNaiveTools = () => {
  window.$dialog = useDialog();
  window.$message = useMessage();
  window.$notification = useNotification();
};
const NaiveProviderContent = defineComponent({
  name: 'NaiveProviderContent',
  setup() {
    registerNaiveTools();
  },
  render() {
    return h('div');
  }
});
</script>
<style scoped></style>
