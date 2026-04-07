package com.ican.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ican.model.dto.ConditionDTO;
import com.ican.model.dto.DocDTO;
import com.ican.model.vo.*;
import com.ican.service.DocService;
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
     * 查看文档列表
     *
     * @return {@link List<DocVO>} 文档列表
     */
    @GetMapping("/docs/list")
    public Result<List<DocVO>> listDocs(ConditionDTO condition) {
        return Result.success(docService.listDocs(condition));
    }

    /**
     * 查看文档列表
     *
     * @return {@link List<DocVersionVO>} 文档列表
     */
    @GetMapping("admin/docs/allList")
    public Result<List<DocManagerVO>> listAllDocs() {
        return Result.success(docService.getAllDocs());
    }

    /**
     * 查看收藏文档列表
     */
    @GetMapping("/docs/collab/favorites")
    public Result<List<Integer>> listFavouriteDocs(@RequestParam Integer userId) {
        return Result.success(docService.listFavouriteDocs(userId));
    }

    /**
     * 添加收藏
     */

    @PutMapping("/docs/collab/addFavorite")
    public Result<Void> addFavorite(@RequestParam Integer userId,
                                    @RequestParam Integer docId) {
        // 调用服务层添加收藏
        docService.addFavorite(userId, docId);
        return Result.success();
    }

    /**
     * 取消收藏
     */
    @DeleteMapping("/docs/collab/cancelFavorite")
    public Result<Void> cancelFavorite(@RequestParam Integer userId,
                                       @RequestParam Integer docId) {
        docService.cancelFavorite(userId, docId);
        return Result.success();
    }

    /**
     * 创建文档
     *
     * @param docDTO 文档信息
     */
    @PostMapping("/docs/collab/create")
    public void createDoc(@RequestBody DocDTO docDTO) {
        docService.createDoc(docDTO);
    }

    /**
     * 修改文档
     *
     * @param docDTO 文档信息
     */
    @PutMapping("/docs/collab/update")
    public Result<Object> updateDoc(@RequestBody DocDTO docDTO) {
        docService.updateDoc(docDTO);
        return Result.success();
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
     * 查看文档标签
     *
     * @return {@link DocVO} 文档
     */
    @GetMapping("/docs/collab/tags")
    public Result<List<CollabTagVO>> getCollabTags() {
        return Result.success(docService.getCollabTags());
    }

    /**
     * 查看文档数量
     *
     * @return {@link DocVO} 文档
     */
    @GetMapping("/docs/collab/count")
    public Result<Integer> getDocCount() {
        return Result.success(docService.getDocCount());
    }
}
