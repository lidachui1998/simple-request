package com.lidachui.simpleRequest.core;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationContext;

/**
 * AbstractClientProxyFactory
 *
 * @author: lihuijie
 * @date: 2024/12/2 23:25
 * @version: 1.0
 */
public abstract class AbstractClientProxyFactory implements ClientProxyFactory {

    @Getter @Setter private ApplicationContext applicationContext;

    /**
     * 创建代理对象
     *
     * @param clientInterface 客户端接口类
     * @return 客户端代理对象
     */
    @Override
    public abstract <T> T create(Class<T> clientInterface);

    /**
     * 判断是否支持指定客户端接口
     *
     * @param clientInterface 客户端接口类
     * @return 是否支持
     */
    @Override
    public abstract boolean supports(Class<?> clientInterface);

    @Override
    public String getBaseUrl(String propertyKey, String baseUrl) {
        if (propertyKey != null && !propertyKey.isEmpty()) {
            String propertyValue =
                    getApplicationContext().getEnvironment().getProperty(propertyKey);
            return (propertyValue != null && !propertyValue.isEmpty()) ? propertyValue : baseUrl;
        }
        return baseUrl;
    }
}
