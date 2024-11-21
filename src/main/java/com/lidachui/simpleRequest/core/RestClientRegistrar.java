package com.lidachui.simpleRequest.core;

import com.lidachui.simpleRequest.annotation.EnableRestClients;
import com.lidachui.simpleRequest.annotation.RestClient;
import com.lidachui.simpleRequest.autoconfigure.RestRequestConfig;
import com.lidachui.simpleRequest.util.ClassScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

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
public class RestClientRegistrar implements ApplicationContextAware, BeanFactoryPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(RestClientRegistrar.class);

    private final List<String> basePackages = new ArrayList<>();
    private RestClientProxyFactory restClientProxyFactory;
    private boolean supportsInstanceSupplier;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.restClientProxyFactory = applicationContext.getBean(RestClientProxyFactory.class);
        this.restClientProxyFactory.setApplicationContext(applicationContext);

        String[] configBeanNames =
                applicationContext.getBeanNamesForAnnotation(EnableRestClients.class);
        for (String beanName : configBeanNames) {
            Object configBean = applicationContext.getBean(beanName);

            EnableRestClients enableRestClients =
                    AnnotationUtils.findAnnotation(configBean.getClass(), EnableRestClients.class);
            if (enableRestClients != null) {
                basePackages.addAll(Arrays.asList(enableRestClients.basePackages()));
                logger.info("Found @EnableRestClients with basePackages: {}", basePackages);
            }
        }

        // 检查是否支持 setInstanceSupplier 方法
        supportsInstanceSupplier =
                Arrays.stream(AbstractBeanDefinition.class.getMethods())
                        .anyMatch(method -> method.getName().equals("setInstanceSupplier"));
        logger.info("Spring version supports setInstanceSupplier: {}", supportsInstanceSupplier);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
            throws BeansException {
        basePackages.parallelStream()
                .forEach(
                        basePackage -> {
                            List<Class<?>> annotatedInterfaces =
                                    scanClassesWithAnnotation(basePackage);
                            for (Class<?> clazz : annotatedInterfaces) {
                                if (clazz.isInterface()) {
                                    try {
                                        Object proxyInstance = restClientProxyFactory.create(clazz);
                                        registerBean(beanFactory, clazz, proxyInstance);
                                        logger.info(
                                                "Registered RestClient proxy for: {}",
                                                clazz.getName());
                                    } catch (Exception e) {
                                        logger.error(
                                                "Failed to register RestClient bean for: {}",
                                                clazz.getName(),
                                                e);
                                        throw new RuntimeException(
                                                "Failed to register RestClient beans", e);
                                    }
                                }
                            }
                        });
    }

    /**
     * 扫描带有注释类
     *
     * @param basePackage 基本包
     * @return 列表<class < ？>>
     */
    private List<Class<?>> scanClassesWithAnnotation(String basePackage) {
        try {
            return ClassScanner.getClassesWithAnnotation(basePackage, RestClient.class);
        } catch (Exception e) {
            logger.error("Failed to scan package: {}", basePackage, e);
            return Collections.emptyList();
        }
    }

    /**
     * 注册bean
     *
     * @param beanFactory 豆厂
     * @param clazz clazz
     * @param proxyInstance 代理实例
     */
    private void registerBean(
            ConfigurableListableBeanFactory beanFactory, Class<?> clazz, Object proxyInstance) {
        if (beanFactory instanceof DefaultListableBeanFactory) {
            DefaultListableBeanFactory registry = (DefaultListableBeanFactory) beanFactory;

            String beanName = determineBeanName(clazz);

            if (supportsInstanceSupplier) {
                // 新版本 Spring 的处理逻辑
                BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition();
                AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
                beanDefinition.setBeanClass(clazz);
                beanDefinition.setInstanceSupplier(() -> proxyInstance);
                registry.registerBeanDefinition(beanName, beanDefinition);
                logger.info("Registered bean with setInstanceSupplier: {}", beanName);
            } else {
                // 旧版本 Spring 的处理逻辑
                registry.registerSingleton(beanName, proxyInstance);
                logger.info("Registered singleton bean: {}", beanName);
            }
        }
    }

    /**
     * 确定bean名称
     *
     * @param clazz clazz
     * @return 一串
     */
    private String determineBeanName(Class<?> clazz) {
        RestClient restClientAnnotation = clazz.getAnnotation(RestClient.class);
        return restClientAnnotation != null && !restClientAnnotation.name().isEmpty()
                ? restClientAnnotation.name()
                : generateBeanName(clazz);
    }

    /**
     * 根据类生成类似 Spring 风格的 Bean 名称。
     *
     * @param clazz 要生成名称的类
     * @return 生成的 Bean 名称
     */
    public static String generateBeanName(Class<?> clazz) {
        // 获取类的简单名称
        String simpleName = clazz.getSimpleName();

        // 将第一个字母小写（符合 Spring 的默认规则）
        return StringUtils.uncapitalize(simpleName);
    }
}
