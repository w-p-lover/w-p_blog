import { Result } from "@/model";
import request from "@/utils/request";
import { AxiosPromise } from "axios";
import { AiChatRequest, AiChatResponse } from "./types";

export function aiChat(data: AiChatRequest): AxiosPromise<Result<AiChatResponse>> {
  return request({ url: "/api/ai/chat", method: "post", data });
}
