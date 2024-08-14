import {AxiosPromise} from "axios";
import request from "@/utils/request";

export function getData(): AxiosPromise<null> {
    return request({
        url: "/admin/exportData",
        method: "get",
        responseType: "arraybuffer"
    });
}
