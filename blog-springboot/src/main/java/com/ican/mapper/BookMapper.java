package com.ican.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ican.entity.Book;
import com.ican.model.vo.BookVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Book Mapper
 */
@Mapper
public interface BookMapper extends BaseMapper<Book> {

    /**
     * 查看书籍列表
     *
     * @param limit   页码
     * @param size    大小
     * @param keyword 关键字
     * @param sortType 排序方式
     * @return 书籍列表
     */
    List<BookVO> selectBookVOList(@Param("limit") Long limit, @Param("size") Long size, @Param("keyword") String keyword, @Param("sortType") String sortType );
}
