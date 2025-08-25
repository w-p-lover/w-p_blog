package com.ican.service.impl;

import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ican.entity.Book;
import com.ican.mapper.BookMapper;
import com.ican.model.dto.BookDTO;
import com.ican.model.vo.BookVO;
import com.ican.model.vo.PageResult;
import com.ican.service.BookService;
import com.ican.utils.BeanCopyUtils;
import com.ican.utils.PageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 书籍业务接口实现类
 *
 * @author
 * @date 2025/08/20
 **/
@Service
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {

    @Autowired
    private BookMapper bookMapper;

    /**
     * 后台查询书籍列表
     */
    @Override
    public PageResult<BookVO> listBookBackVO(String sortType) {
        List<BookVO> bookList = bookMapper.selectBookVOList(PageUtils.getLimit(), PageUtils.getSize(), null, sortType);
        long count = bookList.size();
        if (count == 0) {
            return new PageResult<>();
        }
        return new PageResult<>(bookList, count);
    }

    /**
     * 添加书籍
     */
    @Override
    public void addBook(BookDTO bookDTO) {
        // 判断书名是否已存在
        Book existBook = bookMapper.selectOne(new LambdaQueryWrapper<Book>()
                .select(Book::getId)
                .eq(Book::getTitle, bookDTO.getTitle())
                .eq(Book::getAuthor, bookDTO.getAuthor()));
        Assert.isNull(existBook, bookDTO.getTitle() + " 已存在");

        Book newBook = BeanCopyUtils.copyBean(bookDTO, Book.class);
        newBook.setAddTime(LocalDateTime.now());
        baseMapper.insert(newBook);
    }

    /**
     * 删除书籍
     */
    @Override
    public void deleteBook(List<Integer> bookIdList) {
        Assert.isFalse(CollectionUtils.isEmpty(bookIdList), "请选择要删除的书籍");
        bookMapper.deleteBatchIds(bookIdList);
    }

    /**
     * 修改书籍信息
     */
    @Override
    public void updateBook(BookDTO bookDTO) {
        Book existBook = bookMapper.selectOne(new LambdaQueryWrapper<Book>()
                .select(Book::getId)
                .eq(Book::getTitle, bookDTO.getTitle())
                .eq(Book::getAuthor, bookDTO.getAuthor()));
        Assert.isFalse(Objects.nonNull(existBook) && !existBook.getId().equals(bookDTO.getId()),
                bookDTO.getTitle() + " 已存在");

        Book newBook = BeanCopyUtils.copyBean(bookDTO, Book.class);
        newBook.setUpdateTime(LocalDateTime.now());
        bookMapper.updateById(newBook);
    }

    /**
     * 查看书籍详情
     */
    @Override
    public BookVO getBookDetail(Integer bookId) {
        Book book = bookMapper.selectById(bookId);
        Assert.notNull(book, "书籍不存在");
        return BeanCopyUtils.copyBean(book, BookVO.class);
    }

    /**
     * 更新书籍状态
     */
    @Override
    public void updateBookStatus(Integer bookId, String status) {
        Book book = bookMapper.selectById(bookId);
        Assert.notNull(book, "书籍不存在");
        book.setStatus(status);
        book.setUpdateTime(LocalDateTime.now());
        bookMapper.updateById(book);
    }

    /**
     * 搜索书籍
     */
    @Override
    public List<BookVO> searchBooks(String keyword) {
        List<BookVO> books = bookMapper.selectBookVOList(PageUtils.getLimit(), PageUtils.getSize(), keyword, null);
        if (CollectionUtils.isEmpty(books)) {
            return Collections.emptyList();
        }
        return books;
    }

    /**
     * 前台查询书籍列表
     */
    @Override
    public PageResult<BookVO> listBookVO(String sortType) {
        List<BookVO> bookList = bookMapper.selectBookVOList(PageUtils.getLimit(), PageUtils.getSize(), null, sortType);
        if (CollectionUtils.isEmpty(bookList)) {
            return new PageResult<>();
        }
        long count = bookList.size();
        return new PageResult<>(bookList, count);
    }
}
