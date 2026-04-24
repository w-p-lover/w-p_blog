package com.ican.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ican.entity.Book;
import com.ican.mapper.BookMapper;
import com.ican.model.dto.BookDTO;
import com.ican.model.dto.ResourceDTO;
import com.ican.model.vo.BookVO;
import com.ican.model.vo.PageResult;
import com.ican.service.BookService;
import com.ican.utils.BeanCopyUtils;
import com.ican.utils.PageUtils;
import com.ican.utils.PythonScriptRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {

    private static final String BOOK_DIR_NAME = "book";

    private final BookMapper bookMapper;
    private final PythonScriptRunner pythonScriptRunner;
    private final AtomicInteger totalCount = new AtomicInteger(1);

    @Value("${spider.dir}")
    private String spiderDir;

    @Override
    public PageResult<BookVO> listBookBackVO(String sortType) {
        List<BookVO> bookList = bookMapper.selectBookVOList(PageUtils.getLimit(), PageUtils.getSize(), null, sortType);
        long count = bookList.size();
        if (count == 0) {
            return new PageResult<>();
        }
        return new PageResult<>(bookList, count);
    }

    @Override
    public void addBook(BookDTO bookDTO) {
        Book existBook = bookMapper.selectOne(new LambdaQueryWrapper<Book>()
                .select(Book::getId)
                .eq(Book::getTitle, bookDTO.getTitle())
                .eq(Book::getAuthor, bookDTO.getAuthor()));
        Assert.isNull(existBook, bookDTO.getTitle() + " already exists");

        Book newBook = BeanCopyUtils.copyBean(bookDTO, Book.class);
        newBook.setAddTime(LocalDateTime.now());
        if (bookDTO.getResource() != null) {
            newBook.setResource(bookDTO.getResource());
        }
        baseMapper.insert(newBook);
    }

    @Override
    public void deleteBook(List<Integer> bookIdList) {
        Assert.isFalse(CollectionUtils.isEmpty(bookIdList), "Please select books to delete");
        bookMapper.deleteBatchIds(bookIdList);
    }

    @Override
    public void updateBook(BookDTO bookDTO) {
        Book existBook = bookMapper.selectOne(new LambdaQueryWrapper<Book>()
                .select(Book::getId)
                .eq(Book::getTitle, bookDTO.getTitle())
                .eq(Book::getAuthor, bookDTO.getAuthor()));
        Assert.isFalse(Objects.nonNull(existBook) && !existBook.getId().equals(bookDTO.getId()),
                bookDTO.getTitle() + " already exists");

        Book newBook = BeanCopyUtils.copyBean(bookDTO, Book.class);
        newBook.setUpdateTime(LocalDateTime.now());
        if (bookDTO.getResource() != null) {
            newBook.setResource(bookDTO.getResource());
        }
        bookMapper.updateById(newBook);
    }

    @Override
    public BookVO getBookDetail(Integer bookId) {
        Book book = bookMapper.selectById(bookId);
        Assert.notNull(book, "Book not found");
        return BeanCopyUtils.copyBean(book, BookVO.class);
    }

    @Override
    public void updateBookStatus(Integer bookId, String status) {
        Book book = bookMapper.selectById(bookId);
        Assert.notNull(book, "Book not found");
        book.setStatus(status);
        book.setUpdateTime(LocalDateTime.now());
        bookMapper.updateById(book);
    }

    @Override
    public List<BookVO> searchBooks(String keyword) {
        List<BookVO> books = bookMapper.selectBookVOList(PageUtils.getLimit(), PageUtils.getSize(), keyword, null);
        if (CollectionUtils.isEmpty(books)) {
            return Collections.emptyList();
        }
        return books;
    }

    @Override
    public void updateResource(Integer bookId, String resourceJson) {
        Book book = new Book();
        book.setId(bookId);
        book.setResource(resourceJson);
        bookMapper.updateById(book);
    }

    @Override
    public void deleteResource(Integer bookId, int index) {
        Book book = bookMapper.selectById(bookId);
        if (book == null || book.getResource() == null) {
            return;
        }

        try {
            List<ResourceDTO> resources = JSONUtil.toList(book.getResource(), ResourceDTO.class);
            if (index < 0 || index >= resources.size()) {
                throw new IllegalArgumentException("Resource index out of range");
            }
            resources.remove(index);
            book.setResource(JSON.toJSONString(resources));
            bookMapper.updateById(book);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete resource", e);
        }
    }

    @Override
    public PageResult<BookVO> listBookVO(String sortType) {
        List<BookVO> bookList = bookMapper.selectBookVOList(PageUtils.getLimit(), PageUtils.getSize(), null, sortType);
        if (CollectionUtils.isEmpty(bookList)) {
            return new PageResult<>();
        }
        long count = bookList.size();
        return new PageResult<>(bookList, count);
    }

    @Override
    public void runPythonSpider(AtomicReference<String> status) {
        totalCount.set(0);
        try {
            clearBeforeSpider();
            log.info("Book spider cleanup finished");
        } catch (Exception e) {
            log.error("Book spider cleanup failed", e);
            status.set("FAILED");
            return;
        }

        try {
            PythonScriptRunner.PythonExecutionResult result = pythonScriptRunner.run(
                    "static/zxcs.py",
                    List.of("--save-dir", resolveBookDir().getAbsolutePath()),
                    line -> log.info("[book-spider] {}", line)
            );
            if (result.totalCount() != null) {
                totalCount.set(result.totalCount());
            }
            if (!result.isSuccess()) {
                log.error("Book spider failed, exitCode={}, timedOut={}", result.exitCode(), result.timedOut());
                status.set("FAILED");
                return;
            }
            zipBooksResource(status);
            log.info("Book spider archive finished, status={}", status.get());
        } catch (Exception e) {
            log.error("Book spider execution failed", e);
            status.set("FAILED");
        }
    }

    @Override
    public int getTotalCount() {
        return totalCount.get();
    }

    @Override
    public double getBookCount() {
        File dir = resolveBookDir();
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("Book directory does not exist: {}", dir.getAbsolutePath());
            return 0;
        }
        File[] files = dir.listFiles();
        return files == null ? 0 : files.length;
    }

    private void zipBooksResource(AtomicReference<String> status) {
        File sourceDir = resolveBookDir();
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            log.warn("Book source directory does not exist: {}", sourceDir.getAbsolutePath());
            status.set("FAILED");
            return;
        }

        File zipFile = new File(sourceDir.getParent(), "book_result.zip");
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            zipDirectory(sourceDir, sourceDir.getName(), zos);
            log.info("Book zip generated: {}", zipFile.getAbsolutePath());
            status.set("COMPLETE");
        } catch (IOException e) {
            log.error("Failed to zip book results", e);
            status.set("FAILED");
        }
    }

    private void zipDirectory(File folder, String parentFolder, ZipOutputStream zos) throws IOException {
        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                zipDirectory(file, parentFolder + "/" + file.getName(), zos);
                continue;
            }
            try (FileInputStream fis = new FileInputStream(file)) {
                ZipEntry zipEntry = new ZipEntry(parentFolder + "/" + file.getName());
                zos.putNextEntry(zipEntry);

                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) >= 0) {
                    zos.write(buffer, 0, length);
                }
                zos.closeEntry();
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void clearBeforeSpider() {
        File bookDir = resolveBookDir();
        if (!bookDir.exists() || !bookDir.isDirectory()) {
            log.warn("Book cleanup directory does not exist: {}", bookDir.getAbsolutePath());
            return;
        }

        File[] files = bookDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
        int deletedFileCount = 0;
        int failedFileCount = 0;
        if (files != null) {
            for (File file : files) {
                if (file.delete()) {
                    deletedFileCount++;
                } else {
                    failedFileCount++;
                    log.warn("Failed to delete book file: {}", file.getAbsolutePath());
                }
            }
        }
        log.info("Book cleanup finished: path={}, deleted={}, failed={}",
                bookDir.getAbsolutePath(), deletedFileCount, failedFileCount);
    }

    private File resolveBookDir() {
        return new File(spiderDir, BOOK_DIR_NAME);
    }
}
