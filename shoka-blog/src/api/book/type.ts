// 后端返回的书籍视图对象
export interface BookVO {
    /** 书籍唯一标识 */
    id: number;
    /** 书籍标题 */
    title: string;
    /** 书籍作者（可选） */
    author?: string;
    /** 书籍状态：wish-想读, reading-在读, read-已读 */
    status: "wish" | "reading" | "read";
    /** 添加时间（时间戳或字符串格式） */
    addTime: string;
}

// 新增/修改书籍表单
export interface BookForm {
    /** 书籍标题 */
    title: string;
    /** 书籍作者（可选） */
    author: string;
    /** 书籍状态：wish-想读, reading-在读, read-已读 */
    status: "wish" | "reading" | "read";
    /** 封面URL */
    cover: '',
    /** 标签列表 */
    tags: '',
}

// 搜索结果
export interface BookSearch {
    /** 书籍唯一标识 */
    id: number;
    /** 书籍标题 */
    title: string;
    /** 书籍作者（可选） */
    author?: string;
}
