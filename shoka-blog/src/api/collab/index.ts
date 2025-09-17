import { AxiosPromise } from "axios";
import request from "@/utils/request";
import {PageQuery, Result} from "@/model";
import { Doc, DocDTO } from "./type";

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
 * 创建文档
 * @param data 文档数据
 */
export function createDoc(data: DocDTO): AxiosPromise<Result<null>> {
    return request({
        url: "/docs/create",
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
        url: "/docs/update",
        method: "put",
        data,
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
