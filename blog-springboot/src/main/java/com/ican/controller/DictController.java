package com.ican.controller;

import com.ican.model.dto.ExcelDTO;
import com.ican.service.DictService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

/**
 * @Author：yep
 * @Project：blog
 * @name：DictController
 * @Date：2024/8/14 14:55
 * @Filename：DictController
 */
@RestController
public class DictController {

    @Autowired
    private DictService dictService;

    /**
     * 导出文章列标
     *
     * @param response
     */
    @ApiOperation("导出")
    @GetMapping("/admin/exportData")
    public void exportData(ExcelDTO excelDTO, HttpServletResponse response) {
        dictService.exportArticle(response, excelDTO);
    }
}
