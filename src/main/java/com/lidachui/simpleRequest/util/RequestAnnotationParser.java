package com.lidachui.simpleRequest.util;

import java.util.*;

/**
 * RequestAnnotationParser
 *
 * @author: lihuijie
 * @date: 2024/11/25 22:29
 * @version: 1.0
 */
public class RequestAnnotationParser {

    public static Map<String, String> parseQueryParams(String[] queryParamArray) {
        Map<String, String> queryParams = new HashMap<>();
        if (queryParamArray != null) {
            for (String queryParam : queryParamArray) {
                String[] split = queryParam.split("=", 2);
                if (split.length == 2) {
                    queryParams.put(split[0].trim(), split[1].trim());
                }
            }
        }
        return queryParams;
    }

    public static Map<String, String> parseHeaders(String[] headers) {
        Map<String, String> headerMap = new HashMap<>();
        if (headers != null) {
            for (String header : headers) {
                String[] split = header.split(":", 2);
                if (split.length == 2) {
                    headerMap.put(split[0].trim(), split[1].trim());
                }
            }
        }
        return headerMap;
    }
}
