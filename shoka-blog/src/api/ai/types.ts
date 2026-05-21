export interface AiChatRequest {
  question: string;
}

export interface AiSource {
  title: string;
  url: string;
}

export interface AiChatResponse {
  answer: string;
  sources: AiSource[];
}

export type AiWriteAssistAction = "chat" | "expand" | "polish" | "summary";

export interface AiWriteAssistRequest {
  action: AiWriteAssistAction;
  content: string;
}

export interface AiWriteAssistStreamOptions {
  signal?: AbortSignal;
  onMessage: (chunk: string) => void;
  onComplete?: () => void;
}
