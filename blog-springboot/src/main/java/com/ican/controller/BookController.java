package com.ican.controller;

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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static com.ican.constant.OptTypeConstant.*;

/**
 * 书籍模块
 */
@Api(tags = "书籍模块")
@Slf4j
@RestController
public class BookController {

    @Autowired
    private BookService bookService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final AtomicReference<String> spiderStatus = new AtomicReference<>("IDLE");

    @Value("${spider.dir}")
    private String zipDir;

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

    // 删除书籍
    @ApiOperation(value = "删除书籍")
    @PostMapping("/book/deleteResource")
    public String deleteResource(@RequestParam Integer bookId, @RequestParam int index) {
        bookService.deleteResource(bookId, index);
        return "success";
    }


    // 更新书源字段
    @ApiOperation(value = "更新书源字段")
    @PostMapping("/book/updateResource")
    public String updateResource(@RequestParam Integer bookId, @RequestParam String resourceJson) {
        bookService.updateResource(bookId, resourceJson);
        return "success";
    }

    @ApiOperation(value = "爬虫任务")
    @PostMapping("/book/run")
    public Result<?> runSpider() {
        // 检查爬虫运行状态
        if ("RUNNING".equals(spiderStatus.get())) {
            return Result.fail("爬虫正在运行，请稍后重试");
        }
        spiderStatus.set("RUNNING");
        executor.submit(() -> {
            try {
                // 将专辑名称传递给服务层，由服务层决定具体爬虫逻辑
                bookService.runPythonSpider(spiderStatus);
                spiderStatus.set("COMPLETED");
            } catch (Exception e) {
                spiderStatus.set("FAILED");
                log.error("书源爬虫任务发生异常：{}", e.getMessage());
            }
        });
        return Result.success("书源的爬虫任务已启动");
    }


    @GetMapping("/book/status")
    public Result<?> getStatus() {
        Map<String, Object> statusInfo = new HashMap<>();
        int totalCount = bookService.getTotalCount();
        double bookCount = bookService.getBookCount();
        double spiderPercentage = Math.round(bookCount * 100 / totalCount);

        String message;
        switch (spiderStatus.get()) {
            case "RUNNING":
                message = "爬虫正在运行中...";
                break;
            case "INSERTING":
                message = "正在插入书源数据...";
                break;
            case "COMPLETED":
                message = "爬虫任务已完成 ✅";
                break;
            case "FAILED":
                message = "爬虫任务失败 ❌";
                break;
            default:
                message = "空闲中";
                break;
        }
        statusInfo.put("message", message);
        statusInfo.put("status", spiderStatus.get());
        statusInfo.put("timestamp", System.currentTimeMillis());
        statusInfo.put("spiderPercentage", spiderPercentage);
        return Result.success(statusInfo);
    }

    @GetMapping("/book/download")
    public void downloadBookZip(HttpServletResponse response) {
        String zipPath = zipDir + "book_result.zip";
        File zipFile = new File(zipPath);

        if (!zipFile.exists()) {
            throw new RuntimeException("文件不存在或爬虫尚未完成打包");
        }
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=book_result.zip");

        try (InputStream inputStream = new FileInputStream(zipFile);
             OutputStream outputStream = response.getOutputStream()) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();

        } catch (IOException e) {
            log.error("下载书源zip文件失败", e);
            throw new RuntimeException("下载失败");
        }
    }
}
