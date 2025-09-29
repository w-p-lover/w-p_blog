/**
 * 文档协作者
 */
export interface Collab {
    /** 协作者姓名 */
    name: string;
    /** 角色（editor/viewer） */
    role: string;
}

/**
 * 文档信息（后端返回）
 */
export interface Doc {
    /** 文档ID */
    id: number;
    /** 标题 */
    title: string;
    /** 分类名称 */
    categoryName: string;
    /** 正文内容 */
    content: string;
    /** 主作者 */
    leadAuthor: string;
    /** 版本号 */
    version: string;
    /** 是否正在编辑 */
    status: string;

    rejectReason?: string;
    /** 标签列表 */
    tags: string[];
    /** 评论数 */
    comments: number;
    /** 简介 */
    description: string;
}


/**
 * 文档创建/更新请求体
 */
export interface DocDTO {
    /** 文档ID（更新时必传） */
    id?: number;
    /** 标题 */
    title: string;
    /** 分类 */
    categoryName: string;
    /** 摘要 */
    description: string;
    /** 正文内容 */
    content: string;
    /** 标签名列表 */
    tags: string[];
    /** 版本号 */
    version?: string;
    /** 文档状态 */
    status?: string;
}
// 类型定义
export interface CollabTag {
    id: number;
    tagName: string;
    docCount: number
}

export interface DocCard {
    id: number;
    title: string;
    leadAuthor: string;
    lastUpdateDate: string;
    excerpt: string;
    tags: string[];
    collaborators: {
        name: string;
        avatar?: string;
    }[];
    views: number;
    editCount: number;
    version: number;
    isEditing: boolean;
    comments: number;
}
