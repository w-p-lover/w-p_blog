package com.ican.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ican.entity.Album;
import com.ican.entity.BlogFile;
import com.ican.entity.Photo;
import com.ican.mapper.AlbumMapper;
import com.ican.mapper.BlogFileMapper;
import com.ican.mapper.PhotoMapper;
import com.ican.model.dto.ConditionDTO;
import com.ican.model.dto.PhotoDTO;
import com.ican.model.dto.PhotoInfoDTO;
import com.ican.model.vo.AlbumBackVO;
import com.ican.model.vo.PageResult;
import com.ican.model.vo.PhotoBackVO;
import com.ican.model.vo.PhotoVO;
import com.ican.service.PhotoService;
import com.ican.strategy.context.UploadStrategyContext;
import com.ican.utils.BeanCopyUtils;
import com.ican.utils.FileUtils;
import com.ican.utils.PageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.ican.constant.CommonConstant.FALSE;
import static com.ican.enums.FilePathEnum.PHOTO;

/**
 * 照片业务接口实现类
 *
 * @author xcs
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PhotoServiceImpl extends ServiceImpl<PhotoMapper, Photo> implements PhotoService {

    private final PhotoMapper photoMapper;
    private final AlbumMapper albumMapper;
    private final UploadStrategyContext uploadStrategyContext;
    private final BlogFileMapper blogFileMapper;

    // 从配置文件注入，避免硬编码
    @Value("${spider.wallhaven.dir}")
    private String wallhavenDir;
    @Value("${spider.python.cmd}")
    private String pythonCmd;
    @Value("${spider.photo.album.id}")
    private Integer albumId;

    // ---------------------- 原有业务方法（无需修改） ----------------------
    @Override
    public PageResult<PhotoBackVO> listPhotoBackVO(ConditionDTO condition) {
        Long count = photoMapper.selectCount(new LambdaQueryWrapper<Photo>()
                .eq(Objects.nonNull(condition.getAlbumId()), Photo::getAlbumId, condition.getAlbumId()));
        if (count == 0) {
            return new PageResult<>();
        }
        List<PhotoBackVO> photoList = photoMapper.selectPhotoBackVOList(PageUtils.getLimit(),
                PageUtils.getSize(), condition.getAlbumId());
        return new PageResult<>(photoList, count);
    }

    @Override
    public AlbumBackVO getAlbumInfo(Integer albumId) {
        AlbumBackVO albumBackVO = albumMapper.selectAlbumInfoById(albumId);
        if (Objects.isNull(albumBackVO)) {
            return null;
        }
        Long photoCount = photoMapper.selectCount(new LambdaQueryWrapper<Photo>()
                .eq(Photo::getAlbumId, albumId));
        albumBackVO.setPhotoCount(photoCount);
        return albumBackVO;
    }

    @Override
    @Transactional
    public void addPhoto(PhotoDTO photo) {
        List<Photo> pictureList = photo.getPhotoUrlList().stream()
                .map(url -> Photo.builder()
                        .albumId(photo.getAlbumId())
                        .photoName(IdWorker.getIdStr())
                        .photoUrl(url)
                        .build())
                .collect(Collectors.toList());
        this.saveBatch(pictureList);
    }

    @Override
    public void updatePhoto(PhotoInfoDTO photoInfo) {
        Photo photo = BeanCopyUtils.copyBean(photoInfo, Photo.class);
        baseMapper.updateById(photo);
    }

    @Override
    public void deletePhoto(List<Integer> photoIdList) {
        baseMapper.deleteBatchIds(photoIdList);
    }

    @Override
    @Transactional
    public void movePhoto(PhotoDTO photo) {
        List<Photo> photoList = photo.getPhotoIdList().stream()
                .map(photoId -> Photo.builder()
                        .id(photoId)
                        .albumId(photo.getAlbumId())
                        .build())
                .collect(Collectors.toList());
        this.updateBatchById(photoList);
    }

    @Override
    public Map<String, Object> listPhotoVO(ConditionDTO condition) {
        Map<String, Object> result = new HashMap<>(2);
        Album album = albumMapper.selectOne(new LambdaQueryWrapper<Album>()
                .select(Album::getAlbumName).eq(Album::getId, condition.getAlbumId()));
        if (Objects.isNull(album)) {
            result.put("albumName", "未知相册");
            result.put("photoVOList", Collections.emptyList());
            return result;
        }
        List<PhotoVO> photoVOList = photoMapper.selectPhotoVOList(condition.getAlbumId());
        result.put("albumName", album.getAlbumName());
        result.put("photoVOList", photoVOList);
        return result;
    }

    @Override
    public String uploadPhoto(MultipartFile file) {
        String url = uploadStrategyContext.executeUploadStrategy(file, PHOTO.getPath());
        try {
            String md5 = FileUtils.getMd5(file.getInputStream());
            String extName = FileUtils.getExtension(file);
            BlogFile existFile = blogFileMapper.selectOne(new LambdaQueryWrapper<BlogFile>()
                    .select(BlogFile::getId)
                    .eq(BlogFile::getFileName, md5)
                    .eq(BlogFile::getFilePath, PHOTO.getFilePath()));
            if (Objects.isNull(existFile)) {
                BlogFile newFile = BlogFile.builder()
                        .fileUrl(url)
                        .fileName(md5)
                        .filePath(PHOTO.getFilePath())
                        .extendName(extName)
                        .fileSize((int) file.getSize())
                        .isDir(FALSE)
                        .build();
                blogFileMapper.insert(newFile);
            }
        } catch (IOException e) {
            log.error("上传照片时保存文件信息失败", e); // 打印完整堆栈，方便排查
        }
        return url;
    }

    /**
     * 运行Python爬虫：移除@Transactional，避免事务包含长时间爬虫操作
     */
    @Override
    public void runPythonSpider(AtomicReference<String> status) {
        // 1. 先清理旧数据（独立事务，执行完立即提交，释放锁）
        try {
            clearBeforeSpider();
            log.info("爬虫前置清理完成");
        } catch (Exception e) {
            log.error("爬虫前置清理失败", e);
            status.set("FAILED"); // 显式标记失败状态
            return;
        }

        // 2. 运行Python爬虫：管理临时文件和进程资源
        Process process = null;
        File tempFile = null;
        try {
            // 读取classpath下的python脚本，创建临时文件
            ClassPathResource resource = new ClassPathResource("static/wall.py");
            tempFile = File.createTempFile("photo_spider_", ".py"); // 临时文件名加前缀，便于识别
            Files.copy(resource.getInputStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("创建Python临时脚本：{}", tempFile.getAbsolutePath());

            // 启动爬虫进程
            ProcessBuilder pb = new ProcessBuilder(pythonCmd, tempFile.getAbsolutePath());
            pb.redirectErrorStream(true); // 错误流和输出流合并，方便日志查看
            process = pb.start();

            // 读取爬虫日志
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[爬虫日志] {}", line); // 统一用log，支持日志归档
                }
            }

            // 等待爬虫执行完成，获取退出码
            int exitCode = process.waitFor();
            log.info("爬虫执行完成，退出码：{}（0表示成功）", exitCode);
            if (exitCode != 0) {
                log.error("爬虫执行失败，退出码非0：{}", exitCode);
                status.set("FAILED");
                return;
            }

            // 3. 插入新数据（独立事务，执行完立即提交）
            status.set("INSERTING");
            insertImages(status);
            log.info("爬虫图片插入完成，最终状态：{}", status.get());

        } catch (Exception e) {
            log.error("爬虫执行过程异常", e); // 打印完整堆栈，而非仅消息
            status.set("FAILED");
        } finally {
            // 强制释放资源：销毁进程 + 删除临时文件
            if (process != null && process.isAlive()) {
                process.destroy();
                log.info("强制销毁爬虫进程");
            }
            if (tempFile != null && tempFile.exists() && !tempFile.delete()) {
                log.warn("临时Python脚本删除失败：{}", tempFile.getAbsolutePath());
            }
        }
    }

    @Override
    public double getPhotoCount() {
        File dir = new File(wallhavenDir);
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("统计照片数量时，目录不存在：{}", wallhavenDir);
            return 0;
        }
        File[] files = dir.listFiles();
        return files == null ? 0 : files.length;
    }

    /**
     * 插入爬虫图片：独立事务（REQUIRES_NEW），与外层爬虫逻辑解耦
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void insertImages(AtomicReference<String> status) {
        File dir = new File(wallhavenDir);
        if (!dir.exists() || !dir.isDirectory()) {
            log.error("插入图片时目录不存在：{}", dir.getAbsolutePath());
            status.set("FAILED");
            return;
        }

        // 过滤图片文件（jpg/png/jpeg）
        File[] files = dir.listFiles((d, name) -> {
            String lowerName = name.toLowerCase();
            return lowerName.endsWith(".jpg") || lowerName.endsWith(".png") || lowerName.endsWith(".jpeg");
        });

        if (files == null || files.length == 0) {
            log.info("目录下无图片文件：{}", dir.getAbsolutePath());
            status.set("COMPLETED"); // 无文件也算执行完成，而非失败
            return;
        }

        // 转换为Photo列表并批量插入
        List<Photo> photos = Arrays.stream(files)
                .map(file -> {
                    String fileName = file.getName();
                    String url = "http://localhost:8080/Wallhaven/" + fileName;
                    return Photo.builder()
                            .albumId(albumId) // 从配置注入，避免硬编码1
                            .photoName(fileName)
                            .photoUrl(url)
                            .build();
                })
                .collect(Collectors.toList());

        this.saveBatch(photos);
        log.info("成功插入图片：{} 张（目录：{}）", photos.size(), dir.getAbsolutePath());
        status.set("COMPLETED");
    }

    /**
     * 清理旧数据：独立事务（REQUIRES_NEW），删除文件+删除数据库记录，执行完立即提交
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void clearBeforeSpider() {
        File dir = new File(wallhavenDir);
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("清理旧数据时目录不存在：{}", wallhavenDir);
            return;
        }

        // 1. 删除目录下的图片文件（统计成功/失败数量，便于排查）
        File[] files = dir.listFiles((d, name) -> {
            String lowerName = name.toLowerCase();
            return lowerName.endsWith(".jpg") || lowerName.endsWith(".png") || lowerName.endsWith(".jpeg");
        });

        int deletedFileCount = 0;
        int failedFileCount = 0;
        if (files != null) {
            for (File file : files) {
                if (file.delete()) {
                    deletedFileCount++;
                } else {
                    failedFileCount++;
                    log.warn("清理文件失败：{}（可能被占用）", file.getAbsolutePath());
                }
            }
        }
        log.info("清理目录完成：路径={}，成功删除={} 个，失败={} 个",
                wallhavenDir, deletedFileCount, failedFileCount);

        // 2. 删除数据库中对应记录（独立事务，执行完立即提交，释放锁）
        int deletedDbCount = photoMapper.delete(new LambdaQueryWrapper<Photo>()
                .like(Photo::getPhotoUrl, "Wallhaven"));
        log.info("清理数据库记录完成：删除 {} 条", deletedDbCount);
    }
}