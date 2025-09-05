package com.ican.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ican.model.dto.ConditionDTO;
import com.ican.model.vo.GameVO;
import com.ican.model.vo.GiteeTrendingVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.ican.entity.Game;
import java.util.List;

@Mapper
public interface GameMapper extends BaseMapper<Game> {

    List<Game> getGameList(
            @Param("limit") Long limit,
            @Param("size") Long size,
            @Param("condition") ConditionDTO condition
    );

    @Select("SELECT COUNT(*) FROM t_game")
    int getTotalCount();
}
