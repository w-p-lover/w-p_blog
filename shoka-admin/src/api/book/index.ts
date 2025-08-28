import {PageResult, Result , BookQuery} from "@/model";
import request from "@/utils/request";
import { AxiosPromise } from "axios";
import { BookVO, BookForm, BookSearch,  Resource } from "./types";

/**
 * 查看书籍列表
 * @param params 查询条件
 * @returns 书籍分页结果
 */
export function getBookList(params: BookQuery): AxiosPromise<Result<PageResult<BookVO[]>>> {
    return request({
        url: "/book/list",
        method: "get",
        params,
    });
}

/**
 * 查看单本书籍详情
 * @param bookId 书籍ID
 * @returns 书籍信息
 */
export function getBook(bookId: number): AxiosPromise<Result<BookVO>> {
    return request({
        url: `/book/${bookId}`,
        method: "get",
    });
}

/**
 * 新增书籍
 * @param data 新书籍表单
 */
export function addBook(data: BookForm): AxiosPromise<Result<null>> {
    return request({
        url: "/book/add",
        method: "post",
        data,
    });
}

/**
 * 更新书籍
 * @param data 修改的书籍表单（必须带id）
 */
export function updateBook(data: BookForm & { id: number }): AxiosPromise<Result<null>> {
    return request({
        url: "/admin/book/update",
        method: "put",
        data,
    });
}

/**
 * 删除书籍
 * @param ids 书籍ID
 */
export const deleteBookBatch = (ids: number[]) => {
    return request({
        url: "/admin/book/delete",
        method: "delete",
        data: ids // 直接发送数组
    });
};


/**
 * 修改书籍状态
 * @param bookId 书籍ID
 * @param status 新状态
 */
export function updateBookStatus(bookId: number, status: string): AxiosPromise<Result<null>> {
    return request({
        url: `/book/${bookId}/status`,
        method: "put",
        params: { status },
    });
}

/**
 * 搜索书籍
 * @param keyword 关键字
 */
export function searchBook(keyword: string): AxiosPromise<Result<BookSearch[]>> {
    return request({
        url: "/book/search",
        method: "get",
        params: { keyword },
    });
}

/**
 * 更新书源
 */
export const updateResource = (bookId: number, resources: Resource[]) => {
    return request({
        url: `/book/updateResource?bookId=${bookId}&resourceJson=${encodeURIComponent(JSON.stringify(resources))}`,
        method: "post"
    });
};




/**
 * 删除书源
 */
export const deleteResource = (bookId: number, index: number) => {
    return request({
        url: "/book/deleteResource",
        method:"post",
        params:{ bookId, index }
    });
};