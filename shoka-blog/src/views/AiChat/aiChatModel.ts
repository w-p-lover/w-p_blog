export type AiChatModeKey = "chat" | "qa";

export interface AiChatMode {
  key: AiChatModeKey;
  provider: "write-assist" | "knowledge";
  label: string;
  badge: string;
  headline: string;
  description: string;
  inputTitle: string;
  inputTip: string;
  submitText: string;
  placeholder: string;
  quickPrompts: string[];
}

export const aiChatModes: AiChatMode[] = [
  {
    key: "chat",
    provider: "write-assist",
    label: "自由对话",
    badge: "OPEN CHAT",
    headline: "先聊聊，再把想法慢慢说清楚",
    description: "适合灵感碰撞、日常聊天和还没成形的想法，不必一开始就把问题写成结论。",
    inputTitle: "对话输入",
    inputTip: "随手说一句也可以",
    submitText: "发送",
    placeholder: "例如：陪我聊聊最近想写的主题，帮我把零散想法理出一个方向。",
    quickPrompts: ["陪我聊聊今天想写什么", "我有个模糊想法，帮我一起展开", "换个轻松一点的角度聊聊技术焦虑"],
  },
  {
    key: "qa",
    provider: "knowledge",
    label: "专业问答",
    badge: "FOCUSED Q&A",
    headline: "把问题说清楚，换一份更可落地的回答",
    description: "聚焦技术排障、方案取舍和架构建议，回答会尽量结构化并附参考来源。",
    inputTitle: "提问面板",
    inputTip: "支持 500 字内精准提问",
    submitText: "开始分析",
    placeholder: "例如：我的 SpringBoot 项目 Redis 命中率下降，如何从指标、键设计、过期策略三个层面定位问题？",
    quickPrompts: [
      "帮我对比 Redis 与 Caffeine 的缓存策略选择",
      "给我一个线上慢 SQL 排查 checklist",
      "把这段业务需求拆成可执行开发任务",
    ],
  },
];

export const getAiChatMode = (key: string): AiChatMode =>
  aiChatModes.find((item) => item.key === key) ?? aiChatModes[0];
