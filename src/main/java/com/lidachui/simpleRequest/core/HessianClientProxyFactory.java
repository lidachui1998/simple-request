package com.lidachui.simpleRequest.core;

import com.caucho.hessian.client.HessianProxyFactory;
import com.lidachui.simpleRequest.annotation.HessianClient;

import lombok.extern.slf4j.Slf4j;

import org.springframework.remoting.caucho.HessianProxyFactoryBean;

/**
 * HessionClientProxyFactory
 *
 * @author: lihuijie
 * @date: 2024/12/2 23:00
 * @version: 1.0
 */
@Slf4j
public class HessianClientProxyFactory extends AbstractClientProxyFactory {

    /**
     * 创建代理对象
     *
     * @param clientInterface 客户端接口类
     * @return 客户端代理对象
     */
    @Override
    public <T> T create(Class<T> clientInterface) {
        HessianClient hessianClient = clientInterface.getAnnotation(HessianClient.class);
        if (hessianClient == null) {
            throw new IllegalArgumentException(
                    clientInterface.getName() + " is not annotated with @HessianClient");
        }
        String baseUrl = getBaseUrl(hessianClient.propertyKey(), hessianClient.baseUrl());
        // 创建 Hessian 动态代理
        HessianProxyFactoryBean factory = new HessianProxyFactoryBean();
        HessianProxyFactory proxyFactory = new HessianProxyFactory();
        proxyFactory.setConnectTimeout(10 * 1000L);
        factory.setProxyFactory(proxyFactory);
        factory.setServiceUrl(baseUrl);
        factory.setServiceInterface(clientInterface);
        factory.afterPropertiesSet();
        return (T) factory.getObject();
    }

    /**
     * 判断是否支持指定客户端接口
     *
     * @param clientInterface 客户端接口类
     * @return 是否支持
     */
    @Override
    public boolean supports(Class<?> clientInterface) {
        return clientInterface.isAnnotationPresent(HessianClient.class);
    }
}
