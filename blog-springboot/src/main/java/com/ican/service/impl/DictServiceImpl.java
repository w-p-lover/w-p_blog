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
            // 设置相关参数
            /*String fileName = URLEncoder.encode("数据字典", "UTF-8").replaceAll("\\+", "%20");
*/            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            /*response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + fileName + ".xlsx");
*/            response.addHeader("Access-Control-Expose-Headers", "Content-Disposition");

            // 获取数据
            List<Article> list = articleMapper.selectList(null);
            ArrayList<DictEeVo> dictEeVos = new ArrayList<>();
            for (Article article : list) {
                DictEeVo dictEeVo = new DictEeVo();
                BeanUtils.copyProperties(article, dictEeVo);

                // 处理文章内容，保证长度不超过50
                String articleContent = article.getArticleContent();
                if (articleContent != null && articleContent.length() > 50) {
                    articleContent = articleContent.substring(0, 50).replace("\n", ""); // 截取前50个字符
                }
                dictEeVo.setArticleContent(articleContent);

                dictEeVo.setIsTop(article.getIsTop().equals(1) ? "置顶" : "未置顶");
                dictEeVo.setIsRecommend(article.getIsRecommend().equals(1) ? "推荐" : "未推荐");
                dictEeVo.setStatus(article.getStatus().equals(1) ? "公开"
                        : article.getStatus().equals(2) ? "私人" : "草稿");
                dictEeVo.setArticleType(article.getArticleType().equals(1) ? "原创"
                        : article.getArticleType().equals(2) ? "转载" : "翻译");

                dictEeVos.add(dictEeVo);
            }

            // 使用 EasyExcel 写出数据
            try (ServletOutputStream outputStream = response.getOutputStream()) {
                EasyExcel.write(outputStream, DictEeVo.class).sheet("数据字典").doWrite(dictEeVos);
            }

            // 确保响应流被刷新
            response.flushBuffer();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
