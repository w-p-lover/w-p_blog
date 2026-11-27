// 后端返回的书籍视图对象
export interface BookVO {
    id: number;
    title: string;
    author?: string;
    status: "wish" | "reading" | "read";
    addTime: string;
    cover?: string;
    tags?: string;
    resource: Resource[];
    brief?: string;
    briefImg?: string;
}

export interface Resource {
    name: string;
    url: string;
    type: string;
}

// 新增/修改书籍表单
export interface BookForm {
    title: string;
    author: string;
    status: "wish" | "reading" | "read";
    cover: string;
    briefImg?: string;
    brief?: string;
    tags: string;
    resource?: string;
}

// 搜索结果
export interface BookSearch {
    id: number;
    title: string;
    author?: string;
}
