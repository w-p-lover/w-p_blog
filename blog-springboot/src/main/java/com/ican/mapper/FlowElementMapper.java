package com.ican.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ican.entity.FlowElement;
import com.ican.model.dto.ConditionDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FlowElementMapper extends BaseMapper<FlowElement> {

    @Select("SELECT * FROM t_flow_element")
    List<FlowElement> getElementList();

    @Select("SELECT count(*) FROM t_flow_element WHERE id = #{id}")
    int getTotalCount(@Param("condition") ConditionDTO condition);
}
