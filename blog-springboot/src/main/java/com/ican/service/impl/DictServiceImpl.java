package com.ican.service.impl;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.lang.intern.InternUtil;
import com.alibaba.excel.EasyExcel;
import com.ican.entity.Article;
import com.ican.mapper.ArticleMapper;
import com.ican.model.vo.DictEeVo;
import com.ican.service.DictService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author：yep
 * @Project：blog
 * @name：DictServiceImpl
 * @Date：2024/8/14 14:56
 * @Filename：DictServiceImpl
 */
@Service
public class DictServiceImpl implements DictService {

    @Autowired
    private ArticleMapper articleMapper;

    @Override
    public void exportData(HttpServletResponse response) {
        try {
            //设置相关参数
            response.setContentType("application/vnd.ms-excel");
            response.setCharacterEncoding("UTF-8");
            // 这里URLEncoder.encode可以防止中文乱码 当然和easyexcel没有关系
            String fileName = URLEncoder.encode("数据字典", StandardCharsets.UTF_8);
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
            //获取文件
            List<Article> list = articleMapper.selectList(null);
            //转换文件
            ArrayList<DictEeVo> dictEeVos = new ArrayList<>();
            for (Article article : list) {
                DictEeVo dictEeVo = new DictEeVo();
                //转换
                BeanUtils.copyProperties(article, dictEeVo);
                dictEeVo.setArticleContent("测试一下");
                dictEeVo.setIsTop(article.getIsTop().equals(1) ? "置顶" : "未置顶");
                dictEeVo.setIsRecommend(article.getIsRecommend().equals(1) ? "推荐" : "未推荐");
                dictEeVo.setStatus(article.getStatus().equals(1) ? "公开"
                        : article.getStatus().equals(2) ? "私人" : "草稿");
                dictEeVo.setArticleType(article.getArticleType().equals(1) ? "原创"
                        : article.getArticleType().equals(2) ? "转载" : "翻译");
                //添加
                dictEeVos.add(dictEeVo);
            }
            //写出
            ServletOutputStream outputStream = response.getOutputStream();
            EasyExcel.write(outputStream, DictEeVo.class).sheet("数据字典").doWrite(dictEeVos);
            outputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
