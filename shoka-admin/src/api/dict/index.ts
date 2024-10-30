import { AxiosPromise } from 'axios';
import request from '@/utils/request';

export function getData(data: any): AxiosPromise<Blob> {
    return request({
        url: '/admin/exportData',
        method: 'get',
        params: data, // 使用 params 来传递查询参数
        responseType: 'blob', // 这里指定响应类型为 blob
    });
}
