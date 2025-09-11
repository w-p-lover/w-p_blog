import {PageQuery, PageResult, Result} from "@/model";
import request from "@/utils/request";
import {AxiosPromise} from "axios";
import {FlowNode} from "@/api/life/types";

/**
 * 查看流程卡片
 * @returns 流程卡片
 */
export function getFlowList(): AxiosPromise<FlowNode[]> {
    return request({
        url: "/flow/list",
        method: "get",
    });
}

/**
 * 新增流程卡片
 */
export function addFlowElement(data: FlowNode): AxiosPromise<Result<null>> {
    return request({
        url: "/flow/add",
        method: "post",
        data,
    });
}
