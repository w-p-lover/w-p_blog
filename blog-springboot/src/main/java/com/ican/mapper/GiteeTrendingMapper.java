package com.ican.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ican.entity.Chat;
import com.ican.entity.GiteeTrending;
import com.ican.model.dto.ConditionDTO;
import com.ican.model.vo.GiteeTrendingVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Repository
public interface GiteeTrendingMapper extends BaseMapper<GiteeTrending> {

    /**
     * 分页查询 Gitee Trending 仓库
     *
     * @param limit    偏移量
     * @param size     每页大小
     * @param condition 条件
     * @return Gitee Trending 列表
     */
    List<GiteeTrendingVO> selectGiteeTrendingVOList(
            @Param("limit") Long limit,
            @Param("size") Long size,
            @Param("condition") ConditionDTO condition
    );
    Long countGiteeTrending(@Param("condition") ConditionDTO condition);

    @Select("SELECT DISTINCT sub_category FROM t_gitee_trending")
    List<String> countGiteeTrendingTypes();

    @Select("SELECT DISTINCT language FROM t_gitee_trending WHERE language != '' ")
    List<String> countGiteeTrendingLang();
}
