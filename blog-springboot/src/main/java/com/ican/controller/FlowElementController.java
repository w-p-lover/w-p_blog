package com.ican.controller;

import com.ican.model.dto.FlowElementDTO;
import com.ican.model.vo.FlowElementVO;
import com.ican.service.FlowElementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/flow")
public class FlowElementController {

    @Autowired
    private FlowElementService flowElementService;
    /**
     * 获取流程图列表
     *
     * @return 流程图列表
     */
    @GetMapping("/list")
    public List<FlowElementVO> list() {
        return flowElementService.getElementList();
    }
    /**
     * 添加流程图
     *
     * @param dto 流程图信息
     */
    @PostMapping("/add")
    public void add(@RequestBody FlowElementDTO dto) {
        flowElementService.addElement(dto);
    }
    /**
     * 修改流程图
     *
     * @param dto 流程图信息
     */
    @PutMapping("/update")
    public void update(@RequestBody FlowElementDTO dto) {
        flowElementService.updateElement(dto);
    }
    /**
     * 删除流程图
     *
     * @param ids ids
     */
    @DeleteMapping("/delete")
    public void delete(@RequestBody List<Long> ids) {
        flowElementService.deleteElementBatch(ids);
    }
}
