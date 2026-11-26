export interface Activity {
  id: number;
  title: string;
  description: string;
  status: "DRAFT" | "PUBLISHED";
  startsAt: string;
  endsAt: string;
  capacity: number;
  remaining: number;
}

export type RegistrationStatus = "PENDING" | "CONFIRMED" | "SOLD_OUT" | "CANCELED" | "FAILED";
export interface Registration {
  requestId: string;
  activityId: number;
  status: RegistrationStatus;
  createTime: string;
  updateTime: string;
}
export const registrationLabels: Record<RegistrationStatus, string> = {
  PENDING: "已受理，等待确认", CONFIRMED: "报名成功", SOLD_OUT: "本次名额已满",
  CANCELED: "已取消", FAILED: "处理失败，待管理员恢复",
};

// 服务端LocalDateTime按Asia/Shanghai解释，不依赖浏览器所在时区。
export const activityTime = (value: string) => Date.parse(value.replace(" ", "T") + "+08:00");
export const formatActivityTime = (value: string) => value.replace("T", " ").slice(0, 16);
export function activityPhase(activity: Activity, now: number) {
  if (now < activityTime(activity.startsAt)) return "报名未开始";
  if (now >= activityTime(activity.endsAt)) return "报名已结束";
  return "报名中";
}
