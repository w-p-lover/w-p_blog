package com.ican.strategy.impl;

import cn.hutool.core.util.ByteUtil;
import com.ican.config.properties.TinyProperites;
import com.ican.exception.ServiceException;
import com.ican.strategy.UploadStrategy;
import com.ican.utils.FileUtils;
import com.tinify.Tinify;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 抽象上传模板
 *
 * @author ican
 */
@Service
public abstract class AbstractUploadStrategyImpl implements UploadStrategy {
    @Autowired
    private TinyProperites tinyProperites;

    @Override
    public String uploadFile(MultipartFile file, String path) {
        try {
            //初始化Tinify，设置密钥，对大于2MB的图片进行压缩处理
            Tinify.setKey(tinyProperites.getKey());
            // 获取文件md5值
            String md5 = FileUtils.getMd5(file.getInputStream());
            // 获取文件扩展名
            String extName = FileUtils.getExtension(file);
            // 重新生成文件名
            String fileName = md5 + "." + extName;
            // 判断文件是否已存在
            if (!exists(path + fileName)) {
                long fileSize = file.getSize();
                if (fileSize < 1024L * 1024 * 1024 * 2) {
                    upload(path, fileName, file.getInputStream());
                } else {
                    long strat = System.currentTimeMillis();
                    // 使用 Tinify API 进行压缩
                    byte[] resultData = Tinify.fromBuffer(file.getBytes()).toBuffer();
                    System.out.println(System.currentTimeMillis() - strat / 1000 + "s");
                    // 不存在则继续上传
                    upload(path, fileName, new ByteArrayInputStream(resultData));
                }
            }
            // 返回文件访问路径
            return getFileAccessUrl(path + fileName);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException("文件上传失败");
        }
    }

    /**
     * 判断文件是否存在
     *
     * @param filePath 文件路径
     * @return {@link Boolean}
     */
    public abstract Boolean exists(String filePath);

    /**
     * 上传
     *
     * @param path        路径
     * @param fileName    文件名
     * @param inputStream 输入流
     * @throws IOException io异常
     */
    public abstract void upload(String path, String fileName, InputStream inputStream) throws IOException;

    /**
     * 获取文件访问url
     *
     * @param filePath 文件路径
     * @return {@link String} 文件url
     */
    public abstract String getFileAccessUrl(String filePath);
}
