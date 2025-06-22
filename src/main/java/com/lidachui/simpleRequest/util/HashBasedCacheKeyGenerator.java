package com.lidachui.simpleRequest.util;

/**
 * HashBasedCacheKeyGenerator
 *
 * @author: lihuijie
 * @date: 2025/6/22 14:55
 * @version: 1.0
 */

import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

public class HashBasedCacheKeyGenerator {

    private static final String FRAMEWORK_IDENTIFIER = "MyFramework";

    /**
     * 方案1：简单Hash - 最轻量级的实现
     */
    public static String generateSimpleHashKey(Method method, Object[] args) {
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        int argsHash = deepHashCode(args);

        return String.format("%s:%s:%s:%d",
                FRAMEWORK_IDENTIFIER, className, methodName, argsHash);
    }

    /**
     * 方案2：MD5 Hash - 更安全，避免Hash冲突
     */
    public static String generateMD5HashKey(Method method, Object[] args) {
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        String argsHash = md5Hash(deepHashCode(args));

        return String.format("%s:%s:%s:%s",
                FRAMEWORK_IDENTIFIER, className, methodName, argsHash);
    }

    /**
     * 方案3：纯Hash - 最简洁的实现（推荐）
     */
    public static String generatePureHashKey(Method method, Object[] args) {
        // 将方法信息和参数一起计算hash
        int methodHash = Objects.hash(
                method.getDeclaringClass().getName(),
                method.getName()
        );
        int argsHash = deepHashCode(args);
        int combinedHash = Objects.hash(methodHash, argsHash);

        return FRAMEWORK_IDENTIFIER + ":" + Math.abs(combinedHash);
    }

    /**
     * 方案4：SHA256 Hash - 最安全，几乎零冲突概率
     */
    public static String generateSHA256HashKey(Method method, Object[] args) {
        String methodInfo = method.getDeclaringClass().getName() + ":" + method.getName();
        String argsInfo = String.valueOf(deepHashCode(args));
        String combined = methodInfo + ":" + argsInfo;

        return FRAMEWORK_IDENTIFIER + ":" + sha256Hash(combined);
    }

    /**
     * 深度计算参数数组的HashCode，正确处理各种类型
     */
    private static int deepHashCode(Object[] args) {
        if (args == null) {
            return 0;
        }

        int result = 1;
        for (Object arg : args) {
            result = 31 * result + objectHashCode(arg);
        }
        return result;
    }

    /**
     * 计算单个对象的HashCode，特殊处理各种类型
     */
    private static int objectHashCode(Object obj) {
        if (obj == null) {
            return 0;
        }

        Class<?> clazz = obj.getClass();

        // 处理各种数组类型
        if (clazz.isArray()) {
            if (clazz == byte[].class) {
                return Arrays.hashCode((byte[]) obj);
            } else if (clazz == int[].class) {
                return Arrays.hashCode((int[]) obj);
            } else if (clazz == long[].class) {
                return Arrays.hashCode((long[]) obj);
            } else if (clazz == double[].class) {
                return Arrays.hashCode((double[]) obj);
            } else if (clazz == float[].class) {
                return Arrays.hashCode((float[]) obj);
            } else if (clazz == boolean[].class) {
                return Arrays.hashCode((boolean[]) obj);
            } else if (clazz == char[].class) {
                return Arrays.hashCode((char[]) obj);
            } else if (clazz == short[].class) {
                return Arrays.hashCode((short[]) obj);
            } else {
                // 对象数组
                return Arrays.deepHashCode((Object[]) obj);
            }
        }

        // 处理MultipartFile - 基于文件内容计算hash
        if (obj.getClass().getSimpleName().equals("StandardMultipartFile")) {
            return getMultipartFileHashCode(obj);
        }

        // 普通对象
        return obj.hashCode();
    }

    /**
     * 计算MultipartFile的HashCode
     */
    private static int getMultipartFileHashCode(Object multipartFile) {
        try {
            Method getOriginalFilename = multipartFile.getClass().getMethod("getOriginalFilename");
            Method getSize = multipartFile.getClass().getMethod("getSize");
            Method getBytes = multipartFile.getClass().getMethod("getBytes");

            String filename = (String) getOriginalFilename.invoke(multipartFile);
            Long size = (Long) getSize.invoke(multipartFile);
            byte[] bytes = (byte[]) getBytes.invoke(multipartFile);

            // 基于文件名、大小和内容计算hash
            return Objects.hash(filename, size, Arrays.hashCode(bytes));
        } catch (Exception e) {
            // 如果获取失败，使用对象本身的hashCode
            return multipartFile.hashCode();
        }
    }

    /**
     * MD5哈希
     */
    private static String md5Hash(Object input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(String.valueOf(input).getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }

    /**
     * SHA256哈希
     */
    private static String sha256Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().substring(0, 16); // 取前16位，足够唯一
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }
}