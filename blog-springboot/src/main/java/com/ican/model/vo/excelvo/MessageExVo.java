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
 * @name：MessageExVo
 * @Date：2024/10/25 11:26
 * @Filename：MessageExVo
 */
@Data
@HeadRowHeight(25)
@ContentRowHeight(26)
@EqualsAndHashCode
@ColumnWidth(15)
@Builder
@ApiModel(description = "留言导出")
public class MessageExVo {
    /**
     * 留言人
     */
    @ExcelProperty(value = "留言人", index = 0)
    @ApiModelProperty(value = "留言人")
    private String nickname;

    /**
     * 留言内容
     */
    @ExcelProperty(value = "留言内容", index = 1)
    @ApiModelProperty(value = "留言内容")
    @ColumnWidth(60) // 设置列宽（可以根据需要调整宽度）
    private String MesContent;

    /**
     * IP地址
     */
    @ExcelProperty(value = "IP地址", index = 2)
    @ColumnWidth(50) // 设置列宽（可以根据需要调整宽度）
    @ApiModelProperty(value = "IP地址")
    private String ipAddress;

    /**
     * IP来源
     */
    @ExcelProperty(value = "IP来源", index = 3)
    @ColumnWidth(50) // 设置列宽（可以根据需要调整宽度）
    @ApiModelProperty(value = "IP来源")
    private String IpSource;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间", index = 4)
    @ColumnWidth(50) // 设置列宽（可以根据需要调整宽度）
    @ApiModelProperty(value = "创建时间")
    private String createTime;
}
