package com.example.EcoGo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Resolver for SPA-style routing:
     * - Serve real static resources when they exist/readable
     * - Otherwise fallback to {@code /static/index.html}
     *
     * Extracted from an anonymous inner resolver to make it directly unit-testable
     * (and therefore easier to cover with JaCoCo).
     */
    static class SpaFallbackResourceResolver extends PathResourceResolver {
        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource requestedResource = location.createRelative(resourcePath);
            return requestedResource.exists() && requestedResource.isReadable()
                    ? requestedResource
                    : new ClassPathResource("/static/index.html");
        }

        // Convenience for unit tests without reflection.
        Resource resolve(String resourcePath, Resource location) throws IOException {
            return getResource(resourcePath, location);
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new SpaFallbackResourceResolver());
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Forward requests to / to index.html
        registry.addViewController("/").setViewName("forward:/index.html");
    }
}
