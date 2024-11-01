package com.ican.model.vo.excelvo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author：yep
 * @Project：blog
 * @name：VisitExVo
 * @Date：2024/10/25 13:11
 * @Filename：VisitExVo
 */
@Data
@HeadRowHeight(25)
@ContentRowHeight(26)
@EqualsAndHashCode
@ColumnWidth(15)
@Builder
@ApiModel(description = "访问记录导出")
public class VisitExVo {
    /**
     * 访问页
     */
    @ExcelProperty(value = "访问页", index = 0)
    @ApiModelProperty(value = "访问页")
    private String page;

    /**
     * 操作系统
     */
    @ExcelProperty(value = "操作系统", index = 1)
    @ColumnWidth(50) // 设置列宽（可以根据需要调整宽度）
    @ApiModelProperty(value = "操作系统")
    private String os;

    /**
     * 浏览器
     */
    @ExcelProperty(value = "浏览器", index = 2)
    @ColumnWidth(50) // 设置列宽（可以根据需要调整宽度）
    @ApiModelProperty(value = "浏览器")
    private String browser;

    /**
     * IP地址
     */
    @ExcelProperty(value = "IP地址", index = 3)
    @ColumnWidth(50) // 设置列宽（可以根据需要调整宽度）
    @ApiModelProperty(value = "IP地址")
    private String ipAddress;

    /**
     * IP来源
     */
    @ExcelProperty(value = "IP来源", index = 4)
    @ColumnWidth(50) // 设置列宽（可以根据需要调整宽度）
    @ApiModelProperty(value = "IP来源")
    private String IpSource;

    /**
     * 访问时间
     */
    @ExcelProperty(value = "访问时间", index = 5)
    @ColumnWidth(50) // 设置列宽（可以根据需要调整宽度）
    @ApiModelProperty(value = "访问时间")
    private String createTime;
}
