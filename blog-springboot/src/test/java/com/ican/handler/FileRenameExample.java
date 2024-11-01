package com.ican.handler;

import org.checkerframework.checker.units.qual.A;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicInteger;

public class FileRenameExample {
    public static void main(String[] args) {
        String directoryPath = "F:\\javaProject\\light-edit\\blog\\blog-springboot\\src\\main\\resources\\bg_images"; // 文件夹路径
        try {
            renameFilesInDirectory(directoryPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void renameFilesInDirectory(String directoryPath) throws IOException {
        Path dirPath = Paths.get(directoryPath);
        AtomicInteger i = new AtomicInteger(1);
        // 遍历文件夹中的所有文件
        Files.walkFileTree(dirPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
                // 这里定义新的文件名，可以自定义修改规则
                String newFileName = "bg_" + i.getAndIncrement() + ".jpg";
                // 设置新的文件路径
                Path newFilePath = filePath.resolveSibling(newFileName);

                // 重命名文件
                Files.move(filePath, newFilePath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Renamed: " + filePath + " -> " + newFilePath);

                return FileVisitResult.CONTINUE;
            }
        });
    }
}
