

export interface DocVersionDTO {
    /**
     * 文档ID
     * 后端会从URL路径（/docs/{docId}/versions）中覆盖该值，前端可传可不传（建议按需设为可选）
     */
    docId?: number;

    /**
     * 文档内容（必选）
     * 富文本HTML或纯文本，对应后端content字段，是版本核心数据
     */
    content: string;

    /**
     * 提交时的基线版本（主版本号，必选）
     * 对应后端baseMajor字段，用于版本迭代计算（如基线主版本2 → 新主版本3）
     */
    baseMajor: number;

    /**
     * 提交时的基线版本（次版本号，必选）
     * 对应后端baseMinor字段，用于版本迭代计算（如基线次版本3 → 新次版本4）
     */
    baseMinor: number;

    /**
     * 版本作者（可选）
     * 对应后端author字段，若前端已通过登录态获取用户信息，可传此值；若后端自动获取当前登录人，前端可省略
     */
    author?: string;

    /**
     * 版本描述（可选，扩展自Version接口）
     * 补充后端DTO未定义但前端需传递的「版本修改说明」（如“修复章节2格式错误”）
     */
    description?: string;
}


export interface DocVersion {

    /**
     * 所属文档ID（必选）
     * 对应后端docId字段，关联具体文档，避免跨文档版本混淆
     */
    docId: number;

    /**
     * 版本号（必选）
     * 对应后端version字段（如“2.3”），实现Version接口的version字段
     */
    version: string;

    /**
     * 版本作者（必选）
     * 对应后端author字段，映射Version接口的updater字段（前端展示“更新人”）
     */
    author: string;

    /**
     * 版本状态（必选）
     * 对应后端status字段（如“DRAFT-草稿”“PUBLISHED-已发布”“REJECTED-已驳回”），实现Version接口的status字段
     */
    status: string;

    /**
     * 版本创建时间（必选）
     * 对应后端createdAt字段（LocalDateTime类型），前端接收为ISO格式字符串（如“2024-05-20T14:30:00”），映射Version接口的updateTime字段
     */
    createdAt: string;

    /**
     * 版本描述（可选，实现Version接口）
     * 前端展示“版本修改说明”，若后端未返回，可前端默认填充“无描述”
     */
    description?: string;

    /**
     * 文档内容（可选，实现Version接口）
     * 用于前端预览历史版本内容，后端VO未定义时，可通过“版本详情接口”额外获取
     */
    content?: string;
}