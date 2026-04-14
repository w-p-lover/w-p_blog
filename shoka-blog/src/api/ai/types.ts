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
