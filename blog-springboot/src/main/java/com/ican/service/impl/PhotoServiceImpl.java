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
import java.util.concurrent.atomic.AtomicInteger;
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
    private final AtomicInteger totalCount = new AtomicInteger(0);
    // 从配置文件注入，避免硬编码
    @Value("${spider.dir}")
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

    @Override
    public void runPythonSpider(AtomicReference<String> status, String albumName) {
        totalCount.set(0);
        // 校验专辑名称（避免空值导致的后续问题）
        if (albumName == null || albumName.trim().isEmpty()) {
            log.error("专辑名称不能为空，终止爬虫任务");
            status.set("FAILED");
            return;
        }
        String normalizedAlbumName = albumName.trim(); // 标准化专辑名称（去空格）

        // 1. 按专辑清理旧数据（只清理当前专辑的历史数据，而非全部）
        try {
            clearBeforeSpider(normalizedAlbumName); // 修改清理方法，增加专辑参数
            log.info("专辑[{}]的爬虫前置清理完成", normalizedAlbumName);
        } catch (Exception e) {
            log.error("专辑[{}]的爬虫前置清理失败", normalizedAlbumName, e);
            status.set("FAILED");
            return;
        }

        // 2. 运行Python爬虫（根据专辑名称选择脚本或传递参数）
        Process process = null;
        File tempFile = null;
        try {
            // 2.1 根据专辑名称选择不同的Python脚本（或传递参数）
            String scriptResource = getScriptByAlbum(normalizedAlbumName); // 动态选择脚本
            ClassPathResource resource = new ClassPathResource(scriptResource);

            // 2.2 创建临时脚本文件
            tempFile = File.createTempFile("photo_spider_" + normalizedAlbumName + "_", ".py");
            Files.copy(resource.getInputStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("专辑[{}]的Python临时脚本创建完成：{}", normalizedAlbumName, tempFile.getAbsolutePath());

            // 2.3 启动爬虫进程（传递专辑名称作为参数给Python脚本）
            ProcessBuilder pb = new ProcessBuilder(
                    pythonCmd,
                    tempFile.getAbsolutePath(),
                    normalizedAlbumName // 传递专辑名称给Python脚本
            );
            pb.redirectErrorStream(true);
            process = pb.start();

            // 2.4 读取爬虫日志（增加专辑标识）
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[全局爬虫日志] {}", line);

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
            log.info("专辑[{}]的爬虫执行完成，退出码：{}（0表示成功）", normalizedAlbumName, exitCode);
            if (exitCode != 0) {
                log.error("专辑[{}]的爬虫执行失败，退出码非0", normalizedAlbumName);
                status.set("FAILED");
                return;
            }

            // 3. 按专辑插入新数据（将图片与当前专辑关联）
            status.set("INSERTING");
            insertAlbumImages(status, normalizedAlbumName); // 修改插入方法，增加专辑参数
            log.info("专辑[{}]的图片插入完成，最终状态：{}", normalizedAlbumName, status.get());

        } catch (Exception e) {
            log.error("专辑[{}]的爬虫执行过程异常", normalizedAlbumName, e);
            status.set("FAILED");
        } finally {
            // 强制释放资源
            if (process != null && process.isAlive()) {
                process.destroy();
                log.info("专辑[{}]的爬虫进程已强制销毁", normalizedAlbumName);
            }
            if (tempFile != null && tempFile.exists() && !tempFile.delete()) {
                log.warn("专辑[{}]的临时Python脚本删除失败：{}", normalizedAlbumName, tempFile.getAbsolutePath());
            }
        }
    }


    /**
     * 根据专辑名称选择对应的Python脚本
     * （可根据实际业务扩展，比如不同专辑用不同爬虫逻辑）
     */
    private String getScriptByAlbum(String albumName) {
        // 示例：如果是"风景"专辑，用专门的脚本；其他用默认脚本
        if (albumName.contains("壁纸")) {
            return "static/wall.py";
        } else if (albumName.contains("pixiv")) {
            return "static/pixiv.py";
        }
        return "static/wall.py";
    }

    /**
     * 根据专辑名称选择对应的Python脚本
     * （可根据实际业务扩展，比如不同专辑用不同爬虫逻辑）
     */
    private String getDirAlbum(String albumName) {
        // 示例：如果是"风景"专辑，用专门的脚本；其他用默认脚本
        if (albumName.contains("壁纸")) {
            return "Wallhaven";
        } else if (albumName.contains("pixiv")) {
            return "pixiv";
        }
        return "Wallhaven";
    }


    public int getTotalCount() {
        return totalCount.get();
    }
    @Override
    public double getPhotoCount(String albumName) {
        File dir = new File(wallhavenDir + getDirAlbum(albumName));
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("统计照片数量时，目录不存在：{}", wallhavenDir + getDirAlbum(albumName));
            return 0;
        }
        File[] files = dir.listFiles();
        return files == null ? 0 : files.length;
    }

    /**
     * 插入爬虫图片：独立事务（REQUIRES_NEW），与外层爬虫逻辑解耦
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void insertAlbumImages(AtomicReference<String> status, String albumName) {
        // 1. 为当前专辑创建独立目录（避免不同专辑文件混淆）
        Integer albumId = albumMapper.getIdByName(albumName);
        String albumDirPath = wallhavenDir + getDirAlbum(albumName) ;
        File albumDir = new File(albumDirPath);

        // 确保目录存在（不存在则创建）
        if (!albumDir.exists() && !albumDir.mkdirs()) {
            log.error("创建专辑目录失败：{}", albumDirPath);
            status.set("FAILED");
            return;
        }

        // 2. 过滤当前专辑目录下的图片文件
        File[] files = albumDir.listFiles((d, name) -> {
            String lowerName = name.toLowerCase();
            return lowerName.endsWith(".jpg") || lowerName.endsWith(".png") || lowerName.endsWith(".jpeg");
        });

        if (files == null || files.length == 0) {
            log.info("专辑[{}]目录下无图片文件：{}", albumName, albumDirPath);
            status.set("COMPLETED");
            return;
        }

        // 3. 转换为Photo列表（关联当前专辑信息）
        List<Photo> photos = Arrays.stream(files)
                .map(file -> {
                    String fileName = file.getName();
                    // URL包含专辑标识，便于前端区分
                    String url = "http://localhost:8080/" + getDirAlbum(albumName) + "/" + fileName;
                    return Photo.builder()
                            .albumId(albumId) // 关联专辑ID
                            .photoName(fileName)
                            .photoUrl(url)
                            .build();
                })
                .collect(Collectors.toList());

        // 4. 批量插入数据库（只插入当前专辑的图片）
        this.saveBatch(photos);
        log.info("专辑[{}]成功插入图片：{} 张（目录：{}）",
                albumName, photos.size(), albumDirPath);
        status.set("COMPLETED");
    }

    /**
     * 清理旧数据：只清理当前专辑的文件和数据库记录
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void clearBeforeSpider(String albumName) {
        // 1. 定位当前专辑的目录
        String albumDirPath = wallhavenDir + getDirAlbum(albumName);
        File albumDir = new File(albumDirPath);

        if (!albumDir.exists() || !albumDir.isDirectory()) {
            log.warn("专辑[{}]清理目录不存在：{}", albumName, albumDirPath);
            // 目录不存在仍需清理数据库记录
        } else {
            // 2. 删除当前专辑目录下的图片文件
            File[] files = albumDir.listFiles((d, name) -> {
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
                        log.warn("专辑[{}]清理文件失败：{}", albumName, file.getAbsolutePath());
                    }
                }
            }
            log.info("专辑[{}]目录清理完成：路径={}，成功删除={} 个，失败={} 个",
                    albumName, albumDirPath, deletedFileCount, failedFileCount);
        }

        // 3. 删除数据库中当前专辑的记录（精准匹配，避免影响其他专辑）
        int deletedDbCount = photoMapper.delete(new LambdaQueryWrapper<Photo>()
                .like(Photo::getPhotoUrl, getDirAlbum(albumName)));
        log.info("专辑[{}]数据库记录清理完成：删除 {} 条", albumName, deletedDbCount);
    }
}