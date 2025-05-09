package com.lidachui.simpleRequest.util;

import java.util.Arrays;
import java.util.List;

/**
 * 内容类型工具类
 *
 * @author: lihuijie
 * @date: 2025/5/9 16:20
 * @version: 1.0
 */
public class ContentTypeUtil {
    
    private static final List<String> BINARY_CONTENT_TYPES = Arrays.asList(
        "application/octet-stream",
        "application/pdf",
        "image/",
        "audio/",
        "video/",
        "application/zip",
        "application/x-rar-compressed",
        "application/x-tar"
    );
    
    /**
     * 判断内容类型是否为二进制
     *
     * @param contentType 内容类型
     * @return 是否为二进制类型
     */
    public static boolean isBinaryContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        
        return BINARY_CONTENT_TYPES.stream()
                .anyMatch(contentType::contains);
    }
}
