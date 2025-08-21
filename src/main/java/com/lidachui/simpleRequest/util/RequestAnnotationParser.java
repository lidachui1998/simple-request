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
        if (queryParamArray == null || queryParamArray.length == 0) {
            return Collections.emptyMap();
        }

        Map<String, String> queryParams = new HashMap<>(queryParamArray.length * 2);
        for (String queryParam : queryParamArray) {
            int idx = queryParam.indexOf('=');
            if (idx > 0 && idx < queryParam.length() - 1) {
                String key = queryParam.substring(0, idx).trim();
                String value = queryParam.substring(idx + 1).trim();
                queryParams.put(key, value);
            }
        }
        return queryParams;
    }

    public static Map<String, String> parseHeaders(String[] headers) {
        if (headers == null || headers.length == 0) {
            return Collections.emptyMap();
        }

        Map<String, String> headerMap = new HashMap<>(headers.length * 2);
        for (String header : headers) {
            int idx = header.indexOf(':');
            if (idx > 0 && idx < header.length() - 1) {
                String key = header.substring(0, idx).trim();
                String value = header.substring(idx + 1).trim();
                headerMap.put(key, value);
            }
        }
        return headerMap;
    }
}

