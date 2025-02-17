package com.lidachui.simpleRequest.core;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;

public class RestClientFactoryBean<T> implements FactoryBean<T> {

    private Class<T> restClientInterface;

    @Resource
    private HttpClientProxyFactory httpClientProxyFactory;

    private Environment environment;

    public RestClientFactoryBean() {
    }

    public RestClientFactoryBean(Class<T> restClientInterface) {
        this.restClientInterface = restClientInterface;
    }

    @Override
    public T getObject() throws Exception {
        if (httpClientProxyFactory instanceof AbstractClientProxyFactory) {
            httpClientProxyFactory.setEnvironment(environment);
        }
        return httpClientProxyFactory.create(restClientInterface);
    }

    @Override
    public Class<?> getObjectType() {
        return this.restClientInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public void setRestClientInterface(Class<T> restClientInterface) {
        this.restClientInterface = restClientInterface;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}