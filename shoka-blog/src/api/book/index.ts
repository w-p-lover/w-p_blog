import {BookQuery, PageResult, Result} from "@/model";
import request from "@/utils/request";
import { AxiosPromise } from "axios";
import { BookVO, BookForm, BookSearch,  Resource } from "./type";

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
        url: "/book",
        method: "put",
        data,
    });
}

/**
 * 删除书籍
 * @param bookId 书籍ID
 */
export function deleteBook(bookId: number): AxiosPromise<Result<null>> {
    return request({
        url: `/book/${bookId}`,
        method: "delete",
    });
}

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
 * 上传书籍图片
 * 返回图片链接后写入书籍 cover/briefImg 字段。
 */
export function uploadBookImage(data: FormData): AxiosPromise<Result<string>> {
    return request({
        url: "/book/upload",
        headers: {"content-type": "multipart/form-data"},
        method: "post",
        data,
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
