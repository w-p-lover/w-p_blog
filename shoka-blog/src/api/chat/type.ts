// 好友信息
export interface Friend {
    id: string;           // 好友 ID
    name: string;         // 好友名字
    detail: string;       // 好友详情
    lastMsg: string;      // 最后一条消息
    headImg: string;      // 好友头像
}

// 聊天消息
export interface ChatMessage {
    id?: string | number;
    localId?: string;
    senderAvatar: string;       // 发送者头像
    senderName: string;   // 发送者名字
    senderId: string;   // 发送者名字
    time?: string;         // 消息时间
    content: string;      // 消息内容
    status?: number;       // 消息状态 (0 表示正常)
    clientStatus?: "sending" | "sent" | "failed";
    messageType?: "text" | "image" | "file";
    receiveId: string;     // 好友 ID
    createTime?: string | Date;
    fileInfo?: {
        fileType: number;
        fileName: string;
        fileSize: string;
    };
}

export interface ChatMessagePageRequest {
    senderId: string | number;
    receiveId: string | number;
    pageNum: number;
    pageSize: number;
}

export interface ChatMessagePage {
    records: ChatMessage[];
    total: number;
    pageNum: number;
    pageSize: number;
    hasMore: boolean;
}

export interface ChatReadRequest {
    userId: string | number;
    friendId: string | number;
}
