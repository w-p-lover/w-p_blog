package com.ican.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ican.entity.FlowElement;
import com.ican.model.dto.ConditionDTO;
import com.ican.model.dto.FlowElementDTO;
import com.ican.model.vo.FlowElementVO;

import java.util.List;
import java.util.Map;

public interface FlowElementService extends IService<FlowElement> {

    /**
     * 获取流程元素列表
     *
     * @return 流程元素列表
     */
    List<FlowElementVO> getElementList();

    /**
     * 根据id获取流程元素
     *
     * @param id id
     * @return 流程元素
     */
    FlowElementVO getElementById(Long id);

    /**
     * 添加流程元素
     *
     * @param elementDTO 流程元素信息
     */
    void addElement(FlowElementDTO elementDTO);

    /**
     * 修改流程元素
     *
     * @param elementDTO 流程元素信息
     */
    void updateElement(FlowElementDTO elementDTO);

    /**
     * 删除流程元素
     *
     * @param ids ids
     */
    void deleteElementBatch(List<Long> ids);
}
