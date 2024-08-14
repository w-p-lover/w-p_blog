package com.ican.service;

import javax.servlet.http.HttpServletResponse;

/**
 * @Author：yep
 * @Project：blog
 * @name：DictService
 * @Date：2024/8/14 14:56
 * @Filename：DictService
 */
public interface DictService {
    void exportData(HttpServletResponse response);
}
