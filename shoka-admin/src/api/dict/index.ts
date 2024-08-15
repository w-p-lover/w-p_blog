import { AxiosPromise } from 'axios';
import request from '@/utils/request';

export function getData(): AxiosPromise<Blob> {
    return request({
        url: '/admin/exportData',
        method: 'get',
        responseType: 'blob',
    });
}
