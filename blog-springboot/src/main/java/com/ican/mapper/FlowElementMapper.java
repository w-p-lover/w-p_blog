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

    /**
     * 查询所有流程元素
     * @return 流程元素列表
     */
    @Select("SELECT * FROM t_flow_element")
    List<FlowElement> getElementList();

    /**
     * 查询流程元素数量
     *
     * @param condition 条件
     * @return 数量
     */
    @Select("SELECT count(*) FROM t_flow_element WHERE id = #{id}")
    int getTotalCount(@Param("condition") ConditionDTO condition);
}
