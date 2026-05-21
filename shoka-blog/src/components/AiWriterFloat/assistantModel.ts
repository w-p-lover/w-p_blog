import type { AiWriteAssistAction } from "@/api/ai/types";

export type AssistantMessageRole = "assistant" | "user";

export interface AssistantMessage {
  role: AssistantMessageRole;
  content: string;
}

export type WritingAction = Exclude<AiWriteAssistAction, "chat">;

export const writingActions: Array<{ action: WritingAction; label: string }> = [
  { action: "expand", label: "扩写" },
  { action: "polish", label: "润色" },
  { action: "summary", label: "总结" },
];

export const buildAssistantQuestion = (messages: AssistantMessage[], question: string) =>
  [
    "你是博客里的 AI 写作与对话助手。请自然回应用户，必要时给出写作建议。",
    "以下是最近对话：",
    ...messages.slice(-6).map((item) => `${item.role === "user" ? "用户" : "助手"}：${item.content}`),
    `用户：${question}`,
  ].join("\n");
