package com.lidachui.simpleRequest.core;

import com.lidachui.simpleRequest.annotation.EnableRestClients;
import com.lidachui.simpleRequest.annotation.RestClient;
import com.lidachui.simpleRequest.autoconfigure.RestRequestConfig;
import com.lidachui.simpleRequest.util.ClassScanner;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultBeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.*;

/**
 * RestClientRegistrar
 *
 * @author: lihuijie
 * @date: 2024/11/19 10:13
 * @version: 1.0
 */
@Configuration
@Import({RestRequestConfig.class})
public class RestClientRegistrar implements BeanFactoryPostProcessor, ApplicationContextAware {

    private String defaultBasePackage = "";

    private RestClientProxyFactory restClientProxyFactory;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.restClientProxyFactory = applicationContext.getBean(RestClientProxyFactory.class);
        this.restClientProxyFactory.setApplicationContext(applicationContext);
        String[] configBeanNames =
                applicationContext.getBeanNamesForAnnotation(Configuration.class);
        for (String beanName : configBeanNames) {
            Object configBean = applicationContext.getBean(beanName);
            // 查找是否有 @EnableRestClients 注解
            EnableRestClients enableRestClients =
                    AnnotationUtils.findAnnotation(configBean.getClass(), EnableRestClients.class);
            if (enableRestClients != null) {
                // 获取注解中的 basePackages 属性
                String basePackage = enableRestClients.basePackage();
                defaultBasePackage = basePackage;
                break;
            }
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
            throws BeansException {
        String basePackage = defaultBasePackage;
        if (basePackage != null && !basePackage.isEmpty()) {
            try {
                // 扫描指定包路径下的所有类
                List<Class<?>> annotatedInterfaces =
                        ClassScanner.getClassesWithAnnotation(basePackage, RestClient.class);
                for (Class<?> clazz : annotatedInterfaces) {
                    if (clazz.isInterface()) {
                        // 为接口生成代理对象
                        Object proxyInstance = restClientProxyFactory.create(clazz);

                        // 将代理对象注册为 Spring 的 Bean
                        if (beanFactory instanceof DefaultListableBeanFactory) {
                            DefaultListableBeanFactory registry =
                                    (DefaultListableBeanFactory) beanFactory;

                            BeanDefinitionBuilder builder =
                                    BeanDefinitionBuilder.genericBeanDefinition();
                            AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
                            beanDefinition.setBeanClass(clazz);
                            beanDefinition.setInstanceSupplier(() -> proxyInstance);
                            DefaultBeanNameGenerator defaultBeanNameGenerator =
                                    new DefaultBeanNameGenerator();
                            String beanName =
                                    defaultBeanNameGenerator.generateBeanName(
                                            beanDefinition, registry);

                            // 使用类的全限定名作为 Bean 名称
                            registry.registerBeanDefinition(beanName, beanDefinition);
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to register RestClient beans", e);
            }
        }
    }
}
