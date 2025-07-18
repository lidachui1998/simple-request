package com.lidachui.simpleRequest.resolver;

import com.lidachui.simpleRequest.serialize.Serializer;

import com.lidachui.simpleRequest.util.ObjectUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;


import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * DefaultResponseBuilder
 *
 * @author: lihuijie
 * @date: 2024/11/23 14:08
 * @version: 1.0
 */
public class DefaultResponseBuilder extends AbstractResponseBuilder {

    @Override
    public <T> T buildResponse(Response response, Type responseType) {
        // 处理二进制响应
        if (responseType == byte[].class) {
            if (response instanceof ByteResponse) {
                return (T) ((ByteResponse) response).getRawBytes();
            }
            if (response.getBody() instanceof byte[]) {
                return (T) response.getBody();
            }
            if (response.getBody() instanceof String) {
                return (T) ((String) response.getBody()).getBytes(StandardCharsets.UTF_8);
            }
        }

        // 处理字符串序列化响应
        Serializer serializer = getSerializer();
        if (response instanceof ByteResponse) {
            ByteResponse binaryResponse = (ByteResponse) response;
            return serializer.deserialize(binaryResponse.getRawBytes(), responseType);
        }

        try {
            return serializer.deserialize(
                    ObjectUtil.objectToByteArrayUniversal(response.getBody()), responseType);
        } catch (IOException e) {
            ExceptionUtils.rethrow(e);
        }
        return null;
    }
}
