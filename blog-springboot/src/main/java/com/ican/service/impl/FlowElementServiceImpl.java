package com.ican.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ican.entity.FlowElement;
import com.ican.mapper.FlowElementMapper;
import com.ican.model.dto.ConditionDTO;
import com.ican.model.dto.FlowElementDTO;
import com.ican.model.vo.FlowElementVO;
import com.ican.service.FlowElementService;
import com.ican.utils.BeanCopyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.sql.Array;
import java.util.*;

@Service
public class FlowElementServiceImpl extends ServiceImpl<FlowElementMapper, FlowElement>
        implements FlowElementService {

    @Autowired
    private FlowElementMapper flowElementMapper;

    /**
     * 获取流程图元素列表（分页）
     *
     * @return 包含流程图元素列表及总数的Map对象，key为"recordList"和"count"
     */
    @Override
    public List<FlowElementVO> getElementList() {
        List<FlowElementVO> list = new ArrayList<>();
        // 查询符合条件的流程图元素列表（分页）
        List<FlowElement> elements = flowElementMapper.getElementList();
        // 将实体对象转换为VO对象，并处理关键点字段
        for (FlowElement element : elements) {
            FlowElementVO vo = BeanCopyUtils.copyBean(element, FlowElementVO.class);
            if (element.getKeyPoints() != null) vo.setKeyPoints(Arrays.asList(element.getKeyPoints().split(",")));
            list.add(vo);
        }
        return list;
    }

    /**
     * 根据ID获取流程图元素详情
     *
     * @param id 流程图元素ID
     * @return 流程图元素VO对象
     */
    @Override
    public FlowElementVO getElementById(Long id) {
        FlowElement element = flowElementMapper.selectById(id);
        FlowElementVO vo = BeanCopyUtils.copyBean(element, FlowElementVO.class);
        // 处理关键点字段
        if (element.getKeyPoints() != null) vo.setKeyPoints(Arrays.asList(element.getKeyPoints().split(",")));
        return vo;
    }

    /**
     * 添加流程图元素
     *
     * @param elementDTO 流程图元素数据传输对象
     */
    @Override
    public void addElement(FlowElementDTO elementDTO) {
        FlowElement element = BeanCopyUtils.copyBean(elementDTO, FlowElement.class);
        // 处理关键点字段，将其转换为逗号分隔字符串存储
        if (elementDTO.getKeyPoints() != null) element.setKeyPoints(String.join(",", elementDTO.getKeyPoints()));
        flowElementMapper.insert(element);
    }

    /**
     * 更新流程图元素
     *
     * @param elementDTO 流程图元素数据传输对象
     */
    @Override
    public void updateElement(FlowElementDTO elementDTO) {
        FlowElement element = BeanCopyUtils.copyBean(elementDTO, FlowElement.class);
        // 设置关键点字段
        if (elementDTO.getKeyPoints() != null) element.setKeyPoints(elementDTO.getKeyPoints());
        flowElementMapper.updateById(element);
    }

    /**
     * 批量删除流程图元素
     *
     * @param ids 要删除的流程图元素ID列表
     */
    @Override
    public void deleteElementBatch(List<Long> ids) {
        flowElementMapper.deleteBatchIds(ids);
    }
}
