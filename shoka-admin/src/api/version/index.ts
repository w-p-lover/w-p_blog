import { Result } from "@/model";
import request from "@/utils/request";
import { AxiosPromise } from "axios";
import { DocVersionDTO, DocVersion } from "./types";

/**
 * 提交文档发布
 * @param docId 文档ID
 * @param data 文档版本数据
 */
export function submitForPublish(docId: number, data: DocVersion): AxiosPromise<Result<null>> {
    return request({
        url: `/docs/${docId}/versions/submit`,
        method: "post",
        data,
    });
}

/**
 * 获取文档版本历史
 * @param docId 文档ID
 */
export function listHistory(docId: number): AxiosPromise<Result<DocVersion[]>> {
    return request({
        url: `/docs/${docId}/versions/history`,
        method: "get",
    });
}
