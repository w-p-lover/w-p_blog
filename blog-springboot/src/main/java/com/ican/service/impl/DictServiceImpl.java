package com.ican.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.ican.entity.Article;
import com.ican.entity.Message;
import com.ican.entity.Talk;
import com.ican.entity.User;
import com.ican.entity.VisitLog;
import com.ican.mapper.ArticleMapper;
import com.ican.mapper.MessageMapper;
import com.ican.mapper.TalkMapper;
import com.ican.mapper.UserMapper;
import com.ican.mapper.VisitLogMapper;
import com.ican.model.dto.ExcelDTO;
import com.ican.model.vo.excelvo.ArticleExVo;
import com.ican.model.vo.excelvo.MessageExVo;
import com.ican.model.vo.excelvo.TalkExVo;
import com.ican.model.vo.excelvo.UserExVo;
import com.ican.model.vo.excelvo.VisitExVo;
import com.ican.service.DictService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private TalkMapper talkMapper;
    @Autowired
    private MessageMapper messageMapper;
    @Autowired
    private VisitLogMapper visitLogMapper;

    @Override
    public void exportArticle(HttpServletResponse response, ExcelDTO excelDTO) {
        try {
            try (ServletOutputStream outputStream = response.getOutputStream()) {
                // 设置相关参数
                response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                response.addHeader("Access-Control-Expose-Headers", "Content-Disposition");
                ExcelWriter excelWriter = EasyExcel.write(outputStream).build();
                List<String> sheet = excelDTO.getSheet();
                Map<List<Object>, Class<?>> sheetData = new HashMap<>();
                for (String s : sheet) {
                    switch (s) {
                        case "article":
                            // 获取数据
                            List<Article> list = articleMapper.selectList(null);
                            List<ArticleExVo> dictEeVos = new ArrayList<>();
                            for (Article article : list) {
                                ArticleExVo dictEeVo = new ArticleExVo();
                                BeanUtils.copyProperties(article, dictEeVo);
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
                            WriteSheet sheet1 = EasyExcel.writerSheet(0, "article").head(ArticleExVo.class).build();
                            excelWriter.write(dictEeVos, sheet1);
                            break;
                        case "talk":
                            List<Talk> talks = talkMapper.selectList(null);
                            List<TalkExVo> talkExVos = new ArrayList<>();
                            for (Talk talk : talks) {
                                TalkExVo talkExVo = TalkExVo.builder()
                                        .TalkContent(talk.getTalkContent())
                                        .TalkCover(talk.getImages())
                                        .id(talk.getId())
                                        .status(talk.getStatus().equals(1) ? "公开"
                                                : talk.getStatus().equals(2) ? "私人" : "草稿")
                                        .isTop(talk.getIsTop().equals(1) ? "置顶" : "未置顶")
                                        .build();
                                talkExVos.add(talkExVo);
                            }
                            WriteSheet sheet2 = EasyExcel.writerSheet(1, "talk").head(TalkExVo.class).build();
                            excelWriter.write(talkExVos, sheet2);
                            break;
                        case "message":
                            List<Message> messages = messageMapper.selectList(null);
                            List<MessageExVo> messageExVos = new ArrayList<>();
                            for (Message message : messages) {
                                MessageExVo messageExVo = MessageExVo.builder()
                                        .nickname(message.getNickname())
                                        .MesContent(message.getMessageContent())
                                        .ipAddress(message.getIpAddress())
                                        .IpSource(message.getIpSource())
                                        .createTime(String.valueOf(message.getCreateTime()))
                                        .build();
                                messageExVos.add(messageExVo);
                            }
                            WriteSheet sheet3 = EasyExcel.writerSheet(2, "message").head(MessageExVo.class).build();
                            excelWriter.write(messageExVos, sheet3);
                            break;
                        case "visitLog":
                            List<VisitLog> visitLogs = visitLogMapper.selectList(null);
                            List<VisitExVo> visitExVos = new ArrayList<>();
                            for (VisitLog visitLog : visitLogs) {
                                VisitExVo visitExVo = VisitExVo.builder()
                                        .os(visitLog.getOs())
                                        .page(visitLog.getPage())
                                        .browser(visitLog.getBrowser())
                                        .ipAddress(visitLog.getIpAddress())
                                        .IpSource(visitLog.getIpSource())
                                        .createTime(String.valueOf(visitLog.getCreateTime()))
                                        .build();
                                visitExVos.add(visitExVo);
                            }
                            WriteSheet sheet4 = EasyExcel.writerSheet(3, "visitLog").head(VisitExVo.class).build();
                            excelWriter.write(visitExVos, sheet4);
                            break;
                        case "user":
                            List<User> users = userMapper.selectList(null);
                            List<UserExVo> userExVos = new ArrayList<>();
                            for (User user : users) {
                                UserExVo userExVo = UserExVo.builder()
                                        .email(user.getEmail())
                                        .intro(user.getIntro())
                                        .loginTime(String.valueOf(user.getLoginTime()))
                                        .webSite(user.getWebSite())
                                        .username(user.getUsername())
                                        .nickname(user.getNickname())
                                        .createTime(String.valueOf(user.getCreateTime()))
                                        .IpSource(user.getIpSource())
                                        .ipAddress(user.getIpAddress())
                                        .build();
                                userExVos.add(userExVo);
                            }
                            WriteSheet sheet5 = EasyExcel.writerSheet(4, "user").head(UserExVo.class).build();
                            excelWriter.write(userExVos, sheet5);
                            break;
                    }
                }
                excelWriter.finish();
            }

            // 确保响应流被刷新
            response.flushBuffer();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void exportMessage(HttpServletResponse response) {

    }

    @Override
    public void exportTalk(HttpServletResponse response) {

    }

    @Override
    public void exportUser(HttpServletResponse response) {

    }

    @Override
    public void exportVisit(HttpServletResponse response) {

    }

}
