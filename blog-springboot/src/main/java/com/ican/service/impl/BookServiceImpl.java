package com.ican.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ican.entity.BlogFile;
import com.ican.entity.Book;
import com.ican.mapper.BlogFileMapper;
import com.ican.mapper.BookMapper;
import com.ican.model.dto.BookDTO;
import com.ican.model.dto.ResourceDTO;
import com.ican.model.vo.BookVO;
import com.ican.model.vo.PageResult;
import com.ican.service.BookService;
import com.ican.strategy.context.UploadStrategyContext;
import com.ican.utils.BeanCopyUtils;
import com.ican.utils.FileUtils;
import com.ican.utils.PageUtils;
import com.ican.utils.PythonScriptRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.ican.constant.CommonConstant.FALSE;
import static com.ican.enums.FilePathEnum.BOOK;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {

    private static final String BOOK_DIR_NAME = "book";
    private static final Set<String> VALID_STATUS_SET = Set.of("wish", "reading", "read");

    private final BookMapper bookMapper;
    private final BlogFileMapper blogFileMapper;
    private final UploadStrategyContext uploadStrategyContext;
    private final PythonScriptRunner pythonScriptRunner;
    private final AtomicInteger totalCount = new AtomicInteger(1);

    @Value("${spider.dir}")
    private String spiderDir;

    @Override
    public PageResult<BookVO> listBookBackVO(String sortType) {
        List<BookVO> bookList = bookMapper.selectBookVOList(PageUtils.getLimit(), PageUtils.getSize(), null, sortType);
        long count = countBooks(null);
        if (count == 0) {
            return new PageResult<>();
        }
        return new PageResult<>(bookList, count);
    }

    @Override
    public void addBook(BookDTO bookDTO) {
        Assert.isNull(findDuplicateBook(bookDTO), bookDTO.getTitle() + " already exists");
        ensureStatus(bookDTO);

        Book newBook = BeanCopyUtils.copyBean(bookDTO, Book.class);
        newBook.setAddTime(LocalDateTime.now());
        if (bookDTO.getResource() != null) {
            newBook.setResource(serializeResources(parseResources(bookDTO.getResource())));
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
        Book existBook = findDuplicateBook(bookDTO);
        Assert.isFalse(Objects.nonNull(existBook) && !existBook.getId().equals(bookDTO.getId()),
                bookDTO.getTitle() + " already exists");
        ensureStatus(bookDTO);

        Book newBook = BeanCopyUtils.copyBean(bookDTO, Book.class);
        newBook.setUpdateTime(LocalDateTime.now());
        if (bookDTO.getResource() != null) {
            newBook.setResource(serializeResources(parseResources(bookDTO.getResource())));
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
        validateStatus(status);
        Book book = bookMapper.selectById(bookId);
        Assert.notNull(book, "Book not found");
        book.setStatus(status);
        book.setUpdateTime(LocalDateTime.now());
        bookMapper.updateById(book);
    }

    @Override
    public String uploadBookImage(MultipartFile file) {
        String url = uploadStrategyContext.executeUploadStrategy(file, BOOK.getPath());
        try {
            String md5 = FileUtils.getMd5(file.getInputStream());
            String extName = FileUtils.getExtension(file);
            BlogFile existFile = blogFileMapper.selectOne(new LambdaQueryWrapper<BlogFile>()
                    .select(BlogFile::getId)
                    .eq(BlogFile::getFileName, md5)
                    .eq(BlogFile::getFilePath, BOOK.getFilePath()));
            if (Objects.isNull(existFile)) {
                BlogFile newFile = BlogFile.builder()
                        .fileUrl(url)
                        .fileName(md5)
                        .filePath(BOOK.getFilePath())
                        .extendName(extName)
                        .fileSize((int) file.getSize())
                        .isDir(FALSE)
                        .build();
                blogFileMapper.insert(newFile);
            }
        } catch (IOException e) {
            log.error("书籍图片上传持久化错误: {}", e.getMessage());
        }
        return url;
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
        Assert.notNull(bookMapper.selectById(bookId), "Book not found");
        Book book = new Book();
        book.setId(bookId);
        book.setResource(serializeResources(parseResources(resourceJson)));
        bookMapper.updateById(book);
    }

    @Override
    public void deleteResource(Integer bookId, int index) {
        Book book = bookMapper.selectById(bookId);
        Assert.notNull(book, "Book not found");
        Assert.isFalse(book.getResource() == null || book.getResource().isBlank(), "Book resources not found");

        try {
            List<ResourceDTO> resources = parseResources(book.getResource());
            if (index < 0 || index >= resources.size()) {
                throw new IllegalArgumentException("Resource index out of range");
            }
            resources.remove(index);
            book.setResource(serializeResources(resources));
            bookMapper.updateById(book);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete resource", e);
        }
    }

    @Override
    public PageResult<BookVO> listBookVO(String sortType) {
        List<BookVO> bookList = bookMapper.selectBookVOList(PageUtils.getLimit(), PageUtils.getSize(), null, sortType);
        long count = countBooks(null);
        if (count == 0) {
            return new PageResult<>();
        }
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

    private long countBooks(String keyword) {
        Long count = bookMapper.countBookVOList(keyword);
        return count == null ? 0 : count;
    }

    private Book findDuplicateBook(BookDTO bookDTO) {
        return bookMapper.selectOne(new LambdaQueryWrapper<Book>()
                .select(Book::getId)
                .eq(Book::getTitle, bookDTO.getTitle())
                .eq(Book::getAuthor, bookDTO.getAuthor()));
    }

    private void ensureStatus(BookDTO bookDTO) {
        if (bookDTO.getStatus() == null || bookDTO.getStatus().isBlank()) {
            bookDTO.setStatus("wish");
            return;
        }
        validateStatus(bookDTO.getStatus());
    }

    private void validateStatus(String status) {
        Assert.isTrue(VALID_STATUS_SET.contains(status), "Invalid book status: " + status);
    }

    private List<ResourceDTO> parseResources(String resourceJson) {
        if (resourceJson == null || resourceJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return JSONUtil.toList(resourceJson, ResourceDTO.class);
        } catch (Exception ignored) {
            ResourceDTO resource = JSONUtil.toBean(resourceJson, ResourceDTO.class);
            return Arrays.asList(resource);
        }
    }

    private String serializeResources(List<ResourceDTO> resources) {
        return JSON.toJSONString(resources == null ? Collections.emptyList() : resources);
    }
}
