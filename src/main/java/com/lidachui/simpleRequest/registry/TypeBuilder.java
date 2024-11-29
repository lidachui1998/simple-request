package com.lidachui.simpleRequest.registry;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
/**
 * TypeBuilder
 *
 * @author: lihuijie
 * @date: 2024/11/30 0:09
 * @version: 1.0
 */
public class TypeBuilder {

    /**
     * 构建一个简单的类型，例如 String.class、Integer.class
     *
     * @param rawType 原始类型
     * @return Type 类型
     */
    public static Type type(Class<?> rawType) {
        return rawType;
    }

    /**
     * 构建一个带泛型参数的类型
     * 例如：List<User>，Map<String, User>
     *
     * @param rawType    原始类型（例如 List.class）
     * @param typeParams 泛型参数（例如 User.class）
     * @return ParameterizedType 实例
     */
    public static ParameterizedType paramType(Class<?> rawType, Type... typeParams) {
        return new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return typeParams;
            }

            @Override
            public Type getRawType() {
                return rawType;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }

            @Override
            public String toString() {
                return rawType.getTypeName() + "<" + Arrays.toString(typeParams).replaceAll("\\[|\\]", "") + ">";
            }
        };
    }

    /**
     * 方便的链式构建复杂泛型类型
     * 例如：List<Map<String, User>> 等
     *
     * @param rawType    原始类型（例如 List.class）
     * @param typeParams 泛型参数（例如 Map.class, String.class, User.class）
     * @return ParameterizedType 实例
     */
    public static ParameterizedType nestedType(Class<?> rawType, Type... typeParams) {
        return paramType(rawType, typeParams);
    }
}
