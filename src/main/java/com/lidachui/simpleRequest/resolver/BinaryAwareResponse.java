package com.lidachui.simpleRequest.resolver;

import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 支持二进制和文本响应的响应类
 *
 * @author: lihuijie
 * @date: 2025/5/9 16:00
 * @version: 1.0
 */
@Getter
public class BinaryAwareResponse extends Response {

  /**
   * -- GETTER --
   * 获取原始字节数组，无论内容类型如何
   */
  private final byte[] rawBytes;

    private final boolean binaryContent;

    public BinaryAwareResponse(byte[] bytes, Map<String, String> headers, boolean isBinary) {
        super(bytes, headers); // 暂时初始化body为bytes，后面会根据需要转换
        this.rawBytes = bytes;
        this.binaryContent = isBinary;
    }

    @Override
    public Object getBody() {
        // 如果之前设置过body（例如通过setBody方法），直接返回
        Object currentBody = super.getBody();
        if (currentBody != rawBytes) {
            return currentBody;
        }

        // 否则，根据内容类型决定返回字节数组还是字符串
        return binaryContent ? rawBytes : new String(rawBytes, StandardCharsets.UTF_8);
    }

}
