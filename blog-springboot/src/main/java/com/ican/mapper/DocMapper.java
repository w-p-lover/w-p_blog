package com.ican.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ican.entity.Doc;
import com.ican.model.dto.ConditionDTO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DocMapper extends BaseMapper<Doc> {

    /**
     * 前台文档列表：分页 + 条件筛选
     *
     * @param offset    起始下标
     * @param size      每页大小
     * @param condition 查询条件 DTO（可包含标题关键字、标签、状态等）
     */
    List<Doc> getDocList(
            @Param("limit") Long offset,
            @Param("size") Long size,
            @Param("condition") ConditionDTO condition
    );

    List<Doc> getAdminDocList();
    /**
     * 按标题或摘要搜索
     */
    List<Doc> searchDoc(@Param("keyword") String keyword);

    /**
     * 获取所有标签
     */
    @Select("SELECT DISTINCT tags FROM t_doc")
    List<String> getCollabTags();

    /**
     * 统计总数
     */
    @Select("SELECT COUNT(*) FROM t_doc")
    int getTotalCount();

    /**
     * 浏览量+1
     */
    @Update("UPDATE t_doc SET views = views + 1 WHERE id = #{id}")
    void incrementViewCount(Long id);

    /**
     * 收藏+1
     */
    @Insert("INSERT INTO t_user_favorite (user_id, favorite_id) VALUES (#{userId}, #{docId})")
    void addFavorite(Integer userId, Integer docId);

    /**
     * 收藏-1
     */
    @Delete("DELETE FROM t_user_favorite WHERE user_id = #{userId} AND favorite_id = #{docId}")
    void cancelFavorite(Integer userId, Integer docId);
}
