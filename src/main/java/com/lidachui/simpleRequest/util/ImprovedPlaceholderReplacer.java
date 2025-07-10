package com.lidachui.simpleRequest.util;

import org.springframework.util.PropertyPlaceholderHelper;

import java.util.*;

/**
 * 使用Spring PropertyPlaceholderHelper改进的占位符替换器
 *
 * @author: lihuijie
 * @date: 2025/7/10 22:07
 * @version: 1.0
 */
public class ImprovedPlaceholderReplacer {

    // Spring的PropertyPlaceholderHelper实例
    private final PropertyPlaceholderHelper placeholderHelper;

    /**
     * 构造函数，使用默认的占位符格式 ${}
     */
    public ImprovedPlaceholderReplacer() {
        // 使用Spring默认的占位符配置：前缀${，后缀}，分隔符:
        this.placeholderHelper = new PropertyPlaceholderHelper("${", "}", ":", true);
    }

    /**
     * 自定义占位符格式的构造函数
     *
     * @param prefix                         占位符前缀，如 "${"
     * @param suffix                         占位符后缀，如 "}"
     * @param valueSeparator                 默认值分隔符，如 ":"
     * @param ignoreUnresolvablePlaceholders 是否忽略无法解析的占位符
     */
    public ImprovedPlaceholderReplacer(String prefix, String suffix,
                                       String valueSeparator,
                                       boolean ignoreUnresolvablePlaceholders) {
        this.placeholderHelper = new PropertyPlaceholderHelper(
                prefix, suffix, valueSeparator, ignoreUnresolvablePlaceholders);
    }

    /**
     * 改进的占位符替换方法
     *
     * @param template     模板字符串
     * @param params       参数映射
     * @param consumedKeys 已消费的键集合
     * @return 替换后的字符串
     */
    public String replacePlaceholders(String template,
                                      Map<String, ParamInfo> params,
                                      Set<String> consumedKeys) {

        if (template == null || params.isEmpty()) {
            return template;
        }

        // 使用Spring的PropertyPlaceholderHelper进行替换
        return placeholderHelper.replacePlaceholders(template, placeholderName -> {
            // 检查参数映射中是否存在该键
            if (params.containsKey(placeholderName)) {
                ParamInfo paramInfo = params.get(placeholderName);
                Object value = paramInfo.getValue();

                // 记录已消费的键
                consumedKeys.add(placeholderName);

                // 处理不同类型的值
                return convertValueToString(value);
            }

            // 如果找不到对应的参数，返回null（Spring会根据配置决定如何处理）
            return null;
        });
    }

    /**
     * 将参数值转换为字符串
     *
     * @param value 参数值
     * @return 字符串表示
     */
    private String convertValueToString(Object value) {
        if (value == null) {
            return ""; // 处理null值
        }

        if (value instanceof Collection) {
            Collection<?> collection = (Collection<?>) value;
            if (!collection.isEmpty()) {
                // 取集合的第一个元素
                return collection.iterator().next().toString();
            } else {
                return ""; // 处理空集合
            }
        }

        return value.toString();
    }

    /**
     * 支持默认值的占位符替换 格式：${key:defaultValue}
     *
     * @param template     模板字符串
     * @param params       参数映射
     * @param consumedKeys 已消费的键集合
     * @return 替换后的字符串
     */
    public String replacePlaceholdersWithDefaults(String template,
                                                  Map<String, ParamInfo> params,
                                                  Set<String> consumedKeys) {

        if (template == null) {
            return null;
        }

        return placeholderHelper.replacePlaceholders(template, placeholderName -> {
            if (params.containsKey(placeholderName)) {
                ParamInfo paramInfo = params.get(placeholderName);
                Object value = paramInfo.getValue();

                consumedKeys.add(placeholderName);
                return convertValueToString(value);
            }

            // 返回null，让Spring使用默认值（如果有的话）
            return null;
        });
    }

    /**
     * 使用Properties对象进行占位符替换（更简单的用法）
     *
     * @param template   模板字符串
     * @param properties 属性对象
     * @return 替换后的字符串
     */
    public String replacePlaceholders(String template, Properties properties) {
        if (template == null) {
            return null;
        }

        return placeholderHelper.replacePlaceholders(template, properties);
    }

    /**
     * 使用Properties对象进行占位符替换，支持自定义默认值处理
     * @param template 模板字符串
     * @param properties 属性对象
     * @param defaultValue 当属性不存在时的默认值
     * @return 替换后的字符串
     */
    public String replacePlaceholdersWithCustomDefault(String template, Properties properties, String defaultValue) {
        if (template == null) {
            return template;
        }

        return placeholderHelper.replacePlaceholders(template, placeholderName -> {
            // 首先尝试从Properties中获取值
            String value = properties.getProperty(placeholderName);
            if (value != null) {
                return value;
            }

            // 如果Properties中没有，返回自定义默认值
            return defaultValue;
        });
    }


    /**
     * 检查模板中是否包含占位符
     *
     * @param template 模板字符串
     * @return 是否包含占位符
     */
    public boolean containsPlaceholders(String template) {
        if (template == null) {
            return false;
        }

        // 尝试替换，如果有变化说明包含占位符
        String result = placeholderHelper.replacePlaceholders(template, placeholderName -> "__PLACEHOLDER_FOUND__");

        return !template.equals(result);
    }
}