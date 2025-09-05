import { PageResult, Result } from "@/model";
import request from "@/utils/request";
import { AxiosPromise } from "axios";
import { GameVO, GameForm, GameQuery } from "./types";

/**
 * 获取游戏列表
 * @param params 查询条件
 */
export function getGameList(params: GameQuery): AxiosPromise<Result<PageResult<GameVO[]>>> {
    return request({
        url: "/games/admin/list",
        method: "get",
        params,
    });
}

/**
 * 获取单个游戏详情
 * @param gameId 游戏ID
 */
export function getGame(gameId: number): AxiosPromise<Result<GameVO>> {
    return request({
        url: `/games/admin/${gameId}`,
        method: "get",
    });
}

/** 搜索游戏 */
export function searchGame(keyword: string): AxiosPromise<Result<GameVO[]>> {
    return request({
        url: "/games/admin/search",
        method: "get",
        params: { keyword },
    });
}
/**
 * 新增游戏
 */
export function addGame(data: GameForm): AxiosPromise<Result<null>> {
    return request({
        url: "/games/admin/add",
        method: "post",
        data,
    });
}

/**
 * 更新游戏
 */
export function updateGame(data: GameForm & { id: number }): AxiosPromise<Result<null>> {
    return request({
        url: "/games/admin/update",
        method: "put",
        data,
    });
}

/**
 * 删除游戏
 */
export const deleteGameBatch = (ids: number[]) => {
    return request({
        url: "/games/admin/delete",
        method: "delete",
        data: ids,
    });
};
