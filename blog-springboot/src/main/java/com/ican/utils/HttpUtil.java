package com.ican.utils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class HttpUtil {
    public static String get(String url, Map<String, String> params) {
        HttpURLConnection conn = null;
        try {
            // 1. 拼接参数（处理已有?的情况）
            StringBuilder paramStr = new StringBuilder();
            if (params != null && !params.isEmpty()) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    if (paramStr.length() > 0) paramStr.append("&");
                    paramStr.append(entry.getKey())
                            .append("=")
                            .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
                }
                url += (url.contains("?") ? "&" : "?") + paramStr;
            }

            // 2. 打开连接
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            // 允许接收压缩数据（可选，部分服务器需要显式声明）
            conn.setRequestProperty("Accept-Encoding", "gzip, deflate");

            // 3. 获取响应编码（优先从响应头提取）
            String charset = getCharsetFromHeader(conn);

            // 4. 处理响应流（判断是否需要解压）
            InputStream inputStream = getInputStreamWithDecompression(conn);
            if (inputStream == null) {
                throw new RuntimeException("无法获取响应流，响应码：" + conn.getResponseCode());
            }

            // 5. 读取响应（使用检测到的编码）
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, charset))) {
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                return result.toString();
            }

        } catch (Exception e) {
            throw new RuntimeException("HTTP请求失败：" + e.getMessage(), e);
        } finally {
            if (conn != null) {
                conn.disconnect(); // 关闭连接
            }
        }
    }

    /**
     * 从响应头提取编码（优先级：Content-Type中的charset > 默认UTF-8）
     */
    private static String getCharsetFromHeader(HttpURLConnection conn) {
        String contentType = conn.getHeaderField("Content-Type");
        if (contentType != null && contentType.contains("charset=")) {
            // 提取charset（如 "text/html; charset=GBK" → "GBK"）
            String charset = contentType.split("charset=")[1].trim();
            // 处理可能的引号（如 charset="UTF-8" → 去掉引号）
            if (charset.startsWith("\"") && charset.endsWith("\"")) {
                charset = charset.substring(1, charset.length() - 1);
            }
            return charset;
        }
        // 默认编码（如果接口明确用其他编码，可在此处修改，如"GBK"）
        return "UTF-8";
    }

    /**
     * 处理压缩的响应流（gzip/deflate）
     */
    private static InputStream getInputStreamWithDecompression(HttpURLConnection conn) throws IOException {
        InputStream inputStream;
        // 判断响应码，2xx为成功响应
        if (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300) {
            inputStream = conn.getInputStream();
        } else {
            inputStream = conn.getErrorStream(); // 错误响应流
        }

        // 检查是否需要解压（根据Content-Encoding）
        String contentEncoding = conn.getHeaderField("Content-Encoding");
        if (contentEncoding != null) {
            if (contentEncoding.contains("gzip")) {
                inputStream = new GZIPInputStream(inputStream); // gzip解压
            } else if (contentEncoding.contains("deflate")) {
                // 如需处理deflate压缩，可在此处添加（较少见）
                // inputStream = new InflaterInputStream(inputStream);
            }
        }
        return inputStream;
    }
}
