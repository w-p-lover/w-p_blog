package com.ican.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ican.annotation.OptLogger;
import com.ican.annotation.VisitLogger;
import com.ican.model.dto.BookDTO;
import com.ican.model.vo.BookVO;
import com.ican.model.vo.PageResult;
import com.ican.service.BookService;
import com.ican.model.vo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.ican.constant.OptTypeConstant.*;

/**
 * 书籍模块
 */
@Api(tags = "书籍模块")
@RestController
public class BookController {

    @Autowired
    private BookService bookService;

    /**
     * 后台查看书籍列表
     */
    @ApiOperation(value = "查看后台书籍列表")
    @SaCheckPermission("book:list")
    @GetMapping("/admin/book/list")
    public Result<PageResult<BookVO>> listBookBackVO(@RequestParam(required = false, defaultValue = "newest") String sortType) {
        return Result.success(bookService.listBookBackVO(sortType));
    }

    /**
     * 后台添加书籍
     */
    @OptLogger(value = ADD)
    @ApiOperation(value = "添加书籍")
    @SaCheckPermission("book:add")
    @PostMapping("/admin/book/add")
    public Result<?> addBookDev(@Validated @RequestBody BookDTO bookDTO) {
        bookService.addBook(bookDTO);
        return Result.success();
    }

    /**
     * 后台删除书籍
     */
    @OptLogger(value = DELETE)
    @ApiOperation(value = "删除书籍")
    @SaCheckPermission("book:delete")
    @DeleteMapping("/admin/book/delete")
    public Result<?> deleteBook(@RequestBody List<Integer> bookIdList) {
        bookService.deleteBook(bookIdList);
        return Result.success();
    }

    /**
     * 后台修改书籍
     */
    @OptLogger(value = UPDATE)
    @ApiOperation(value = "修改书籍")
    @SaCheckPermission("book:update")
    @PutMapping("/admin/book/update")
    public Result<?> updateBook(@Validated @RequestBody BookDTO bookDTO) {
        bookService.updateBook(bookDTO);
        return Result.success();
    }

    /**
     * 后台编辑/查看书籍详情
     */
    @ApiOperation(value = "编辑/查看书籍详情")
    @SaCheckPermission("book:edit")
    @GetMapping("/admin/book/edit/{bookId}")
    public Result<BookVO> editBook(@PathVariable("bookId") Integer bookId) {
        return Result.success(bookService.getBookDetail(bookId));
    }

    /**
     * 前台书籍列表
     */
    @VisitLogger(value = "书籍列表")
    @ApiOperation(value = "查看书籍列表")
    @GetMapping("/book/list")
    public Result<PageResult<BookVO>> listBookVO(@RequestParam(value = "sortType", defaultValue = "newest") String sortType) {
        return Result.success(bookService.listBookVO(sortType));
    }
    /**
     * 添加书籍
     */
    @ApiOperation(value = "添加书籍")
    @PostMapping("/book/add")
    public Result<?> addBook(@Validated @RequestBody BookDTO bookDTO) {
        bookService.addBook(bookDTO);
        return Result.success();
    }
    /**
     * 前台查看某本书
     */
    @VisitLogger(value = "书籍")
    @ApiOperation(value = "查看书籍")
    @GetMapping("/book/{bookId}")
    public Result<BookVO> getBook(@PathVariable("bookId") Integer bookId) {
        return Result.success(bookService.getBookDetail(bookId));
    }

    /**
     * 更新书籍状态（前台用户操作）
     */
/*    @OptLogger(value = UPDATE)*/
/*    @SaCheckLogin*/
    @ApiOperation(value = "更新书籍状态")
    @PutMapping("/book/{bookId}/status")
    public Result<?> updateBookStatus(@PathVariable("bookId") Integer bookId,
                                      @RequestParam("status") String status) {
        bookService.updateBookStatus(bookId, status);
        return Result.success();
    }

    /**
     * 搜索书籍
     */
    @ApiOperation(value = "搜索书籍")
    @GetMapping("/book/search")
    public Result<List<BookVO>> searchBooks(@RequestParam String keyword) {
        return Result.success(bookService.searchBooks(keyword));
    }
}
