package com.lidachui.simpleRequest.core;

/**
 * ClientProxyFactory
 *
 * @author: lihuijie
 * @date: 2024/12/2 22:48
 * @version: 1.0
 */
public interface ClientProxyFactory {
    /**
     * 创建代理对象
     *
     * @param clientInterface 客户端接口类
     * @return 客户端代理对象
     */
    <T> T create(Class<T> clientInterface);

    /**
     * 判断是否支持指定客户端接口
     *
     * @param clientInterface 客户端接口类
     * @return 是否支持
     */
    boolean supports(Class<?> clientInterface);

    String getBaseUrl(String propertyKey,String baseUrl);
}
