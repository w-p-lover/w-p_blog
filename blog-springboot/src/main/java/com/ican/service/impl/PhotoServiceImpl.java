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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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

    private static final String WALLHAVEN_DIR = System.getProperty("user.dir") + "/blog-springboot/src/main/resources/static/Wallhaven";

    @Override
    public PageResult<PhotoBackVO> listPhotoBackVO(ConditionDTO condition) {
        // 查询照片数量
        Long count = photoMapper.selectCount(new LambdaQueryWrapper<Photo>()
                .eq(Objects.nonNull(condition.getAlbumId()), Photo::getAlbumId, condition.getAlbumId()));
        if (count == 0) {
            return new PageResult<>();
        }
        // 查询照片列表
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
        // 批量保存照片
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
        String albumName = albumMapper.selectOne(new LambdaQueryWrapper<Album>()
                        .select(Album::getAlbumName).eq(Album::getId, condition.getAlbumId()))
                .getAlbumName();
        List<PhotoVO> photoVOList = photoMapper.selectPhotoVOList(condition.getAlbumId());
        result.put("albumName", albumName);
        result.put("photoVOList", photoVOList);
        return result;
    }

    @Override
    public String uploadPhoto(MultipartFile file) {
        // 上传文件
        String url = uploadStrategyContext.executeUploadStrategy(file, PHOTO.getPath());
        try {
            // 获取文件md5值
            String md5 = FileUtils.getMd5(file.getInputStream());
            // 获取文件扩展名
            String extName = FileUtils.getExtension(file);
            BlogFile existFile = blogFileMapper.selectOne(new LambdaQueryWrapper<BlogFile>()
                    .select(BlogFile::getId)
                    .eq(BlogFile::getFileName, md5)
                    .eq(BlogFile::getFilePath, PHOTO.getFilePath()));
            if (Objects.isNull(existFile)) {
                // 保存文件信息
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
            e.printStackTrace();
        }
        return url;
    }

    @Override
    @Transactional
    public void runPythonSpider(AtomicReference<String> status) {
        // 删除之前爬虫数据
        clearBeforeSpider();
        // 运行爬虫
        try {
            ClassPathResource resource = new ClassPathResource("static/wall.py");
            File tempFile = File.createTempFile("photo", ".py");
            Files.copy(resource.getInputStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            String pythonCmd = "python"; // 最好改成配置项
            ProcessBuilder pb = new ProcessBuilder(
                    pythonCmd,
                    tempFile.getAbsolutePath()
/*                "--language", language != null ? language : "",
                "--category", category != null ? category : "");*/
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            System.out.println("--------------------------爬虫执行--------------------------");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[爬虫日志] " + line);
                }
            }
            int exitCode = process.waitFor();
            System.out.println("-------------------爬虫执行完成，退出码：" + exitCode + "-------------------");
            insertImages(status);

        } catch (Exception e) {
            log.error("爬虫任务发生异常：{}", e.getMessage());
        }
    }

    @Transactional
    public void insertImages(AtomicReference<String> status) {
        // 获取爬虫图片
        File dir = new File(WALLHAVEN_DIR);
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("目录不存在：" + dir.getAbsolutePath());
            return;
        }
        File[] files = dir.listFiles((d, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".jpg") || lower.endsWith(".png") || lower.endsWith(".jpeg");
        });
        if (files == null || files.length == 0) {
            System.out.println("目录下没有图片文件");
            return;
        }
        List<Photo> photos = Arrays.stream(files)
                .map(file -> {
                    String fileName = file.getName();
                    String url = "http://localhost:8080/Wallhaven/" + fileName;
                    return Photo.builder()
                            .albumId(1)
                            .photoName(fileName)
                            .photoUrl(url)
                            .build();
                })
                .collect(Collectors.toList());
        this.saveBatch(photos);
        System.out.println("成功插入 " + photos.size() + " 张图片");

        status.set("COMPLETED");
    }

    @Transactional
    public void clearBeforeSpider() {
        String wallhavenPath;
        File dir = new File(WALLHAVEN_DIR);
        wallhavenPath = dir.getAbsolutePath();

        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("目录不存在：" + wallhavenPath);
            return;
        }

        // 遍历并删除文件
        File[] files = dir.listFiles((d, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".jpg") || lower.endsWith(".png") || lower.endsWith(".jpeg");
        });

        if (files != null) {
            for (File file : files) {
                if (file.delete()) {
                    System.out.println("已删除文件：" + file.getName());
                } else {
                    System.out.println("删除失败：" + file.getName());
                }
            }
        }
        System.out.println("Wallhaven 文件夹已清空，路径：" + wallhavenPath);
        int deleted = photoMapper.delete(new LambdaQueryWrapper<Photo>()
                .like(Photo::getPhotoUrl, "Wallhaven"));
        System.out.println("已删除数据库记录：" + deleted + " 条");
    }

}