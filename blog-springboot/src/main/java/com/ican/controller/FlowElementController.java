package com.ican.controller;

import com.ican.model.dto.FlowElementDTO;
import com.ican.model.vo.FlowElementVO;
import com.ican.service.FlowElementService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Api(value = "流程图模块")
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
    @ApiOperation(value = "获取流程图列表")
    public List<FlowElementVO> list() {
        return flowElementService.getElementList();
    }
    /**
     * 添加流程图
     *
     * @param dto 流程图信息
     */
    @PostMapping("/add")
    @ApiOperation(value = "添加流程图")
    public void add(@RequestBody FlowElementDTO dto) {
        flowElementService.addElement(dto);
    }
    /**
     * 修改流程图
     *
     * @param dto 流程图信息
     */
    @PutMapping("/update")
    @ApiOperation(value = "修改流程图")
    public void update(@RequestBody FlowElementDTO dto) {
        flowElementService.updateElement(dto);
    }
    /**
     * 删除流程图
     *
     * @param ids ids
     */
    @DeleteMapping("/delete")
    @ApiOperation(value = "删除流程图")
    public void delete(@RequestBody List<Long> ids) {
        flowElementService.deleteElementBatch(ids);
    }
}
