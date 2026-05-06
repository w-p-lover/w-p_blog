import type { BookVO, Resource } from "@/api/book/type";

export type BookStatus = "wish" | "reading" | "read";
export type BookStatusFilter = BookStatus | "all";

export interface BookItem extends Omit<BookVO, "resource" | "status"> {
  status: BookStatus;
  resource: Resource[];
  hover?: boolean;
}

const STATUS_LABELS: Record<BookStatus, string> = {
  wish: "想读",
  reading: "在读",
  read: "已读",
};

const VALID_STATUSES: BookStatus[] = ["wish", "reading", "read"];

export const normalizeStatus = (status?: string): BookStatus => {
  return VALID_STATUSES.includes(status as BookStatus) ? (status as BookStatus) : "wish";
};

export const getStatusLabel = (status?: string) => {
  return STATUS_LABELS[normalizeStatus(status)];
};

export const splitTags = (tags?: string) => {
  if (!tags) {
    return [];
  }
  return tags
    .split(/[,，、;；]/)
    .map((tag) => tag.trim())
    .filter(Boolean);
};

export const joinTags = (tags: string[]) => {
  return tags.map((tag) => tag.trim()).filter(Boolean).join(",");
};

export const parseResources = (resource: unknown): Resource[] => {
  if (!resource) {
    return [];
  }
  if (Array.isArray(resource)) {
    return resource.map(normalizeResource).filter(Boolean) as Resource[];
  }
  if (typeof resource === "string") {
    try {
      return parseResources(JSON.parse(resource));
    } catch {
      return [];
    }
  }
  if (typeof resource === "object") {
    const item = normalizeResource(resource);
    return item ? [item] : [];
  }
  return [];
};

export const normalizeBook = (book: any): BookItem => {
  return {
    ...book,
    status: normalizeStatus(book?.status),
    resource: parseResources(book?.resource),
  };
};

export const filterBooks = (books: BookItem[], keyword: string, status: BookStatusFilter) => {
  const normalizedKeyword = keyword.trim().toLowerCase();
  return books.filter((book) => {
    if (status !== "all" && book.status !== status) {
      return false;
    }
    if (!normalizedKeyword) {
      return true;
    }
    const searchableText = `${book.title} ${book.author ?? ""} ${splitTags(book.tags).join(" ")}`;
    return searchableText.toLowerCase().includes(normalizedKeyword);
  });
};

export const sortBooks = (books: BookItem[], sortType: string) => {
  return [...books].sort((left, right) => {
    if (sortType === "oldest") {
      return getTime(left.addTime) - getTime(right.addTime);
    }
    if (sortType === "nameAsc") {
      return left.title.localeCompare(right.title, "en");
    }
    return getTime(right.addTime) - getTime(left.addTime);
  });
};

export const getBookStats = (books: BookItem[]) => {
  return books.reduce(
    (stats, book) => {
      stats.total += 1;
      stats[book.status] += 1;
      stats.resources += book.resource.length;
      return stats;
    },
    { total: 0, wish: 0, reading: 0, read: 0, resources: 0 },
  );
};

export const getPrimaryResource = (resources: Resource[]) => {
  return resources.find((item) => Boolean(item.url?.trim())) || null;
};

export const getAvailableResourceCount = (resources: Resource[]) => {
  return resources.filter((item) => Boolean(item.url?.trim())).length;
};

const normalizeResource = (resource: any): Resource | null => {
  if (!resource || typeof resource !== "object") {
    return null;
  }
  return {
    name: String(resource.name ?? "").trim(),
    url: String(resource.url ?? "").trim(),
    type: String(resource.type ?? "other").trim() || "other",
  };
};

const getTime = (value?: string) => {
  const time = value ? new Date(value).getTime() : 0;
  return Number.isNaN(time) ? 0 : time;
};
