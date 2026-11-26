import { computed, onActivated, onBeforeUnmount, onDeactivated, ref, watch, type Ref } from "vue";
import useStore from "@/store";
import { cancelRegistration, getMyRegistration, registerActivity } from "@/api/activity";
import type { Registration } from "@/api/activity/types";

export function useRegistration(activityId: Ref<number>) {
  const { user, app } = useStore();
  const registration = ref<Registration | null>(null);
  const busy = ref(false);
  const ready = ref(false);
  const notice = ref("");
  const loggedIn = computed(() => user.id !== undefined);
  let timer: ReturnType<typeof setTimeout> | undefined;
  let sequence = 0;
  let polls = 0;
  let active = true;
  let activatedOnce = false;
  const stop = () => { clearTimeout(timer); timer = undefined; };
  const currentKey = () => `activity-registration:${user.id}:${activityId.value}`;

  function schedule() {
    stop();
    if (registration.value?.status !== "PENDING") return;
    if (polls >= 20) {
      notice.value = "仍在确认中，可稍后刷新结果。请勿重复创建报名。";
      return;
    }
    timer = setTimeout(() => { polls++; void load(); }, 3000);
  }

  async function load() {
    stop();
    if (!active || !loggedIn.value) return;
    const ticket = ++sequence;
    const id = activityId.value;
    busy.value = true;
    try {
      const { data } = await getMyRegistration(id);
      if (ticket !== sequence) return;
      if (!data.flag) { notice.value = data.msg || "结果查询失败，请刷新重试。"; return; }
      registration.value = data.data;
      ready.value = true;
      notice.value = "";
      schedule();
    } catch {
      if (ticket === sequence) notice.value = "结果暂未查到，请刷新确认；原报名请求号已保留。";
    } finally {
      if (ticket === sequence) busy.value = false;
    }
  }

  function requestIdentity() {
    const key = currentKey();
    // 先持久化身份再发请求；存储不可用时不发出无法在刷新后接续的请求。
    const previous = localStorage.getItem(key);
    if (previous) return previous;
    const bytes = crypto.getRandomValues(new Uint8Array(16));
    bytes[6] = (bytes[6] & 15) | 64;
    bytes[8] = (bytes[8] & 63) | 128;
    const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, "0")).join("");
    const id = `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
    localStorage.setItem(key, id);
    return id;
  }

  async function submit() {
    if (!loggedIn.value) { app.setLoginFlag(true); return; }
    if (busy.value || !ready.value || registration.value) return;
    let requestId: string;
    try { requestId = requestIdentity(); }
    catch { notice.value = "请允许浏览器本地存储后再报名，以便保留请求号。"; return; }
    await mutate(() => registerActivity(activityId.value, requestId));
  }

  async function mutate(action: () => ReturnType<typeof registerActivity>) {
    stop();
    const ticket = ++sequence;
    busy.value = true;
    try {
      const { data } = await action();
      if (ticket !== sequence) return;
      if (!data.flag) { notice.value = data.msg || "操作未确认，请刷新结果。"; return; }
      registration.value = data.data;
      notice.value = "";
      polls = 0;
      schedule();
    } catch {
      if (ticket === sequence) {
        ready.value = false;
        notice.value = "操作响应未确认，请先刷新结果；报名请求号保持不变。";
      }
    } finally {
      if (ticket === sequence) busy.value = false;
    }
  }

  async function cancel() {
    if (busy.value || !["PENDING", "CONFIRMED"].includes(registration.value?.status || "")) return;
    if (!window.confirm("取消后无法重新报名，确认取消？")) return;
    await mutate(() => cancelRegistration(activityId.value));
  }
  function refresh() { if (!busy.value) { polls = 0; void load(); } }
  watch([activityId, () => user.id], () => {
    stop(); sequence++; polls = 0;
    registration.value = null; busy.value = false; ready.value = false; notice.value = "";
    void load();
  }, { immediate: true, flush: "sync" });
  onActivated(() => {
    if (activatedOnce) { active = true; ready.value = false; polls = 0; void load(); }
    activatedOnce = true;
  });
  onDeactivated(() => { active = false; stop(); sequence++; busy.value = false; });
  onBeforeUnmount(() => { active = false; stop(); sequence++; });
  return { registration, loggedIn, busy, ready, notice, submit, cancel, refresh };
}
