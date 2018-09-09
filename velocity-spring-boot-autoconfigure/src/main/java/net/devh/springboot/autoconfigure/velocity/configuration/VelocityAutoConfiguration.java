package net.devh.springboot.autoconfigure.velocity.configuration;

import net.devh.springboot.autoconfigure.velocity.spring.support.VelocityEngineFactory;
import net.devh.springboot.autoconfigure.velocity.spring.support.VelocityEngineFactoryBean;
import net.devh.springboot.autoconfigure.velocity.spring.web.VelocityConfig;
import net.devh.springboot.autoconfigure.velocity.spring.web.VelocityConfigurer;
import net.devh.springboot.autoconfigure.velocity.spring.web.VelocityViewResolver;
import net.devh.springboot.autoconfigure.velocity.view.EmbeddedVelocityLayoutToolboxView;
import net.devh.springboot.autoconfigure.velocity.view.EmbeddedVelocityLayoutViewResolver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.template.TemplateLocation;
import org.springframework.boot.autoconfigure.web.ConditionalOnEnabledResourceChain;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.resource.ResourceUrlEncodingFilter;

import java.io.IOException;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.servlet.Servlet;

@Configuration
@ConditionalOnClass({VelocityEngine.class, VelocityEngineFactory.class})
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
@EnableConfigurationProperties({VelocityProperties.class})
public class VelocityAutoConfiguration {

    private static final Log logger = LogFactory.getLog(VelocityAutoConfiguration.class);

    private final ApplicationContext applicationContext;

    private final VelocityProperties properties;

    public VelocityAutoConfiguration(ApplicationContext applicationContext,
                                     VelocityProperties properties) {
        this.applicationContext = applicationContext;
        this.properties = properties;
    }

    @PostConstruct
    public void checkTemplateLocationExists() {
        if (this.properties.isCheckTemplateLocation()) {
            TemplateLocation location = new TemplateLocation(
                    this.properties.getResourceLoaderPath());
            if (!location.exists(this.applicationContext)) {
                logger.warn("Cannot find template location: " + location
                        + " (please add some templates, check your Velocity "
                        + "configuration, or set spring.velocity."
                        + "checkTemplateLocation=false)");
            }
        }
    }

    protected static class VelocityConfiguration {

        @Autowired
        protected VelocityProperties properties;

        protected void applyProperties(VelocityEngineFactory factory) {
            factory.setResourceLoaderPath(this.properties.getResourceLoaderPath());
            factory.setPreferFileSystemAccess(this.properties.isPreferFileSystemAccess());
            Properties velocityProperties = new Properties();
            velocityProperties.setProperty("input.encoding",
                    this.properties.getCharsetName());
            velocityProperties.putAll(this.properties.getProperties());
            factory.setVelocityProperties(velocityProperties);
        }

    }

    @Configuration
    @ConditionalOnNotWebApplication
    public static class VelocityNonWebConfiguration extends VelocityAutoConfiguration.VelocityConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public VelocityEngineFactoryBean velocityConfiguration() {
            VelocityEngineFactoryBean velocityEngineFactoryBean = new VelocityEngineFactoryBean();
            applyProperties(velocityEngineFactoryBean);
            return velocityEngineFactoryBean;
        }

    }

    @Configuration
    @ConditionalOnClass(Servlet.class)
    @ConditionalOnWebApplication
    public static class VelocityWebConfiguration extends VelocityAutoConfiguration.VelocityConfiguration {

        @Bean
        @ConditionalOnMissingBean(VelocityConfig.class)
        public VelocityConfigurer velocityConfigurer() {
            VelocityConfigurer configurer = new VelocityConfigurer();
            applyProperties(configurer);
            return configurer;
        }

        @Bean
        public VelocityEngine velocityEngine(VelocityConfigurer configurer)
                throws VelocityException, IOException {
            return configurer.getVelocityEngine();
        }

        @Bean
        @ConditionalOnMissingBean(name = "velocityViewResolver")
        @ConditionalOnProperty(name = "spring.velocity.enabled", matchIfMissing = true)
        public VelocityViewResolver velocityViewResolver() {
            EmbeddedVelocityLayoutViewResolver resolver = new EmbeddedVelocityLayoutViewResolver();
            resolver.setViewClass(EmbeddedVelocityLayoutToolboxView.class);
            resolver.setLayoutUrl("templates/layout/index.vm");
            resolver.setToolboxConfigLocation(properties.getToolboxConfigLocation());
            resolver.setDateToolAttribute(properties.getDateToolAttribute());
            resolver.setNumberToolAttribute(properties.getNumberToolAttribute());
            this.properties.applyToMvcViewResolver(resolver);
            return resolver;
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnEnabledResourceChain
        public ResourceUrlEncodingFilter resourceUrlEncodingFilter() {
            return new ResourceUrlEncodingFilter();
        }

    }
}