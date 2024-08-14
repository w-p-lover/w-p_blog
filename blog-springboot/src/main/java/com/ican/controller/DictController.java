package com.ican.controller;

import com.ican.service.DictService;
import io.swagger.annotations.ApiOperation;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
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

    @ApiOperation("导出")
    @GetMapping("/admin/exportData")
    public void exportData(HttpServletResponse response){
        dictService.exportData(response);
    }
}
