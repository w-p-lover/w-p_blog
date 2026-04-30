export type ChatMessageType = "text" | "image" | "file";
export type ClientMessageStatus = "sending" | "sent" | "failed";

export interface ChatFileInfo {
  fileType: number;
  fileName: string;
  fileSize: string;
}

export interface ChatMessageModel {
  id: string | number;
  localId?: string;
  content: string;
  messageType: ChatMessageType;
  senderName: string;
  name: string;
  createTime: Date;
  senderId: string | number;
  receiveId: string | number;
  senderAvatar: string;
  clientStatus?: ClientMessageStatus;
  fileInfo?: ChatFileInfo;
}

export interface CreateOutgoingMessageOptions {
  content: string;
  messageType: ChatMessageType;
  senderId: string | number;
  receiveId: string | number;
  senderName: string;
  receiverName: string;
  senderAvatar: string;
  fileInfo?: ChatFileInfo;
  localId?: string;
  now?: Date;
}

export interface UploadLikeFile {
  type?: string;
  size: number;
}

export interface ConversationModel {
  id: string | number;
  name?: string;
  detail?: string;
  lastMsg?: string;
  lastTime?: string | Date;
  unreadCount?: number;
  clientStatus?: ClientMessageStatus;
  [key: string]: unknown;
}

export interface ConversationUpdatePayload {
  id: string | number;
  lastMsg?: string;
  lastTime?: string | Date;
  clientStatus?: ClientMessageStatus;
  activeConversationId?: string | number;
  incoming?: boolean;
}

const escapeHtml = (value: string) => {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
};

export const renderEmojiContent = (rawContent: string, emojiMap: Record<string, string>) => {
  const escapedContent = escapeHtml(rawContent.trim());
  if (!escapedContent) {
    return "";
  }
  return escapedContent.replace(/\[.+?\]/g, (token) => {
    const emojiUrl = emojiMap[token];
    if (!emojiUrl) {
      return token;
    }
    return `<img src="${emojiUrl}" width="21" height="21" style="margin: 0 1px;vertical-align: text-bottom"/>`;
  });
};

export const createOutgoingMessage = (options: CreateOutgoingMessageOptions): ChatMessageModel => {
  const localId = options.localId || `local-${Date.now()}-${Math.random().toString(16).slice(2)}`;
  return {
    id: localId,
    localId,
    content: options.content,
    messageType: options.messageType,
    senderName: options.senderName,
    name: options.receiverName,
    createTime: options.now || new Date(),
    senderId: options.senderId,
    receiveId: options.receiveId,
    senderAvatar: options.senderAvatar,
    clientStatus: "sending",
    ...(options.fileInfo ? { fileInfo: options.fileInfo } : {}),
  };
};

export const getFileTypeByMime = (mimeType = "", fileName = "") => {
  const lowerName = fileName.toLowerCase();
  if (
    mimeType === "application/msword" ||
    mimeType === "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ||
    lowerName.endsWith(".doc") ||
    lowerName.endsWith(".docx")
  ) {
    return 1;
  }
  if (
    mimeType === "application/vnd.ms-excel" ||
    mimeType === "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ||
    lowerName.endsWith(".xls") ||
    lowerName.endsWith(".xlsx")
  ) {
    return 2;
  }
  if (
    mimeType === "application/vnd.ms-powerpoint" ||
    mimeType === "application/vnd.openxmlformats-officedocument.presentationml.presentation" ||
    lowerName.endsWith(".ppt") ||
    lowerName.endsWith(".pptx")
  ) {
    return 3;
  }
  if (mimeType === "application/pdf" || lowerName.endsWith(".pdf")) {
    return 4;
  }
  if (
    mimeType === "application/zip" ||
    mimeType === "application/x-zip-compressed" ||
    lowerName.endsWith(".zip")
  ) {
    return 5;
  }
  if (mimeType === "text/plain" || lowerName.endsWith(".txt")) {
    return 6;
  }
  return 0;
};

