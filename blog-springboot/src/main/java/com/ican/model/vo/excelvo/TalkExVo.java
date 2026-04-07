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
 * @name：TalkExVo
 * @Date：2024/10/25 11:26
 * @Filename：TalkExVo
 */
@Data
@HeadRowHeight(25)
@ContentRowHeight(26)
@EqualsAndHashCode
@ColumnWidth(15)
@Builder
public class TalkExVo {
    /**
     * 说说作者id
     */
    @ExcelProperty(value = "说说用户", index = 0)
    private Integer id;

    /**
     * 说说内容
     */
    @ExcelProperty(value = "说说内容", index = 1)
    @ColumnWidth(60) // 设置列宽（可以根据需要调整宽度）
    private String TalkContent;

    /**
     * 是否置顶 (0否 1是)
     */
    @ExcelProperty(value = "是否置顶", index = 2)
    private String isTop;

    /**
     * 状态 (1公开 2私密 )
     */
    @ExcelProperty(value = "说说状态", index = 3)
    private String status;

    /**
     * 文章缩略图
     */
    @ExcelProperty(value = "文章缩略图", index = 4)
    @ColumnWidth(90) // 设置列宽（可以根据需要调整宽度）
    private String TalkCover;

}
