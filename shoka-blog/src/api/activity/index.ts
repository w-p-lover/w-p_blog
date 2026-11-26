import type { AxiosPromise } from "axios";
import type { PageResult, Result } from "@/model";
import request from "@/utils/request";
import type { Activity, Registration } from "./types";

export const getActivities = (page: number): AxiosPromise<Result<PageResult<Activity[]>>> =>
  request({ url: "/activities", params: { page, size: 12 } });
export const getActivity = (id: number): AxiosPromise<Result<Activity>> =>
  request({ url: `/activities/${id}` });
export const getMyRegistration = (id: number): AxiosPromise<Result<Registration | null>> =>
  request({ url: `/activities/${id}/registration` });
export const registerActivity = (id: number, requestId: string): AxiosPromise<Result<Registration>> =>
  request({ url: `/activities/${id}/registration`, method: "post", data: { requestId } });
export const cancelRegistration = (id: number): AxiosPromise<Result<Registration>> =>
  request({ url: `/activities/${id}/registration`, method: "delete" });
