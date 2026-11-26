package com.ican.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ican.entity.AiTask;
import com.ican.model.dto.AiTaskQueryDTO;
import com.ican.model.vo.AiTaskBackVO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AI任务 Mapper
 */
@Repository
public interface AiTaskMapper extends BaseMapper<AiTask> {

    List<Integer> selectDuePendingTasks(@Param("size") int size);

    int reserveDispatch(@Param("taskId") Integer taskId);

    int claimPendingTask(@Param("taskId") Integer taskId, @Param("token") String token);

    int saveGeneratedPayload(@Param("taskId") Integer taskId, @Param("token") String token,
                             @Param("payload") String payload);

    int completeExecution(@Param("task") AiTask task, @Param("token") String token);

    int resetFailedTask(@Param("taskId") Integer taskId, @Param("retryCount") int retryCount);

    AiTask lockExecution(@Param("taskId") Integer taskId, @Param("token") String token);

    int renewExecution(@Param("taskId") Integer taskId, @Param("token") String token);

    List<Integer> selectRecoverableTasks(@Param("size") int size);

    int expireExecution(@Param("taskId") Integer taskId);

    int resumeCheckpointTask(@Param("taskId") Integer taskId);

    List<java.util.Map<String, Object>> selectTaskBacklog();

    Long countTaskBackVO(@Param("condition") AiTaskQueryDTO condition);

    List<AiTaskBackVO> selectTaskBackVO(@Param("limit") Long limit,
                                        @Param("size") Long size,
                                        @Param("condition") AiTaskQueryDTO condition);
}
