package com.lidachui.simpleRequest.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * ObjectUtil
 *
 * @author: lihuijie
 * @date: 2025/5/29 14:32
 * @version: 1.0
 */
public class ObjectUtil {

    /**
     * 对象到字节数组
     *
     * @param obj 对象
     * @return {@code byte[] }
     * @throws IOException IOException
     */
    public static byte[] objectToByteArray(Object obj) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(obj);
        oos.flush();
        return bos.toByteArray();
    }
}
