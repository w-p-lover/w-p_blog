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
export interface ActivityForm {
  title: string;
  description: string;
  capacity: number;
  startsAt: string;
  endsAt: string;
}
export type RegistrationStatus = "PENDING" | "CONFIRMED" | "SOLD_OUT" | "CANCELED" | "FAILED";
export interface Registration {
  requestId: string;
  activityId: number;
  status: RegistrationStatus;
  createTime: string;
  updateTime: string;
}
export interface AdminRegistration extends Registration {
  dispatchAttempts: number;
  failureCount: number;
  nextDispatchAt: string | null;
  failureSummary: string | null;
}
export const registrationLabels: Record<RegistrationStatus, string> = {
  PENDING: "已受理", CONFIRMED: "已确认", SOLD_OUT: "名额已满", CANCELED: "已取消", FAILED: "处理失败",
};
export const activityTime = (value: string) => Date.parse(value.replace(" ", "T") + "+08:00");
export const formatActivityTime = (value: string) => value.replace("T", " ").slice(0, 16);
