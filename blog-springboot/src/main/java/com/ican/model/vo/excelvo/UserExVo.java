package com.ican.model.vo.excelvo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author：yep
 * @Project：blog
 * @name：UserExVo
 * @Date：2024/10/25 13:10
 * @Filename：UserExVo
 */
@Data
@HeadRowHeight(25)
@ContentRowHeight(26)
@EqualsAndHashCode
@ColumnWidth(15)
@Builder
public class UserExVo {
    /**
     * 昵称
     */
    @ExcelProperty(value = "昵称", index = 0)
    private String nickname;

    /**
     * 用户名
     */
    @ExcelProperty(value = "用户名", index = 1)
    private String username;

    /**
     * 个人网站
     */
    @ExcelProperty(value = "个人网站", index = 2)
    @ColumnWidth(50) // 设置列宽（可以根据需要调整宽度）
    private String webSite;

    /**
     * 个人简介
     */
    @ExcelProperty(value = "个人简介", index = 3)
    @ColumnWidth(50) // 设置列宽（可以根据需要调整宽度）
    private String intro;

    /**
     * 邮箱
     */
    @ExcelProperty(value = "邮箱", index = 4)
    @ColumnWidth(50) // 设置列宽（可以根据需要调整宽度）
    private String email;
    /**
     * IP地址
     */
    @ExcelProperty(value = "IP地址", index = 5)
    @ColumnWidth(50) // 设置列宽（可以根据需要调整宽度）
    private String ipAddress;

    /**
     * IP来源
     */
    @ExcelProperty(value = "IP来源", index = 6)
    @ColumnWidth(50) // 设置列宽（可以根据需要调整宽度）
    private String IpSource;

    /**
     * 最后登录时间
     */
    @ExcelProperty(value = "最后登录时间", index = 7)
    @ColumnWidth(50) // 设置列宽（可以根据需要调整宽度）
    private String loginTime;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间", index = 8)
    @ColumnWidth(50) // 设置列宽（可以根据需要调整宽度）
    private String createTime;
}
