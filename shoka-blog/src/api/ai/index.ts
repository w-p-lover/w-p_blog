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

export function aiKnowledgeChat(data: AiChatRequest, signal?: AbortSignal): AxiosPromise<Result<AiChatResponse>> {
  return request({ url: "/api/ai/chat", method: "post", data, signal });
}

export const aiChat = aiKnowledgeChat;

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
  let eventLines: string[] = [];

  const emitEvent = () => {
    if (!eventLines.length) {
      return;
    }

    const chunk = eventLines.join("\n");
    eventLines = [];
    if (chunk && chunk !== "[DONE]") {
      options.onMessage(chunk);
    }
  };

  const parseLine = (line: string) => {
    const raw = line.endsWith("\r") ? line.slice(0, -1) : line;
    if (!raw) {
      emitEvent();
      return;
    }

    if (raw.startsWith("data:")) {
      const data = raw.startsWith("data: ") ? raw.slice(6) : raw.slice(5);
      eventLines.push(data);
      return;
    }

    if (!raw.startsWith(":")) {
      eventLines.push(raw);
    }
  };

  while (true) {
    const { done, value } = await reader.read();
    if (done) {
      break;
    }

    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split("\n");
    buffer = lines.pop() ?? "";

    lines.forEach(parseLine);
  }

  if (buffer) {
    parseLine(buffer);
  }
  emitEvent();

  options.onComplete?.();
}
