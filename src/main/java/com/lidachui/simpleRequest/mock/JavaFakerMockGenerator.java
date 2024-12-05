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

    private static final int MIN_COLLECTION_SIZE = 1;
    private static final int MAX_COLLECTION_SIZE = 10;

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
                return generateMockCollection(type.getActualTypeArguments()[0]);
            }

            if (Map.class.isAssignableFrom(rawClass)) {
                return generateMockMap(type.getActualTypeArguments()[0], type.getActualTypeArguments()[1]);
            }

            return generateMockForCustomGenericClass(rawClass, type.getActualTypeArguments());
        }

        return null;
    }

    private static Collection<Object> generateMockCollection(Type elementType) {
        Collection<Object> collection = new ArrayList<>();
        int collectionSize = faker.random().nextInt(MIN_COLLECTION_SIZE, MAX_COLLECTION_SIZE);
        for (int i = 0; i < collectionSize; i++) {
            collection.add(generateMockData(elementType));
        }
        return collection;
    }

    private static Map<Object, Object> generateMockMap(Type keyType, Type valueType) {
        Map<Object, Object> map = new HashMap<>();
        int mapSize = faker.random().nextInt(MIN_COLLECTION_SIZE, MAX_COLLECTION_SIZE);
        for (int i = 0; i < mapSize; i++) {
            map.put(generateMockData(keyType), generateMockData(valueType));
        }
        return map;
    }

    private static Object generateMockForCustomGenericClass(Class<?> clazz, Type[] genericArguments) {
        try {
            Object instance = clazz.getDeclaredConstructor().newInstance();

            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue; // 跳过静态字段
                }
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
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue; // 跳过静态字段
                }
                field.setAccessible(true);
                String fieldName = field.getName();
                Type fieldType = field.getGenericType();
                Object value = generateMockForField(fieldName, fieldType);
                field.set(instance, value);
            }
            return instance;
        } catch (ReflectiveOperationException e) {
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
            return faker.name().fullName();
        } else if (lowerCaseFieldName.contains("email")) {
            return faker.internet().safeEmailAddress();
        } else if (lowerCaseFieldName.contains("phone") || lowerCaseFieldName.contains("mobile")) {
            return faker.phoneNumber().phoneNumber();
        } else if (lowerCaseFieldName.contains("gender")) {
            return faker.options().option("男", "女", "其他");
        } else if (lowerCaseFieldName.contains("age")) {
            return String.valueOf(faker.number().numberBetween(18, 80));
        } else if (lowerCaseFieldName.contains("address")) {
            return faker.address().streetAddress() + ", " + faker.address().city() + ", " + faker.address().state();
        } else if (lowerCaseFieldName.contains("city")) {
            return faker.address().cityName();
        } else if (lowerCaseFieldName.contains("province")) {
            return faker.address().state();
        } else if (lowerCaseFieldName.contains("company")) {
            return faker.company().name();
        } else if (lowerCaseFieldName.contains("status")) {
            return faker.options().option("激活", "未激活", "冻结", "待审核");
        } else if (lowerCaseFieldName.contains("date") || lowerCaseFieldName.contains("time")) {
            return faker.date().past(3650, TimeUnit.DAYS).toString();
        } else if (lowerCaseFieldName.contains("id")) {
            return faker.idNumber().valid();
        } else if (lowerCaseFieldName.contains("bank")) {
            return faker.finance().creditCard();
        }

        return faker.lorem().sentence();
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
