export type RouteFilter = "all" | "stage-birth" | "stage-study" | "stage-work" | "stage-retire";
export type RouteStatus = "done" | "doing" | "planned";

export interface RawFlowElement {
  id: string;
  elemType: "node" | "edge";
  label?: string;
  positionX?: number;
  positionY?: number;
  elemClass?: string;
  description?: string;
  keyPoints?: string[] | string;
  sourceId?: string;
  targetId?: string;
  animated?: boolean;
  color?: string;
  time?: string;
  status?: RouteStatus;
  tags?: string[];
  links?: RouteLink[];
}

export interface RouteLink {
  title: string;
  url: string;
  type: "article" | "project" | "note";
}

export interface RouteNodeData {
  label: string;
  desc: string;
  time: string;
  status: RouteStatus;
  tags: string[];
  keyPoints: string[];
  links: RouteLink[];
}

export interface RouteElement {
  id: string;
  type?: string;
  label?: string;
  source?: string;
  target?: string;
  animated?: boolean;
  color?: string;
  position?: { x: number; y: number };
  originalPosition?: { x: number; y: number };
  class?: string;
  elemType?: "node" | "edge";
  data?: RouteNodeData;
}

export interface RouteStats {
  total: number;
  done: number;
  doing: number;
  planned: number;
  linked: number;
}

const stageStatusMap: Record<string, RouteStatus> = {
  "stage-birth": "done",
  "stage-study": "done",
  "stage-work": "doing",
  "stage-retire": "planned",
};

const normalizeList = (value?: string[] | string) => {
  if (Array.isArray(value)) {
    return value.map((item) => item.trim()).filter(Boolean);
  }
  if (typeof value === "string") {
    return value.split(",").map((item) => item.trim()).filter(Boolean);
  }
  return [];
};

const getStageClass = (className = "") => {
  return ["stage-birth", "stage-study", "stage-work", "stage-retire"].find((stage) => className.includes(stage)) || "stage-study";
};

const getFallbackLinks = (stageClass: string, label: string): RouteLink[] => {
  if (stageClass !== "stage-work") return [];
  return [
    {
      title: `${label}相关项目`,
      url: "/trend",
      type: "project",
    },
  ];
};

export const enrichRouteNodes = (rawElements: RawFlowElement[]): RouteElement[] => {
  return rawElements
    .map((item, index) => {
      if (item.elemType === "node") {
        const stageClass = getStageClass(item.elemClass);
        const keyPoints = normalizeList(item.keyPoints);
        const label = item.label || `路线节点 ${item.id}`;
        const links = item.links?.length ? item.links : getFallbackLinks(stageClass, label);

        return {
          id: item.id,
          type: "default",
          label,
          elemType: "node",
          position: { x: item.positionX || 0, y: item.positionY || 0 },
          originalPosition: { x: item.positionX || 0, y: item.positionY || 0 },
          class: stageClass,
          data: {
            label,
            desc: item.description || "这个阶段还没有写下复盘。",
            time: item.time || `阶段 ${String(index + 1).padStart(2, "0")}`,
            status: item.status || stageStatusMap[stageClass],
            tags: item.tags?.length ? item.tags : keyPoints.slice(0, 4),
            keyPoints,
            links,
          },
        };
      }

      if (!item.sourceId || !item.targetId) {
        return undefined;
      }

      return {
        id: item.id,
        elemType: "edge",
        source: item.sourceId,
        target: item.targetId,
        animated: item.animated,
        color: item.color,
        class: item.elemClass,
      };
    })
    .filter(Boolean) as RouteElement[];
};

export const getRouteStats = (elements: RouteElement[]): RouteStats => {
  const routeNodes = elements.filter((item) => item.elemType === "node");
  return {
    total: routeNodes.length,
    done: routeNodes.filter((item) => item.data?.status === "done").length,
    doing: routeNodes.filter((item) => item.data?.status === "doing").length,
    planned: routeNodes.filter((item) => item.data?.status === "planned").length,
    linked: routeNodes.filter((item) => (item.data?.links.length || 0) > 0).length,
  };
};

export const getPlaybackSequence = (elements: RouteElement[]) => {
  return elements
    .filter((item) => item.elemType === "node")
    .sort((a, b) => Number(a.id) - Number(b.id));
};

export const getFilteredRouteElements = (elements: RouteElement[], filter: RouteFilter) => {
  if (filter === "all") return elements;

  const visibleNodeIds = new Set(
    elements
      .filter((item) => item.elemType === "node" && item.class?.includes(filter))
      .map((item) => item.id)
  );

  return elements.filter((item) => {
    if (item.elemType === "node") return visibleNodeIds.has(item.id);
    if (!item.source || !item.target) return false;
    return visibleNodeIds.has(item.source) && visibleNodeIds.has(item.target);
  });
};
