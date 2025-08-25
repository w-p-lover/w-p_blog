package com.ican.service;

import com.ican.model.vo.BookVO;
import com.ican.model.vo.PageResult;

import java.util.List;

/**
 * 书籍服务
 */
public interface BookService {

    /**
     * 后台查看书籍列表 (可按 sortType 排序)
     */
    PageResult<BookVO> listBookBackVO(String sortType);

    /**
     * 前台查看书籍列表
     */
    PageResult<BookVO> listBookVO(String sortType);

    /**
     * 添加书籍
     */
    void addBook(com.ican.model.dto.BookDTO bookDTO);

    /**
     * 删除书籍
     */
    void deleteBook(List<Integer> bookIdList);

    /**
     * 修改书籍
     */
    void updateBook(com.ican.model.dto.BookDTO bookDTO);

    /**
     * 获取书籍详情
     */
    BookVO getBookDetail(Integer bookId);

    /**
     * 更新书籍状态
     */
    void updateBookStatus(Integer bookId, String status);

    /**
     * 搜索书籍
     */
    List<BookVO> searchBooks(String keyword);
}
