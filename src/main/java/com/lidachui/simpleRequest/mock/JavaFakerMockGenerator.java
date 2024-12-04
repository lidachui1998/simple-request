package com.lidachui.simpleRequest.mock;

import com.github.javafaker.Faker;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
/**
 * AbstractMockGenerator
 *
 * @author: lihuijie
 * @date: 2024/12/4 23:08
 * @version: 1.0
 */
public class JavaFakerMockGenerator implements MockGenerator {

    private static final Faker faker = new Faker(new Locale("zh-CN"));

    @Override
    public <T> T generate(Type type) {
        return generateMockData(type);
    }

    @SuppressWarnings("unchecked")
    public static <T> T generateMockData(Type type) {
        if (type instanceof Class<?>) {
            return (T) generateMockForClass((Class<?>) type);
        } else if (type instanceof ParameterizedType) {
            return (T) generateMockForParameterizedType((ParameterizedType) type);
        } else {
            throw new UnsupportedOperationException("Unsupported Type: " + type);
        }
    }

    private static Object generateMockForClass(Class<?> clazz) {
        if (clazz == String.class) {
            return fakerBasedOnFieldName("default");
        } else if (clazz == Integer.class || clazz == int.class) {
            return faker.number().numberBetween(1, 100);
        } else if (clazz == Long.class || clazz == long.class) {
            return faker.number().numberBetween(1L, 100L);
        } else if (clazz == Double.class || clazz == double.class) {
            return faker.number().randomDouble(2, 1, 100);
        } else if (clazz == Boolean.class || clazz == boolean.class) {
            return faker.bool().bool();
        } else if (clazz.isEnum()) {
            Object[] constants = clazz.getEnumConstants();
            return constants[faker.random().nextInt(constants.length)];
        } else if (clazz == Date.class) {
            return faker.date().past(3650, TimeUnit.DAYS); // 最近 10 年的随机日期
        } else if (clazz == LocalDate.class) {
            return LocalDate.now().minusDays(faker.number().numberBetween(1, 3650));
        } else if (clazz == LocalDateTime.class) {
            return LocalDateTime.now().minusHours(faker.number().numberBetween(1, 87600)); // 最近 10 年的随机时间
        } else if (Collection.class.isAssignableFrom(clazz)) {
            return new ArrayList<>();
        } else if (Map.class.isAssignableFrom(clazz)) {
            return new HashMap<>();
        } else {
            return generateMockForCustomClass(clazz);
        }
    }

    private static Object generateMockForParameterizedType(ParameterizedType type) {
        Type rawType = type.getRawType();

        if (rawType instanceof Class<?>) {
            Class<?> rawClass = (Class<?>) rawType;

            if (Collection.class.isAssignableFrom(rawClass)) {
                Type elementType = type.getActualTypeArguments()[0];
                Collection<Object> collection = new ArrayList<>();
                for (int i = 0; i < faker.random().nextInt(1, 5); i++) {
                    collection.add(generateMockData(elementType));
                }
                return collection;
            }

            if (Map.class.isAssignableFrom(rawClass)) {
                Type keyType = type.getActualTypeArguments()[0];
                Type valueType = type.getActualTypeArguments()[1];
                Map<Object, Object> map = new HashMap<>();
                for (int i = 0; i < faker.random().nextInt(1, 5); i++) {
                    map.put(generateMockData(keyType), generateMockData(valueType));
                }
                return map;
            }

            return generateMockForCustomGenericClass(rawClass, type.getActualTypeArguments());
        }

        return null;
    }

    private static Object generateMockForCustomGenericClass(Class<?> clazz, Type[] genericArguments) {
        try {
            Object instance = clazz.getDeclaredConstructor().newInstance();

            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Type fieldType = field.getGenericType();

                if (fieldType instanceof TypeVariable<?>) {
                    String typeName = ((TypeVariable<?>) fieldType).getName();
                    int index = getGenericParameterIndex(clazz, typeName);
                    if (index >= 0 && index < genericArguments.length) {
                        fieldType = genericArguments[index];
                    }
                }

                Object fieldValue = generateMockData(fieldType);
                field.set(instance, fieldValue);
            }

            return instance;

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate mock data for custom generic class: " + clazz.getName(), e);
        }
    }

    private static Object generateMockForCustomClass(Class<?> clazz) {
        try {
            Object instance = clazz.getDeclaredConstructor().newInstance();
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                String fieldName = field.getName();
                Type fieldType = field.getGenericType();
                Object value = generateMockForField(fieldName, fieldType);
                field.set(instance, value);
            }
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate mock data for class: " + clazz.getName(), e);
        }
    }

    private static Object generateMockForField(String fieldName, Type fieldType) {
        if (fieldType == String.class) {
            return fakerBasedOnFieldName(fieldName);
        } else {
            return generateMockData(fieldType);
        }
    }

    private static String fakerBasedOnFieldName(String fieldName) {
        String lowerCaseFieldName = fieldName.toLowerCase();

        if (lowerCaseFieldName.contains("name")) {
            // 姓名逻辑，支持男女随机生成
            return faker.name().fullName();
        } else if (lowerCaseFieldName.contains("email")) {
            // 随机生成邮箱
            return faker.internet().emailAddress();
        } else if (lowerCaseFieldName.contains("phone") || lowerCaseFieldName.contains("mobile")) {
            // 中国格式手机号
            return faker.phoneNumber().cellPhone().replaceAll("[^\\d]", "").substring(0, 11);
        } else if (lowerCaseFieldName.contains("gender")) {
            // 性别字段生成"男"/"女"
            return faker.bool().bool() ? "男" : "女";
        } else if (lowerCaseFieldName.contains("age")) {
            // 合理年龄区间
            return String.valueOf(faker.number().numberBetween(18, 60));
        } else if (lowerCaseFieldName.contains("address")) {
            // 地址生成更贴近实际
            return faker.address().fullAddress();
        } else if (lowerCaseFieldName.contains("city")) {
            // 城市字段
            return faker.address().city();
        } else if (lowerCaseFieldName.contains("province")) {
            // 省份字段
            return faker.address().state();
        } else if (lowerCaseFieldName.contains("company")) {
            // 公司名称
            return faker.company().name();
        } else if (lowerCaseFieldName.contains("status")) {
            // 状态字段
            return faker.options().option("激活", "未激活", "冻结");
        } else if (lowerCaseFieldName.contains("date") || lowerCaseFieldName.contains("time")) {
            // 日期或时间字段
            return faker.date().birthday().toString();
        } else if (lowerCaseFieldName.contains("id")) {
            // 身份证号模拟
            return faker.idNumber().valid();
        } else if (lowerCaseFieldName.contains("bank")) {
            // 银行卡号生成
            return faker.finance().creditCard();
        }

        // 默认使用 lorem 生成
        return faker.lorem().word();
    }

    private static int getGenericParameterIndex(Class<?> clazz, String typeName) {
        TypeVariable<?>[] typeParameters = clazz.getTypeParameters();
        for (int i = 0; i < typeParameters.length; i++) {
            if (typeParameters[i].getName().equals(typeName)) {
                return i;
            }
        }
        return -1;
    }
}
