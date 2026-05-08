export const DEFAULT_HITOKOTO_TYPE = "d";
export const HITOKOTO_TYPE_STORAGE_KEY = "home-hitokoto-type";
export const HITOKOTO_TYPE_CHANGE_EVENT = "home-hitokoto-type-change";

export const HITOKOTO_TYPES = [
  { value: "a", label: "动画" },
  { value: "b", label: "漫画" },
  { value: "c", label: "游戏" },
  { value: "d", label: "文学" },
  { value: "e", label: "原创" },
  { value: "f", label: "来自网络" },
  { value: "g", label: "其他" },
  { value: "h", label: "影视" },
  { value: "i", label: "诗词" },
  { value: "j", label: "网易云" },
  { value: "k", label: "哲学" },
  { value: "l", label: "抖机灵" },
] as const;

export type HitokotoType = typeof HITOKOTO_TYPES[number]["value"];

export const normalizeHitokotoType = (type: unknown): HitokotoType => {
  return HITOKOTO_TYPES.some((item) => item.value === type) ? type as HitokotoType : DEFAULT_HITOKOTO_TYPE;
};

export const buildHitokotoUrl = (type: unknown) => {
  return `https://international.v1.hitokoto.cn/?c=${normalizeHitokotoType(type)}`;
};
