package com.lidachui.simpleRequest.autoconfigure;

import com.lidachui.simpleRequest.cache.LocalCacheStrategy;
import com.lidachui.simpleRequest.cache.RedisCacheStrategy;
import com.lidachui.simpleRequest.core.HttpClientProxyFactory;
import com.lidachui.simpleRequest.filter.DefaultRequestFilter;
import com.lidachui.simpleRequest.handler.OkHttpHandler;
import com.lidachui.simpleRequest.handler.RestTemplateHandler;
import com.lidachui.simpleRequest.util.SpringUtil;
import com.lidachui.simpleRequest.validator.DefaultResponseValidator;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RestRequestConfig
 *
 * @author: lihuijie
 * @date: 2024/11/19 22:32
 * @version: 1.0
 */
@Configuration
public class RestClientAutoConfiguration implements ApplicationContextAware {
    private ApplicationContext applicationContext;

    @Bean
    public HttpClientProxyFactory httpClientProxyFactory() {
        HttpClientProxyFactory factory = new HttpClientProxyFactory();
        factory.setApplicationContext(applicationContext);
        return factory;
    }

    @Bean
    public DefaultResponseValidator defaultResponseValidator() {
        return new DefaultResponseValidator();
    }

    @Bean(name = "restTemplateHandler")
    public RestTemplateHandler restTemplateHandler() {
        return new RestTemplateHandler();
    }

    @Bean(name = "okhttpClientHandler")
    public OkHttpHandler okHttpHandler() {
        return new OkHttpHandler();
    }

    @Bean
    public DefaultRequestFilter defaultRequestFilter() {
        return new DefaultRequestFilter();
    }

    @Bean(name = "simpleRequestSpringUtil")
    public SpringUtil springUtil() {
        return new SpringUtil();
    }

    @Bean
    public LocalCacheStrategy localCacheStrategy() {
        return new LocalCacheStrategy();
    }

    @Bean
    public RedisCacheStrategy redisCacheStrategy() {
        return new RedisCacheStrategy();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
