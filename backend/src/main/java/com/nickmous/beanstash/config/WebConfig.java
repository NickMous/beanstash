package com.nickmous.beanstash.config;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@AllArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final UserContextInterceptor userContextInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userContextInterceptor);
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // Prefix every controller in this app with /api/{version}, e.g. GET /api/v1/users.
        // The {version} segment is a URI variable (required by path-segment versioning) and is
        // resolved below. Scoped to our base package so springdoc (/v3/api-docs) and the
        // actuator endpoints are left untouched.
        configurer.addPathPrefix(
            "/api/{version}",
            HandlerTypePredicate.forBasePackage("com.nickmous.beanstash.controller"));
    }

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        // Version lives in path segment index 1: /api[0]/{version}[1]/...
        // Handlers declare version = "v1" so springdoc renders concrete /api/v1/... paths
        // in the OpenAPI spec; the default SemanticApiVersionParser normalizes "v1" to
        // major version 1 for request matching (the leading "v" is ignored).
        configurer.usePathSegment(1).addSupportedVersions("v1");
    }
}
