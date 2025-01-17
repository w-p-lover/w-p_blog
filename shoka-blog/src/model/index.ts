/**
 * 分页返回接口
 */
export interface PageResult<T> {
    /**
     * 分页结果
     */
    recordList: T;
    /**
     * 总数
     */
    count: number;
}

/**
 * 结果返回接口
 */
export interface Result<T> {
    /**
     * 返回状态
     */
    flag: boolean;
    /**
     * 状态码
     */
    code: number;
    /**
     * 返回信息
     */
    msg: string;
    /**
     * 返回数据
     */
    data: T;
}

export interface PageQueryArticle {
    /**
     * 当前页
     */
    current: number;
    /**
     * 每页大小
     */
    size: number;
    /**
     * 排序方式
     */
    sort?: string;    // 排序方式，? 表示可选
    /**
     * 时间选择
     */
    start: any;

    end: any;
    /**
     * 标签ID查询
     */
    tagId: any;
}

/**
 * 分页参数
 */
export interface PageQuery {
    /**
     * 当前页
     */
    current: number;
    /**
     * 每页大小
     */
    size: number;
}

/**
 * 用户信息
 */
export interface UserForm {
    /**
     * 用户名
     */
    username: string;
    /**
     * 密码
     */
    password: string;
    /**
     * 验证码
     */
    code: string;
}
