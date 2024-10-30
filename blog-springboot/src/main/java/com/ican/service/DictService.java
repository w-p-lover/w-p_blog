package com.ican.service;

import com.ican.model.dto.ExcelDTO;

import javax.servlet.http.HttpServletResponse;

/**
 * @Author：yep
 * @Project：blog
 * @name：DictService
 * @Date：2024/8/14 14:56
 * @Filename：DictService
 */
public interface DictService {
    /**
     * 后端导出文章数据，前端下载excel文件
     *
     * @param response
     * @param excelDTO
     */
    void exportArticle(HttpServletResponse response, ExcelDTO excelDTO);

    /**
     * 后端导出文章数据，前端下载excel文件
     * @param response
     */
    void exportMessage(HttpServletResponse response);

    /**
     * 后端导出文章数据，前端下载excel文件
     * @param response
     */
    void exportTalk(HttpServletResponse response);

    /**
     * 后端导出文章数据，前端下载excel文件
     * @param response
     */
    void exportUser(HttpServletResponse response);

    /**
     * 后端导出文章数据，前端下载excel文件
     * @param response
     */
    void exportVisit(HttpServletResponse response);
}
