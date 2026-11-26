package com.ican.consumer;

import cn.hutool.json.JSONUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.ican.model.dto.CanalDTO;
import com.ican.service.ElasticsearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.ican.constant.ElasticConstant.*;
import static com.ican.constant.MqConstant.*;

/**
 * 文章消费者
 *
 * @author xcs
 **/
@Component
@RequiredArgsConstructor
@ConditionalOnBean(ElasticsearchClient.class)
public class ArticleConsumer {

    private final ElasticsearchService elasticsearchService;

    @RabbitListener(queues = ARTICLE_QUEUE, containerFactory = "articleSearchListenerContainerFactory")
    public void listenSaveArticle(Message message) {
        String data = new String(message.getBody(), StandardCharsets.UTF_8);
        CanalDTO canalDTO = JSONUtil.toBean(data, CanalDTO.class);
        if (Boolean.TRUE.equals(canalDTO.getIsDdl()) || !"t_article".equals(canalDTO.getTable())) {
            return;
        }
        if (!INSERT.equals(canalDTO.getType()) && !UPDATE.equals(canalDTO.getType())
                && !DELETE.equals(canalDTO.getType())) {
            return;
        }
        if (canalDTO.getData() == null || canalDTO.getData().isEmpty()) {
            throw new IllegalArgumentException("文章变更消息缺少数据行");
        }
        // 先校验整批主键，消息只作为同步提示，不使用其中可能过期的正文。
        List<Integer> articleIds = new ArrayList<>();
        for (Map<String, Object> row : canalDTO.getData()) {
            Integer articleId = Integer.valueOf(String.valueOf(row == null ? null : row.get("id")));
            if (articleId <= 0) {
                throw new IllegalArgumentException("文章变更消息主键无效");
            }
            articleIds.add(articleId);
        }
        for (Integer articleId : articleIds) {
            // 任一行失败都向上传递，让整条消息进入重试和故障转存。
            elasticsearchService.syncArticle(articleId);
        }
    }
}