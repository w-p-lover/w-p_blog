package com.ican.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ican.entity.AiTask;
import com.ican.model.dto.AiTaskQueryDTO;
import com.ican.model.vo.AiTaskBackVO;
import com.ican.model.vo.PageResult;

/**
 * AI任务中心服务
 */
public interface AiTaskService extends IService<AiTask> {

    Integer createArticleTask(Integer articleId, String title, String content);

    PageResult<AiTaskBackVO> listTaskBackVO(AiTaskQueryDTO condition);

    void executeTask(Integer taskId);

    void retryTask(Integer taskId);
}
