package com.ican.utils;

import com.ican.exception.ServiceException;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * IP地址工具类
 *
 * @author xcs
 */
@SuppressWarnings("all")
public class IpUtils {

    private static Searcher searcher;

    static {
        // 解决项目打包找不到ip2region.xdb
        try {
            InputStream inputStream = new ClassPathResource("/ipdb/ip2region.xdb").getInputStream();
            //将 ip2region.db 转为 ByteArray
            byte[] cBuff = FileCopyUtils.copyToByteArray(inputStream);
            searcher = Searcher.newWithBuffer(cBuff);
        } catch (IOException e) {
            throw new ServiceException("ip2region.xdb加载失败");
        }

    }

    /**
     * 在Nginx等代理之后获取用户真实IP地址
     */
    public static String getIpAddress(HttpServletRequest request) {
        String ip;
        try {
            ip = request.getHeader("X-Real-IP");
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("x-forwarded-for");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_CLIENT_IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_X_FORWARDED_FOR");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
                if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
                    //根据网卡取本机配置的IP
                    InetAddress inet = null;
                    try {
                        inet = InetAddress.getLocalHost();
                    } catch (UnknownHostException e) {
                        throw new UnknownHostException("无法确定主机的IP地址");
                    }
                    ip = inet.getHostAddress();
                }
            }
            // 使用代理，则获取第一个IP地址
            if (!StringUtils.hasText(ip) && Objects.requireNonNull(ip).length() > 15) {
                int idx = ip.indexOf(",");
                if (idx > 0) {
                    ip = ip.substring(0, idx);
                }
            }
        } catch (Exception e) {
            ip = "";
        }
        return ip;
    }

    /**
     * 根据ip从 ip2region.db 中获取地理位置
     *
     * @param ip
     * @return
     */
    public static String getIpSource(String ip) {
        try {
            String address = searcher.searchByStr(ip);
            if (!StringUtils.hasText(address)) {
                return "";
            }

            // 如果已经是正确中文，直接返回（最安全）
            if (containsChinese(address) && !looksLikeGbkMisDecoded(address)) {
                return clean(address);
            }

            // 只修复“GBK 被 UTF-8 误解”的那一类
            if (looksLikeGbkMisDecoded(address)) {
                address = new String(address.getBytes("GBK"), StandardCharsets.UTF_8);
                return clean(address);
            }

            // 其他情况（包括 ����IP），直接返回原值或空
            return clean(address);
        } catch (Exception e) {
            return "";
        }
    }

    private static String clean(String address) {
        return address.replace("|0", "").replace("0|", "");
    }

    /**
     * 判断字符串里是否包含中文
     */
    private static boolean containsChinese(String str) {
        for (char c : str.toCharArray()) {
            if (c >= '\u4e00' && c <= '\u9fa5') {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否像“GBK → UTF-8 错解”的乱码
     */
    private static boolean looksLikeGbkMisDecoded(String str) {
        // 这些字符几乎只会出现在 GBK 乱码中
        return str.contains("鍐")
                || str.contains("綉")
                || str.contains("鏂")
                || str.contains("绯")
                || str.contains("鍖");
    }
}