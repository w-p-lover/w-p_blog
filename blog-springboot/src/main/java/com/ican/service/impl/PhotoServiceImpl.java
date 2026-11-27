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
import com.ican.utils.PythonScriptRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.ican.constant.CommonConstant.FALSE;
import static com.ican.enums.FilePathEnum.PHOTO;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhotoServiceImpl extends ServiceImpl<PhotoMapper, Photo> implements PhotoService {

    private final PhotoMapper photoMapper;
    private final AlbumMapper albumMapper;
    private final UploadStrategyContext uploadStrategyContext;
    private final BlogFileMapper blogFileMapper;
    private final PythonScriptRunner pythonScriptRunner;
    private final AtomicInteger totalCount = new AtomicInteger(0);

    @Value("${spider.dir}")
    private String spiderDir;

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
            result.put("albumName", "Unknown album");
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
            log.error("Failed to persist uploaded photo metadata", e);
        }
        return url;
    }

    @Override
    public void runPythonSpider(AtomicReference<String> status, String albumName) {
        totalCount.set(0);
        if (albumName == null || albumName.trim().isEmpty()) {
            log.error("Album name is required");
            status.set("FAILED");
            return;
        }
        String normalizedAlbumName = albumName.trim();

        try {
            clearBeforeSpider(normalizedAlbumName);
            log.info("Photo spider cleanup finished for album={}", normalizedAlbumName);
        } catch (Exception e) {
            log.error("Photo spider cleanup failed for album={}", normalizedAlbumName, e);
            status.set("FAILED");
            return;
        }

        try {
            String scriptResource = getScriptByAlbum(normalizedAlbumName);
            File outputDir = resolveAlbumDir(normalizedAlbumName);
            List<String> scriptArgs = "static/pixiv.py".equals(scriptResource)
                    ? List.of()
                    : List.of("--save-dir", outputDir.getAbsolutePath());

            PythonScriptRunner.PythonExecutionResult result = pythonScriptRunner.run(
                    scriptResource,
                    scriptArgs,
                    line -> log.info("[photo-spider][{}] {}", normalizedAlbumName, line)
            );
            if (result.totalCount() != null) {
                totalCount.set(result.totalCount());
            }
            if (!result.isSuccess()) {
                log.error("Photo spider failed for album={}, exitCode={}, timedOut={}",
                        normalizedAlbumName, result.exitCode(), result.timedOut());
                status.set("FAILED");
                return;
            }

            status.set("INSERTING");
            insertAlbumImages(status, normalizedAlbumName);
            log.info("Photo spider import finished for album={}, status={}", normalizedAlbumName, status.get());
        } catch (Exception e) {
            log.error("Photo spider execution failed for album={}", normalizedAlbumName, e);
            status.set("FAILED");
        }
    }

    private String getScriptByAlbum(String albumName) {
        if (albumName.contains("壁纸")) {
            return "static/wall.py";
        }
        if (albumName.toLowerCase().contains("pixiv")) {
            return "static/pixiv.py";
        }
        return "static/wall.py";
    }

    private String getDirAlbum(String albumName) {
        if (albumName.contains("壁纸")) {
            return "Wallhaven";
        }
        if (albumName.toLowerCase().contains("pixiv")) {
            return "pixiv";
        }
        return "Wallhaven";
    }

    @Override
    public int getTotalCount() {
        return totalCount.get();
    }

    @Override
    public double getPhotoCount(String albumName) {
        File dir = resolveAlbumDir(albumName);
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("Photo directory does not exist: {}", dir.getAbsolutePath());
            return 0;
        }
        File[] files = dir.listFiles();
        return files == null ? 0 : files.length;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void insertAlbumImages(AtomicReference<String> status, String albumName) {
        Integer albumId = albumMapper.getIdByName(albumName);
        File albumDir = resolveAlbumDir(albumName);

        if (!albumDir.exists() && !albumDir.mkdirs()) {
            log.error("Failed to create album directory: {}", albumDir.getAbsolutePath());
            status.set("FAILED");
            return;
        }

        File[] files = albumDir.listFiles((dir, name) -> {
            String lowerName = name.toLowerCase();
            return lowerName.endsWith(".jpg") || lowerName.endsWith(".png") || lowerName.endsWith(".jpeg");
        });

        if (files == null || files.length == 0) {
            log.info("No images found for album={} in {}", albumName, albumDir.getAbsolutePath());
            status.set("COMPLETED");
            return;
        }

        String dirName = getDirAlbum(albumName);
        List<Photo> photos = Arrays.stream(files)
                .map(file -> Photo.builder()
                        .albumId(albumId)
                        .photoName(file.getName())
                        .photoUrl("http://localhost:8080/" + dirName + "/" + file.getName())
                        .build())
                .collect(Collectors.toList());

        this.saveBatch(photos);
        log.info("Inserted {} photos for album={}", photos.size(), albumName);
        status.set("COMPLETED");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void clearBeforeSpider(String albumName) {
        File albumDir = resolveAlbumDir(albumName);

        if (!albumDir.exists() || !albumDir.isDirectory()) {
            log.warn("Album cleanup directory does not exist: {}", albumDir.getAbsolutePath());
        } else {
            File[] files = albumDir.listFiles((dir, name) -> {
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
                        log.warn("Failed to delete photo file: {}", file.getAbsolutePath());
                    }
                }
            }
            log.info("Album cleanup finished: album={}, deleted={}, failed={}",
                    albumName, deletedFileCount, failedFileCount);
        }

        int deletedDbCount = photoMapper.delete(new LambdaQueryWrapper<Photo>()
                .like(Photo::getPhotoUrl, getDirAlbum(albumName)));
        log.info("Deleted {} photo records for album={}", deletedDbCount, albumName);
    }

    private File resolveAlbumDir(String albumName) {
        return new File(spiderDir, getDirAlbum(albumName));
    }
}
