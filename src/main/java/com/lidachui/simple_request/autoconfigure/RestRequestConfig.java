package com.lidachui.simple_request.autoconfigure;

import com.lidachui.simple_request.core.RestClientProxyFactory;
import com.lidachui.simple_request.handler.RestTemplateHandler;
import com.lidachui.simple_request.validator.DefaultResponseValidator;
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
public class RestRequestConfig {

    @Bean
    public RestClientProxyFactory restClientProxyFactory(){
        return new RestClientProxyFactory();
    }

    @Bean
    public DefaultResponseValidator defaultResponseValidator(){
        return new DefaultResponseValidator();
    }

    @Bean(name = "restTemplateHandler")
    public RestTemplateHandler restTemplateHandler(){
        return new RestTemplateHandler();
    }
}
