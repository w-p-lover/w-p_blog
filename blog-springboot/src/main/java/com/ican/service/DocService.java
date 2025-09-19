package com.ican.service;

import com.ican.model.dto.ConditionDTO;
import com.ican.model.dto.DocDTO;
import com.ican.model.vo.CollabTagVO;
import com.ican.model.vo.DocVO;

import java.util.List;

/**
 * 共享文档服务
 *
 */
public interface DocService {

    /**
     * 获取文档信息
     *
     * @param id 文档id
     * @return 文档信息
     */
    DocVO getDocById(Long id);
    /**
     * 获取文档列表
     *
     * @return 文档列表
     */
    List<DocVO> listDocs(ConditionDTO condition);

    /**
     * 创建文档
     *
     * @param docDTO 文档信息
     */
    void createDoc(DocDTO docDTO);
    /**
     * 修改文档
     *
     * @param docDTO 文档信息
     */
    void updateDoc(DocDTO docDTO);
    /**
     * 删除文档
     *
     * @param id 文档id
     */
    void deleteDoc(Long id);

    /**
     * 获取文档标签
     *
     * @return 文档标签
     */
    List<CollabTagVO> getCollabTags();

    /**
     * 获取文档数量
     *
     * @return 文档数量
     */
    Integer getDocCount();

    /**
     * 获取用户收藏的文档id
     *
     * @param userId 用户id
     * @return 文档id列表
     */
    List<Integer> listFavouriteDocs(Integer userId);

    /**
     * 添加文档收藏
     *
     * @param userId 用户id
     * @param docId 文档id
     */
    void addFavorite(Integer userId, Integer docId);

    /**
     * 取消文档收藏
     *
     * @param userId 用户id
     * @param docId 文档id
     */
    void cancelFavorite(Integer userId, Integer docId);
}
