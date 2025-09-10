import {PageQuery, PageResult, Result} from "@/model";
import request from "@/utils/request";
import {AxiosPromise} from "axios";
import {FlowNode} from "@/api/life/types";

/**
 * 查看文章归档
 * @returns 文章归档
 */
export function getFlowList(): AxiosPromise<FlowNode[]> {
    return request({
        url: "/flow/list",
        method: "get",
    });
}
