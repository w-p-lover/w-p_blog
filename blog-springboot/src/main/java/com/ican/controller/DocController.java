package com.ican.controller;

import com.ican.model.dto.ConditionDTO;
import com.ican.model.dto.DocDTO;
import com.ican.model.vo.Result;
import com.ican.service.DocService;
import com.ican.model.vo.DocVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文档控制器
 *
 */
@RestController
@RequestMapping()
public class DocController {

    @Autowired
    private DocService docService;

    /**
     * 查看文档
     *
     * @param id 文档id
     * @return {@link DocVO} 文档
     */
    @GetMapping("/docs/{id}")
    public Result<DocVO> getDoc(@PathVariable Long id) {
        return Result.success(docService.getDocById(id));
    }

    /**
     * 查看文档列表
     *
     * @return {@link List<DocVO>} 文档列表
     */
    @GetMapping("/docs/list")
    public Result<List<DocVO>> listDocs(ConditionDTO condition) {
        return Result.success(docService.listDocs(condition));
    }

    /**
     * 创建文档
     *
     * @param docDTO 文档信息
     */
    @PostMapping("/docs/create")
    public void createDoc(@RequestBody DocDTO docDTO) {
        docService.createDoc(docDTO);
    }

    /**
     * 修改文档
     *
     * @param docDTO 文档信息
     */
    @PutMapping("/docs/update")
    public void updateDoc(@RequestBody DocDTO docDTO) {
        docService.updateDoc(docDTO);
    }

    /**
     * 删除文档
     *
     * @param id 文档id
     */
    @DeleteMapping("/docs/{id}")
    public void deleteDoc(@PathVariable Long id) {
        docService.deleteDoc(id);
    }
}
