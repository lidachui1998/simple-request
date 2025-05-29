package com.lidachui.simpleRequest.core;

import com.lidachui.simpleRequest.annotation.RestClient;

import java.util.Objects;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * ClassPathMapperScanner
 *
 * @author: lihuijie
 * @date: 2025/2/18 9:18
 * @version: 1.0
 */
public class ClassPathMapperScanner extends ClassPathBeanDefinitionScanner implements EnvironmentAware {

    private Class<? extends Annotation> annotationClass = RestClient.class;
    private Class<?> markerInterface;
    private RestClientFactoryBean<?> restClientFactoryBean = new RestClientFactoryBean<>();
    private Environment environment;

    public ClassPathMapperScanner(BeanDefinitionRegistry registry) {
        super(registry, false);
    }

    public void setAnnotationClass(Class<? extends Annotation> annotationClass) {
        this.annotationClass = annotationClass;
    }

    public void setMarkerInterface(Class<?> markerInterface) {
        this.markerInterface = markerInterface;
    }

    public void setRestClientFactoryBean(RestClientFactoryBean<?> restClientFactoryBean) {
        this.restClientFactoryBean =
                restClientFactoryBean != null
                        ? restClientFactoryBean
                        : new RestClientFactoryBean<>();
    }

    @Override
    public void setEnvironment(Environment environment) {
        super.setEnvironment(environment);
        this.environment = environment;
    }

    public void registerFilters() {
        if (this.annotationClass != null) {
            addIncludeFilter(new AnnotationTypeFilter(this.annotationClass));
        }

        if (this.markerInterface != null) {
            addIncludeFilter(
                    (metadataReader, metadataReaderFactory) -> {
                        String[] interfaceNames =
                                metadataReader.getClassMetadata().getInterfaceNames();
                        for (String interfaceName : interfaceNames) {
                            if (interfaceName.equals(this.markerInterface.getName())) {
                                return true;
                            }
                        }
                        return false;
                    });
        }
    }

    @Override
    protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
        Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);

        for (BeanDefinitionHolder holder : beanDefinitions) {
            GenericBeanDefinition definition = (GenericBeanDefinition) holder.getBeanDefinition();
            definition
                    .getConstructorArgumentValues()
                    .addGenericArgumentValue(Objects.requireNonNull(definition.getBeanClassName()));
            definition.setBeanClass(this.restClientFactoryBean.getClass());

            // 添加环境变量属性
            definition.getPropertyValues().add("environment", environment);
        }

        return beanDefinitions;
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        return beanDefinition.getMetadata().isInterface()
                && beanDefinition.getMetadata().isIndependent();
    }
}
