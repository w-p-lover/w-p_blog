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

    Long countTaskBackVO(@Param("condition") AiTaskQueryDTO condition);

    List<AiTaskBackVO> selectTaskBackVO(@Param("limit") Long limit,
                                        @Param("size") Long size,
                                        @Param("condition") AiTaskQueryDTO condition);
}
