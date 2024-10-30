package com.ican.model.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author：yep
 * @Project：blog
 * @name：ExcelDTO
 * @Date：2024/10/25 14:36
 * @Filename：ExcelDTO
 */
@Data
@ApiModel(description = "Excel导出结构DTO")
public class ExcelDTO {
    private boolean articleInfo;
    private boolean sayInfo;
    private boolean messageInfo;
    private boolean userInfo;
    private boolean accessInfo;

    public List<String> getSheet() {
        List<String> sheetList = new ArrayList<>();
        if (this.accessInfo) sheetList.add("visitLog");
        if (this.articleInfo) sheetList.add("article");
        if (this.sayInfo) sheetList.add("talk");
        if (this.messageInfo) sheetList.add("message");
        if (this.userInfo) sheetList.add("user");
        return sheetList;
    }
}
