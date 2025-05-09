package com.lidachui.simpleRequest.resolver;

import com.lidachui.simpleRequest.serialize.Serializer;

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
            if (response instanceof BinaryAwareResponse) {
                return (T) ((BinaryAwareResponse) response).getRawBytes();
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
        if (response instanceof BinaryAwareResponse) {
            BinaryAwareResponse binaryResponse = (BinaryAwareResponse) response;
            if (binaryResponse.isBinaryContent()) {
                // 二进制响应但需要转换为对象，先转为字符串
                String bodyStr = new String(binaryResponse.getRawBytes(), StandardCharsets.UTF_8);
                return serializer.deserialize(bodyStr, responseType);
            }
        }

        return serializer.deserialize(response.getBody().toString(), responseType);
    }
}
