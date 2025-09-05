import {PageQuery, PageResult, Result} from "@/model";
import request from "@/utils/request";
import {AxiosPromise} from "axios";
import {Game} from "@/api/game/types";

/**
 * 查看文章归档
 * @param params 查询条件
 * @returns 文章归档
 */
export function getGameList(params: PageQuery): AxiosPromise<Result<PageResult<Game[]>>> {
    return request({
        url: "/games/list",
        method: "get",
        params,
    });
}
