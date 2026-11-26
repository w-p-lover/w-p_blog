import type { AxiosPromise } from "axios";
import type { PageResult, Result } from "@/model";
import request from "@/utils/request";
import type { Activity, ActivityForm, AdminRegistration, Registration, RegistrationStatus } from "./types";
export const getActivities = (page: number): AxiosPromise<Result<PageResult<Activity[]>>> =>
  request({ url: "/admin/activities", params: { page, size: 10 } });
export const createActivity = (data: ActivityForm): AxiosPromise<Result<Activity>> =>
  request({ url: "/admin/activities", method: "post", data });
export const updateActivity = (id: number, data: ActivityForm): AxiosPromise<Result<null>> =>
  request({ url: `/admin/activities/${id}`, method: "put", data });
export const publishActivity = (id: number): AxiosPromise<Result<null>> =>
  request({ url: `/admin/activities/${id}/publish`, method: "post" });
export const getRegistrations = (id: number, page: number, status: RegistrationStatus | ""): AxiosPromise<Result<PageResult<AdminRegistration[]>>> =>
  request({ url: `/admin/activities/${id}/registrations`, params: { page, size: 10, status: status || undefined } });
export const retryRegistration = (requestId: string): AxiosPromise<Result<Registration>> =>
  request({ url: `/admin/activities/registrations/${requestId}/retry`, method: "post" });
