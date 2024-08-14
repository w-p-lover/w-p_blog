package com.ican.utils;

import com.alibaba.excel.EasyExcel;
import com.ican.model.vo.DemoData;

import java.util.ArrayList;

/**
 * @Author：yep
 * @Project：blog
 * @name：ExcelUtils
 * @Date：2024/8/12 10:09
 * @Filename：ExcelUtils
 */
public class ExcelUtils {
    public static void simpleExcel() {
        String outPath = "out.xlsx";
        EasyExcel.write(outPath, DemoData.class)
                .sheet("导出数据")
                .doWrite(new ArrayList<>());
    }

}
