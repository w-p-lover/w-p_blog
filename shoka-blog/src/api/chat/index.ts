import {ChatMessage,Friend} from "@/api/chat/type";
import { Result } from "@/model";
import request from "@/utils/request";
import { AxiosPromise } from "axios";

/**
 * 获取好友列表
 * @returns 好友列表
 */
export function getFriendList(): AxiosPromise<Result<Friend[]>> {
    return request({
        url: "/friend/friendList",
        method: "post",
    });
}

export function getCurrentUserUid(): AxiosPromise<any> {
    return request({
        url: "/friend/getUserUid",
        method: "get",
    });
}


/**
 * 获取聊天信息
 * @param params 查询条件
 * @returns 聊天信息
 */
export function getChatMessage(params: Record<any, any>): AxiosPromise<Result<ChatMessage[]>> {
    return request({
        url: "/friend/chatMsg",
        method: "post",
        data: params,
    });
}