package com.ican.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ican.entity.Doc;
import com.ican.model.dto.ConditionDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DocMapper extends BaseMapper<Doc> {

    /**
     * 前台文档列表：分页 + 条件筛选
     * @param offset  起始下标
     * @param size    每页大小
     * @param condition 查询条件 DTO（可包含标题关键字、标签、状态等）
     */
    List<Doc> getDocList(
            @Param("limit") Long offset,
            @Param("size") Long size,
            @Param("condition") ConditionDTO condition
    );

    /**
     * 统计总数
     */
    @Select("SELECT COUNT(*) FROM t_doc")
    int getTotalCount();

    /**
     * 按标题或摘要搜索
     */
    @Select("SELECT * FROM t_doc " +
            "WHERE title LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR excerpt LIKE CONCAT('%', #{keyword}, '%')")
    List<Doc> searchDoc(@Param("keyword") String keyword);
}
