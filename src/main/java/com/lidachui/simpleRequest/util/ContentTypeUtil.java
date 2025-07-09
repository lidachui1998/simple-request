package com.lidachui.simpleRequest.util;

import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;

/**
 * 内容类型工具类
 *
 * @author: lihuijie
 * @date: 2025/5/9 16:20
 * @version: 1.0
 */
public class ContentTypeUtil {

    private static final Set<String> BINARY_PREFIXES = new HashSet<>(Arrays.asList(
        "application/octet-stream",
        "application/pdf",
        "image/",
        "audio/",
        "video/",
        "application/zip",
        "application/x-rar-compressed",
        "application/x-tar",
        "application/x-7z-compressed",
        "application/x-bzip2",
        "application/x-gzip"
    ));

    private static final Set<String> TEXT_PREFIXES = new HashSet<>(Arrays.asList(
        "text/",
        "application/json",
        "application/xml",
        "application/javascript",
        "application/x-www-form-urlencoded"
    ));

    public static boolean isBinaryContentType(String contentType) {
        if (contentType == null || contentType.trim().isEmpty()) {
            return false;
        }

        String lowerContentType = contentType.toLowerCase().trim();

        // 先检查是否为明确的文本类型
        for (String textPrefix : TEXT_PREFIXES) {
            if (lowerContentType.startsWith(textPrefix)) {
                return false;
            }
        }

        // 再检查是否为二进制类型
        for (String binaryPrefix : BINARY_PREFIXES) {
            if (lowerContentType.startsWith(binaryPrefix)) {
                return true;
            }
        }

        return false;
    }
}
