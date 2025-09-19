import { AxiosPromise } from "axios";
import request from "@/utils/request";
import {PageQuery, Result} from "@/model";
import {CollabTag, Doc, DocDTO} from "./type";

/**
 * 获取文档详情
 * @param id 文档ID
 */
export function getDoc(id: number): AxiosPromise<Result<Doc>> {
    return request({
        url: `/docs/${id}`,
        method: "get",
    });
}

/**
 * 获取文档列表
 */
export function listDocs(params: PageQuery): AxiosPromise<Result<Doc[]>> {
    return request({
        url: "/docs/list",
        method: "get",
        params,
    });
}

/**
 * 获取文档列表
 */
export function getDocCount(): AxiosPromise<Result<number>> {
    return request({
        url: "/docs/collab/count",
        method: "get",
    });
}

/**
 * 获取文档列表
 */
export function getUserFavorites(userId: number): AxiosPromise<Result<number[]>> {
    return request({
        url: "/docs/collab/favorites",
        method: "get",
        params: { userId }
    });
}

/**
 * 创建文档
 * @param data 文档数据
 */
export function createDoc(data: DocDTO): AxiosPromise<Result<null>> {
    return request({
        url: "/docs/collab/create",
        method: "post",
        data,
    });
}

/**
 * 更新文档
 * @param data 文档数据
 */
export function updateDoc(data: DocDTO): AxiosPromise<Result<null>> {
    return request({
        url: "/docs/collab/update",
        method: "put",
        data,
    });
}

/**
 * 添加收藏
 */
export function addFavorite(userId: number, docId: number): AxiosPromise<Result<null>> {
    return request({
        url: "/docs/collab/addFavorite",
        method: "put",
        params: {  userId, docId }
    });
}

/**
 * 取消收藏
 */
export function cancelFavorite(userId: number, docId: number): AxiosPromise<Result<null>> {
    return request({
        url: "/docs/collab/cancelFavorite",
        method: "delete",
        params: { userId, docId }
    });
}

/**
 * 获取文档标签
 */
export function getDocTags(): AxiosPromise<Result<CollabTag[]>> {
    return request({
        url: "/docs/collab/tags",
        method: "get",
    });
}

/**
 * 删除文档
 * @param id 文档ID
 */
export function deleteDoc(id: number): AxiosPromise<Result<null>> {
    return request({
        url: `/docs/${id}`,
        method: "delete",
    });
}
