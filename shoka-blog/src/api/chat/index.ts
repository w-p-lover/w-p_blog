import {ChatMessage,Friend} from "@/api/chat/type";
import { Result } from "@/model";
import request from "@/utils/request";
import { AxiosPromise } from "axios";

/**
 * 获取好友列表
 * @returns 好友列表
 */
export function getFriendList(userId: number): AxiosPromise<Result<Friend[]>> {
    return request({
        url: `/chat/friendList/${userId}`,
        method: "post",
    });
}

export function getCurrentUserUid(): AxiosPromise<any> {
    return request({
        url: "/chat/getUserUid",
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
        url: "/chat/chatMsg",
        method: "post",
        data: params,
    });
}