export const formatFileSize = (bytes: number) => {
  if (bytes >= 1024 * 1024) {
    return `${(bytes / 1024 / 1024).toFixed(2)}MB`;
  }
  return `${(bytes / 1024).toFixed(2)}KB`;
};

export const shouldCompressUpload = (file: UploadLikeFile, uploadType: "img" | "file") => {
  return uploadType === "img" && Boolean(file.type?.startsWith("image/")) && file.size / 1024 > 200;
};

export const getMessagePreview = (
  message: Pick<ChatMessageModel, "messageType" | "content" | "fileInfo">,
  maxLength = 24
) => {
  let preview = "";
  if (message.messageType === "image") {
    preview = "[图片]";
  } else if (message.messageType === "file") {
    preview = `[文件] ${message.fileInfo?.fileName || "未命名文件"}`;
  } else {
    preview = message.content
      .replace(/<img\b[^>]*>/gi, "")
      .replace(/<[^>]+>/g, "")
      .replace(/&lt;/g, "<")
      .replace(/&gt;/g, ">")
      .replace(/&amp;/g, "&")
      .replace(/&quot;/g, "\"")
      .replace(/&#39;/g, "'")
      .replace(/\s+/g, " ")
      .trim();
  }
  if (preview.length > maxLength) {
    return `${preview.slice(0, Math.max(0, maxLength - 1))}...`;
  }
  return preview;
};

export const filterConversations = <T extends ConversationModel>(conversations: T[], keyword: string) => {
  const normalizedKeyword = keyword.trim().toLowerCase();
  if (!normalizedKeyword) {
    return conversations;
  }
  return conversations.filter((conversation) => {
    return [conversation.name, conversation.detail, conversation.lastMsg]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(normalizedKeyword));
  });
};

export const updateConversationState = <T extends ConversationModel>(
  conversations: T[],
  payload: ConversationUpdatePayload
) => {
  const targetIndex = conversations.findIndex((conversation) => conversation.id == payload.id);
  if (targetIndex === -1) {
    return conversations;
  }
  const nextConversations = conversations.slice();
  const target = {
    ...nextConversations[targetIndex],
    ...(payload.lastMsg ? { lastMsg: payload.lastMsg } : {}),
    ...(payload.lastTime ? { lastTime: payload.lastTime } : {}),
    ...(payload.clientStatus ? { clientStatus: payload.clientStatus } : {}),
  } as T;
  const isActive = payload.activeConversationId != null && payload.activeConversationId == payload.id;
  target.unreadCount = payload.incoming && !isActive ? (target.unreadCount || 0) + 1 : target.unreadCount || 0;
  nextConversations.splice(targetIndex, 1);
  nextConversations.unshift(target);
  return nextConversations;
};

interface MergeableMessage {
  id?: string | number;
  localId?: string;
  createTime?: string | Date;
  content?: string;
}

const getMessageKey = (message: MergeableMessage) => {
  if (message.id != null) {
    return `id:${message.id}`;
  }
  if (message.localId) {
    return `local:${message.localId}`;
  }
  return `fallback:${message.createTime || ""}:${message.content || ""}`;
};

const getMessageTime = (message: MergeableMessage) => {
  if (!message.createTime) {
    return 0;
  }
  const timestamp = new Date(message.createTime).getTime();
  return Number.isNaN(timestamp) ? 0 : timestamp;
};

export const mergeOlderMessages = <T extends MergeableMessage>(currentMessages: T[], olderMessages: T[]) => {
  const currentKeys = new Set(currentMessages.map(getMessageKey));
  const uniqueOlderMessages = olderMessages.filter((message) => !currentKeys.has(getMessageKey(message)));
  return [...uniqueOlderMessages, ...currentMessages].sort((a, b) => getMessageTime(a) - getMessageTime(b));
};
