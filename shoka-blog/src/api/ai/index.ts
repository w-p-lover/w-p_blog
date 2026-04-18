import { Result } from "@/model";
import request from "@/utils/request";
import { getToken, token_prefix } from "@/utils/token";
import { AxiosPromise } from "axios";
import {
  AiChatRequest,
  AiChatResponse,
  AiWriteAssistRequest,
  AiWriteAssistStreamOptions,
} from "./types";

export function aiChat(data: AiChatRequest): AxiosPromise<Result<AiChatResponse>> {
  return request({ url: "/api/ai/chat", method: "post", data });
}

export async function aiWriteAssistStream(
  params: AiWriteAssistRequest,
  options: AiWriteAssistStreamOptions,
): Promise<void> {
  const query = new URLSearchParams({
    action: params.action,
    content: params.content,
  });

  const headers: Record<string, string> = {
    Accept: "text/event-stream",
  };

  const token = getToken();
  if (token) {
    headers.Authorization = token_prefix + token;
  }

  const response = await fetch(`http://localhost:8080/api/ai/write-assist?${query.toString()}`, {
    method: "GET",
    headers,
    signal: options.signal,
  });

  if (!response.ok || !response.body) {
    throw new Error("写作助手请求失败");
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  while (true) {
    const { done, value } = await reader.read();
    if (done) {
      break;
    }

    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split("\n");
    buffer = lines.pop() ?? "";

    lines.forEach((line) => {
      const raw = line.trim();
      if (!raw) {
        return;
      }

      const chunk = raw.startsWith("data:") ? raw.slice(5).trim() : raw;
      if (!chunk || chunk === "[DONE]") {
        return;
      }

      options.onMessage(chunk);
    });
  }

  const tail = buffer.trim();
  if (tail) {
    const chunk = tail.startsWith("data:") ? tail.slice(5).trim() : tail;
    if (chunk && chunk !== "[DONE]") {
      options.onMessage(chunk);
    }
  }

  options.onComplete?.();
}
