package com.lidachui.simpleRequest.core;

import com.lidachui.simpleRequest.annotation.EnableRestClients;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * RestClientRegistrar
 *
 * @author: lihuijie
 * @date: 2024/11/19 10:13
 * @version: 1.0
 */
public class RestClientRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestClientRegistrar.class);

    private ResourceLoader resourceLoader;
    private Environment environment;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        LOGGER.debug("Starting to register RestClient bean definitions");

        AnnotationAttributes attributes = getAnnotationAttributes(importingClassMetadata);
        if (attributes == null) return;

        ClassPathMapperScanner scanner = createAndConfigureScanner(registry, attributes);
        List<String> basePackages = getBasePackages(attributes);
        registerRestClients(scanner, basePackages);
    }

    private AnnotationAttributes getAnnotationAttributes(AnnotationMetadata metadata) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(
            metadata.getAnnotationAttributes(EnableRestClients.class.getName()));

        if (attributes == null) {
            LOGGER.warn("No @EnableRestClients annotation found");
        }
        return attributes;
    }

    private ClassPathMapperScanner createAndConfigureScanner(BeanDefinitionRegistry registry,
        AnnotationAttributes attributes) {
        ClassPathMapperScanner scanner = new ClassPathMapperScanner(registry);

        configureScanner(scanner, attributes);
        configureFactoryBean(scanner, attributes);

        return scanner;
    }

    private void configureScanner(ClassPathMapperScanner scanner, AnnotationAttributes attributes) {
        if (resourceLoader != null) {
            scanner.setResourceLoader(resourceLoader);
            LOGGER.debug("ResourceLoader set for scanner");
        }

        scanner.setEnvironment(environment);
        LOGGER.debug("Environment set for scanner");

        // 配置注解类
        Class<? extends Annotation> annotationClass = attributes.getClass("annotationClass");
        if (annotationClass != Annotation.class) {
            LOGGER.info("Using custom annotation class: {}", annotationClass.getName());
            scanner.setAnnotationClass(annotationClass);
        }

        // 配置标记接口
        Class<?> markerInterface = attributes.getClass("markerInterface");
        if (markerInterface != Class.class) {
            LOGGER.info("Using marker interface: {}", markerInterface.getName());
            scanner.setMarkerInterface(markerInterface);
        }
    }

    private void configureFactoryBean(ClassPathMapperScanner scanner, AnnotationAttributes attributes) {
        Class<? extends RestClientFactoryBean> factoryBean = attributes.getClass("factoryBean");
        if (!RestClientFactoryBean.class.equals(factoryBean)) {
            scanner.setRestClientFactoryBean(BeanUtils.instantiateClass(factoryBean));
            LOGGER.info("Using custom RestClientFactoryBean: {}", factoryBean.getName());
        }
    }

    private List<String> getBasePackages(AnnotationAttributes attributes) {
        List<String> basePackages = new ArrayList<>();

        addPackagesFromValue(basePackages, attributes);
        addPackagesFromBasePackages(basePackages, attributes);
        addPackagesFromBasePackageClasses(basePackages, attributes);

        LOGGER.info("Configured base packages for scanning: {}", basePackages);
        return basePackages;
    }

    private void addPackagesFromValue(List<String> basePackages, AnnotationAttributes attributes) {
        for (String pkg : attributes.getStringArray("value")) {
            addIfNotEmpty(basePackages, pkg);
        }
    }

    private void addPackagesFromBasePackages(List<String> basePackages, AnnotationAttributes attributes) {
        for (String pkg : attributes.getStringArray("basePackages")) {
            addIfNotEmpty(basePackages, pkg);
        }
    }

    private void addPackagesFromBasePackageClasses(List<String> basePackages, AnnotationAttributes attributes) {
        for (Class<?> clazz : attributes.getClassArray("basePackageClasses")) {
            basePackages.add(ClassUtils.getPackageName(clazz));
        }
    }

    private void addIfNotEmpty(List<String> basePackages, String pkg) {
        if (StringUtils.hasText(pkg)) {
            basePackages.add(pkg);
        }
    }

    private void registerRestClients(ClassPathMapperScanner scanner, List<String> basePackages) {
        scanner.registerFilters();
        Set<BeanDefinitionHolder> beanDefinitions = scanner.doScan(StringUtils.toStringArray(basePackages));
        LOGGER.info("Found {} RestClient interfaces", beanDefinitions.size());
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
