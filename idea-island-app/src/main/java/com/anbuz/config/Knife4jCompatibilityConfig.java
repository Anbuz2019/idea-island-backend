package com.anbuz.config;

import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import com.github.xiaoymin.knife4j.core.conf.ExtensionsConstants;
import com.github.xiaoymin.knife4j.core.conf.GlobalConstants;
import com.github.xiaoymin.knife4j.spring.configuration.Knife4jHttpBasic;
import com.github.xiaoymin.knife4j.spring.configuration.Knife4jProperties;
import com.github.xiaoymin.knife4j.spring.configuration.Knife4jSetting;
import com.github.xiaoymin.knife4j.spring.extension.OpenApiExtensionResolver;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 覆盖 Knife4j 4.5.0 内置 customizer，兼容 Spring Boot 3.4 / Springdoc 2.8。
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({Knife4jProperties.class, Knife4jSetting.class, Knife4jHttpBasic.class})
public class Knife4jCompatibilityConfig {

    @Bean
    @ConditionalOnProperty(name = "knife4j.enable", havingValue = "true")
    public com.github.xiaoymin.knife4j.spring.extension.Knife4jOpenApiCustomizer knife4jOpenApiCustomizer(
            Knife4jProperties knife4jProperties,
            SpringDocConfigProperties properties) {
        return new Boot34CompatibleKnife4jOpenApiCustomizer(knife4jProperties, properties);
    }

    @Slf4j
    static class Boot34CompatibleKnife4jOpenApiCustomizer
            extends com.github.xiaoymin.knife4j.spring.extension.Knife4jOpenApiCustomizer {

        private final Knife4jProperties knife4jProperties;
        private final SpringDocConfigProperties properties;

        Boot34CompatibleKnife4jOpenApiCustomizer(
                Knife4jProperties knife4jProperties,
                SpringDocConfigProperties properties) {
            super(knife4jProperties, properties);
            this.knife4jProperties = knife4jProperties;
            this.properties = properties;
        }

        @Override
        public void customise(OpenAPI openApi) {
            log.debug("Register Spring Boot 3.4 compatible Knife4j customizer");
            if (!knife4jProperties.isEnable()) {
                return;
            }
            Knife4jSetting setting = knife4jProperties.getSetting();
            OpenApiExtensionResolver openApiExtensionResolver =
                    new OpenApiExtensionResolver(setting, knife4jProperties.getDocuments());
            openApiExtensionResolver.start();
            Map<String, Object> objectMap = new HashMap<>();
            objectMap.put(GlobalConstants.EXTENSION_OPEN_SETTING_NAME, setting);
            objectMap.put(GlobalConstants.EXTENSION_OPEN_MARKDOWN_NAME,
                    openApiExtensionResolver.getMarkdownFiles());
            openApi.addExtension(GlobalConstants.EXTENSION_OPEN_API_NAME, objectMap);
            addOrderExtension(openApi);
        }

        private void addOrderExtension(OpenAPI openApi) {
            Set<SpringDocConfigProperties.GroupConfig> groupConfigs = properties.getGroupConfigs();
            if (CollectionUtils.isEmpty(groupConfigs)) {
                return;
            }
            Set<String> packagesToScan = groupConfigs.stream()
                    .map(SpringDocConfigProperties.GroupConfig::getPackagesToScan)
                    .filter(toScan -> !CollectionUtils.isEmpty(toScan))
                    .flatMap(List::stream)
                    .collect(Collectors.toSet());
            if (CollectionUtils.isEmpty(packagesToScan)) {
                return;
            }
            Set<Class<?>> classes = packagesToScan.stream()
                    .map(packageToScan -> scanPackageByAnnotation(packageToScan, RestController.class))
                    .flatMap(Set::stream)
                    .filter(clazz -> clazz.isAnnotationPresent(ApiSupport.class))
                    .collect(Collectors.toSet());
            if (CollectionUtils.isEmpty(classes) || CollectionUtils.isEmpty(openApi.getTags())) {
                return;
            }
            Map<String, Integer> tagOrderMap = new HashMap<>();
            classes.forEach(clazz -> {
                Tag tag = getTag(clazz);
                if (Objects.nonNull(tag)) {
                    ApiSupport apiSupport = clazz.getAnnotation(ApiSupport.class);
                    tagOrderMap.putIfAbsent(tag.name(), apiSupport.order());
                }
            });
            openApi.getTags().forEach(tag -> {
                if (tagOrderMap.containsKey(tag.getName())) {
                    tag.addExtension(ExtensionsConstants.EXTENSION_ORDER, tagOrderMap.get(tag.getName()));
                }
            });
        }

        private Tag getTag(Class<?> clazz) {
            Tag tag = clazz.getAnnotation(Tag.class);
            if (Objects.nonNull(tag)) {
                return tag;
            }
            Class<?>[] interfaces = clazz.getInterfaces();
            if (ArrayUtils.isEmpty(interfaces)) {
                return null;
            }
            for (Class<?> interfaceClazz : interfaces) {
                Tag interfaceTag = interfaceClazz.getAnnotation(Tag.class);
                if (Objects.nonNull(interfaceTag)) {
                    return interfaceTag;
                }
            }
            return null;
        }

        private Set<Class<?>> scanPackageByAnnotation(
                String packageName,
                Class<? extends Annotation> annotationClass) {
            ClassPathScanningCandidateComponentProvider scanner =
                    new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(annotationClass));
            Set<Class<?>> classes = new HashSet<>();
            for (BeanDefinition beanDefinition : scanner.findCandidateComponents(packageName)) {
                try {
                    classes.add(Class.forName(beanDefinition.getBeanClassName()));
                } catch (ClassNotFoundException e) {
                    log.debug("Skip class when scanning package={} className={}",
                            packageName, beanDefinition.getBeanClassName(), e);
                }
            }
            return classes;
        }
    }
}
