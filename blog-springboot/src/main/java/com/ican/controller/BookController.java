package com.ican.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ican.annotation.OptLogger;
import com.ican.annotation.VisitLogger;
import com.ican.model.dto.BookDTO;
import com.ican.model.vo.BookVO;
import com.ican.model.vo.PageResult;
import com.ican.model.vo.Result;
import com.ican.service.BookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.ican.constant.OptTypeConstant.ADD;
import static com.ican.constant.OptTypeConstant.DELETE;
import static com.ican.constant.OptTypeConstant.UPDATE;

@Slf4j
@RestController
public class BookController {

    @Autowired
    private BookService bookService;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicReference<String> spiderStatus = new AtomicReference<>("IDLE");
    private final AtomicBoolean spiderRunning = new AtomicBoolean(false);

    @Value("${spider.dir}")
    private String zipDir;

    @SaCheckPermission("book:list")
    @GetMapping("/admin/book/list")
    public Result<PageResult<BookVO>> listBookBackVO(@RequestParam(required = false, defaultValue = "newest") String sortType) {
        return Result.success(bookService.listBookBackVO(sortType));
    }

    @OptLogger(value = ADD)
    @SaCheckPermission("book:add")
    @PostMapping("/admin/book/add")
    public Result<?> addBookDev(@Validated @RequestBody BookDTO bookDTO) {
        bookService.addBook(bookDTO);
        return Result.success();
    }

    @OptLogger(value = DELETE)
    @SaCheckPermission("book:delete")
    @DeleteMapping("/admin/book/delete")
    public Result<?> deleteBook(@RequestBody List<Integer> bookIdList) {
        bookService.deleteBook(bookIdList);
        return Result.success();
    }

    @OptLogger(value = UPDATE)
    @SaCheckPermission("book:update")
    @PutMapping("/admin/book/update")
    public Result<?> updateBook(@Validated @RequestBody BookDTO bookDTO) {
        bookService.updateBook(bookDTO);
        return Result.success();
    }

    @SaCheckPermission("book:edit")
    @GetMapping("/admin/book/edit/{bookId}")
    public Result<BookVO> editBook(@PathVariable("bookId") Integer bookId) {
        return Result.success(bookService.getBookDetail(bookId));
    }

    @VisitLogger(value = "书籍列表")
    @GetMapping("/book/list")
    public Result<PageResult<BookVO>> listBookVO(@RequestParam(value = "sortType", defaultValue = "newest") String sortType) {
        return Result.success(bookService.listBookVO(sortType));
    }

    @PostMapping("/book/add")
    public Result<?> addBook(@Validated @RequestBody BookDTO bookDTO) {
        bookService.addBook(bookDTO);
        return Result.success();
    }

    @PostMapping("/book/upload")
    public Result<String> uploadBookImage(@RequestParam("file") MultipartFile file) {
        return Result.success(bookService.uploadBookImage(file));
    }

    @VisitLogger(value = "书籍")
    @GetMapping("/book/{bookId}")
    public Result<BookVO> getBook(@PathVariable("bookId") Integer bookId) {
        return Result.success(bookService.getBookDetail(bookId));
    }

    @PutMapping("/book/{bookId}/status")
    public Result<?> updateBookStatus(@PathVariable("bookId") Integer bookId,
                                      @RequestParam("status") String status) {
        bookService.updateBookStatus(bookId, status);
        return Result.success();
    }

    @GetMapping("/book/search")
    public Result<List<BookVO>> searchBooks(@RequestParam String keyword) {
        return Result.success(bookService.searchBooks(keyword));
    }

    @PostMapping("/book/deleteResource")
    public String deleteResource(@RequestParam Integer bookId, @RequestParam int index) {
        bookService.deleteResource(bookId, index);
        return "success";
    }

    @PostMapping("/book/updateResource")
    public String updateResource(@RequestParam Integer bookId, @RequestParam String resourceJson) {
        bookService.updateResource(bookId, resourceJson);
        return "success";
    }

    @PostMapping("/book/run")
    public Result<?> runSpider() {
        if (!spiderRunning.compareAndSet(false, true)) {
            return Result.fail("爬虫正在运行，请稍后重试");
        }
        spiderStatus.set("RUNNING");
        executor.submit(() -> {
            try {
                bookService.runPythonSpider(spiderStatus);
                if (!"FAILED".equals(spiderStatus.get())) {
                    spiderStatus.set("COMPLETED");
                }
            } catch (Exception e) {
                spiderStatus.set("FAILED");
                log.error("Book spider task failed", e);
            } finally {
                spiderRunning.set(false);
            }
        });
        return Result.success("书源爬虫任务已启动");
    }

    @GetMapping("/book/status")
    public Result<?> getStatus() {
        Map<String, Object> statusInfo = new HashMap<>();
        int totalCount = bookService.getTotalCount();
        double bookCount = bookService.getBookCount();
        double spiderPercentage = totalCount <= 0 ? 0 : Math.round(bookCount * 100 / totalCount);

        String message;
        switch (spiderStatus.get()) {
            case "RUNNING":
                message = "爬虫正在运行中...";
                break;
            case "INSERTING":
                message = "正在插入书源数据...";
                break;
            case "COMPLETED":
            case "COMPLETE":
                message = "爬虫任务已完成";
                break;
            case "FAILED":
                message = "爬虫任务失败";
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
            throw new RuntimeException("文件不存在或爬虫尚未打包完成");
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
            log.error("Download book zip failed", e);
            throw new RuntimeException("下载失败");
        }
    }
}
