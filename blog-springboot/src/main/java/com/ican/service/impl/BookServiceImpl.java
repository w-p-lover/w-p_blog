package com.ican.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ican.entity.Book;
import com.ican.entity.Photo;
import com.ican.mapper.BookMapper;
import com.ican.model.dto.BookDTO;
import com.ican.model.dto.ResourceDTO;
import com.ican.model.vo.BookVO;
import com.ican.model.vo.PageResult;
import com.ican.service.BookService;
import com.ican.utils.BeanCopyUtils;
import com.ican.utils.PageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 书籍业务接口实现类
 *
 * @author
 * @date 2025/08/20
 **/
@Service
@Slf4j
@RequiredArgsConstructor
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {

    private final BookMapper bookMapper;
    private final AtomicInteger totalCount = new AtomicInteger(1);
    // 从配置文件注入，避免硬编码
    @Value("${spider.dir}")
    private String wallhavenDir;
    @Value("${spider.python.cmd}")
    private String pythonCmd;
    @Value("${spider.photo.album.id}")
    private Integer albumId;

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

        if (bookDTO.getResource() != null) {
            newBook.setResource(bookDTO.getResource());
        }
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
        if (bookDTO.getResource() != null) {
            newBook.setResource(bookDTO.getResource());
        }
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
     * 更新书源字段
     */
    public void updateResource(Integer bookId, String resourceJson) {
        Book book = new Book();
        book.setId(bookId);
        book.setResource(resourceJson);
        bookMapper.updateById(book);
    }

    /**
     * 删除某书书源
     */
    @Override
    public void deleteResource(Integer bookId, int index) {
        Book book = bookMapper.selectById(bookId);
        if (book == null || book.getResource() == null) return;

        try {
            List<ResourceDTO> resourceDTOS = JSONUtil.toList(book.getResource(), ResourceDTO.class);
            if (index < 0 || index >= resourceDTOS.size()) {
                throw new IllegalArgumentException("删除索引越界");
            }
            resourceDTOS.remove(index);
            book.setResource(JSON.toJSONString(resourceDTOS));
            bookMapper.updateById(book);
        } catch (Exception e) {
            throw new RuntimeException("删除书源失败", e);
        }
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

    @Override
    public void runPythonSpider(AtomicReference<String> status) {
        totalCount.set(0);
        try {
            clearBeforeSpider(); // 修改清理方法，增加专辑参数
            log.info("书源爬虫前置清理完成");
        } catch (Exception e) {
            log.error("书源爬虫前置清理失败");
            status.set("FAILED");
            return;
        }

        // 2. 运行Python爬虫（根据专辑名称选择脚本或传递参数）
        Process process = null;
        File tempFile = null;
        try {
            // 2.1 根据专辑名称选择不同的Python脚本（或传递参数）
            String scriptResource = "static/zxcs.py";
            ClassPathResource resource = new ClassPathResource(scriptResource);

            // 2.2 创建临时脚本文件
            tempFile = File.createTempFile("book_spider", ".py");
            Files.copy(resource.getInputStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("书源的Python临时脚本创建完成：{}", tempFile.getAbsolutePath());

            // 2.3 启动爬虫进程（传递专辑名称作为参数给Python脚本）
            ProcessBuilder pb = new ProcessBuilder(
                    pythonCmd,
                    tempFile.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            process = pb.start();

            // 2.4 读取爬虫日志（增加专辑标识）
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[书源爬虫日志] {}", line);

                    // 解析总数量（匹配Python输出的TOTAL_COUNT前缀）
                    if (line.startsWith("TOTAL_COUNT: ")) {
                        String totalStr = line.split(": ")[1].trim();
                        try {
                            int total = Integer.parseInt(totalStr);
                            totalCount.set(total); // 更新全局总数
                            log.info("解析到全局总需爬取数量：{}", total);
                        } catch (NumberFormatException e) {
                            log.error("解析总数量失败，格式错误：{}", line);
                        }
                    }
                }
            }

            // 2.5 等待爬虫完成并检查退出码
            int exitCode = process.waitFor();
            log.info("书源的爬虫执行完成，退出码：{}（0表示成功）", exitCode);
            if (exitCode != 0) {
                log.error("书源的爬虫执行失败，退出码非0");
                status.set("FAILED");
                return;
            }
            zipBooksResource(status);
            log.info("书源插入完成，最终状态：{}", status.get());

        } catch (Exception e) {
            log.error("书源爬虫执行过程异常", e);
            status.set("FAILED");
        } finally {
            // 强制释放资源
            if (process != null && process.isAlive()) {
                process.destroy();
                log.info("书源的爬虫进程已强制销毁");
            }
            if (tempFile != null && tempFile.exists() && !tempFile.delete()) {
                log.warn("书源的临时Python脚本删除失败：{}", tempFile.getAbsolutePath());
            }
        }
    }

    @Override
    public int getTotalCount() {
        return totalCount.get();
    }

    @Override
    public double getBookCount() {
        String sourceDirPath = wallhavenDir + "book";
        File dir = new File(sourceDirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("统计书源数量时，目录不存在：{}", sourceDirPath);
            return 0;
        }
        File[] files = dir.listFiles();
        return files == null ? 0 : files.length;
    }

    private void zipBooksResource(AtomicReference<String> status) {
        String sourceDirPath = wallhavenDir + "book";
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            log.warn("打包失败，源目录不存在：{}", sourceDirPath);
            status.set("FAILED");
            return;
        }

        // 输出路径（临时 zip 文件）
        File zipFile = new File(sourceDir.getParent(), "book_result.zip");

        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            zipDirectory(sourceDir, sourceDir.getName(), zos);
            log.info("书源打包完成：{}", zipFile.getAbsolutePath());
            status.set("COMPLETE");

        } catch (IOException e) {
            log.error("书源打包失败", e);
            status.set("FAILED");
        }
    }

    /**
     * 递归压缩文件夹内容
     */
    private void zipDirectory(File folder, String parentFolder, ZipOutputStream zos) throws IOException {
        File[] files = folder.listFiles();
        if (files == null) return;

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
        // 1. 定位当前专辑的目录
        String bookDirPath = wallhavenDir + "book";
        File albumDir = new File(bookDirPath);

        if (!albumDir.exists() || !albumDir.isDirectory()) {
            log.warn("书源清理目录不存在：{}", bookDirPath);
            // 目录不存在仍需清理数据库记录
        } else {
            // 2. 删除当前专辑目录下的图片文件
            File[] files = albumDir.listFiles((d, name) -> {
                String lowerName = name.toLowerCase();
                return lowerName.endsWith(".txt");
            });

            int deletedFileCount = 0;
            int failedFileCount = 0;
            if (files != null) {
                for (File file : files) {
                    if (file.delete()) {
                        deletedFileCount++;
                    } else {
                        failedFileCount++;
                        log.warn("书源清理文件失败：{}", file.getAbsolutePath());
                    }
                }
            }
            log.info("书源目录清理完成：路径={}，成功删除={} 个，失败={} 个",
                    bookDirPath, deletedFileCount, failedFileCount);
        }
    }
}
