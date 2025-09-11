import {PageQuery, PageResult, Result} from "@/model";
import request from "@/utils/request";
import {AxiosPromise} from "axios";
import {Game} from "@/api/game/types";

/**
 * 查看游戏列表
 * @param params 查询条件
 * @returns 游戏列表
 */
export function getGameList(params: PageQuery): AxiosPromise<Result<PageResult<Game[]>>> {
    return request({
        url: "/games/list",
        method: "get",
        params,
    });
}
