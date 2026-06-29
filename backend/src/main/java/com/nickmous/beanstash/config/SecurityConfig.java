package com.nickmous.beanstash.config;

import tools.jackson.databind.JacksonModule;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity;
import org.springframework.security.web.webauthn.jackson.WebauthnJacksonModule;
import org.springframework.security.web.webauthn.management.JdbcPublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.JdbcUserCredentialRepository;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.security.web.webauthn.management.Webauthn4JRelyingPartyOperations;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.url}")
    private String websiteOrigin;

    @Value("${app.rp-id}")
    private String rpId;

    // Parent domain the XSRF-TOKEN cookie is scoped to, e.g. "beanstash.org", so the
    // front-end on beanstash.org can read a cookie set by api.beanstash.org. Leave empty
    // for local dev (host-only cookie; localhost ignores the port so it still works).
    @Value("${app.cookie-domain:}")
    private String cookieDomain;

    @Bean
    public SecurityFilterChain filterChain(org.springframework.security.config.annotation.web.builders.HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf
                // Double-submit cookie: token in a JS-readable XSRF-TOKEN cookie, echoed
                // back in the X-XSRF-TOKEN header. Plain (non-XOR) handler so the SPA can
                // send the cookie value verbatim; BREACH masking is irrelevant for a JSON
                // API that never reflects the token in a server-rendered response body.
                .csrfTokenRepository(csrfTokenRepository())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            // Renders the XSRF-TOKEN cookie on safe requests so the SPA can bootstrap a token.
            .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
            .authorizeHttpRequests((authorize)
                -> authorize
                .requestMatchers("/api/*/auth/**")
                .permitAll()
                .requestMatchers("/actuator/health")
                .permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                .permitAll()
                .requestMatchers("/api/*/")
                .authenticated()
                .requestMatchers("/api/*/users/**")
                .permitAll()
                // Public read: gated by the authority (granted to everyone by default), not by
                // authenticated(), so anonymous requests carrying user:read are allowed through.
                .requestMatchers("/api/*/user/**")
                .hasAuthority("user:read")
                .requestMatchers("/api/**")
                .authenticated()
                .anyRequest()
                .authenticated()
            )
            // Grant user:read to not-logged-in requests by default (authenticated requests get it
            // via IpAuthorityFilter). This is what makes /api/v1/user/** publicly readable.
            .anonymous(anonymous -> anonymous.authorities("ROLE_ANONYMOUS", "user:read"))
            // Adjusts per-request authorities (adds a default authority now; IP-based
            // withdraw/add can be enabled inside the filter later). Runs after
            // authentication and before authorization so both request matchers and
            // @PreAuthorize see the adjusted authorities.
            .addFilterBefore(new IpAuthorityFilter(), AuthorizationFilter.class)
            .webAuthn(webAuthn -> webAuthn
                .rpId(rpId)
                .allowedOrigins(websiteOrigin)
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // websiteOrigin (app.url) is the front-end origin — the same value WebAuthn uses
        // as its allowed origin. Must be explicit (not "*") because credentials are allowed.
        config.setAllowedOrigins(List.of(websiteOrigin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> {
            cookie.sameSite("Lax");
            if (StringUtils.hasText(cookieDomain)) {
                cookie.domain(cookieDomain);
            }
        });
        return repository;
    }

    @Bean
    JdbcPublicKeyCredentialUserEntityRepository jdbcPublicKeyCredentialRepository(JdbcOperations jdbc) {
        return new JdbcPublicKeyCredentialUserEntityRepository(jdbc);
    }

    @Bean
    JdbcUserCredentialRepository jdbcUserCredentialRepository(JdbcOperations jdbc) {
        return new JdbcUserCredentialRepository(jdbc);
    }

    @Bean
    JacksonModule webauthnJacksonModule() {
        return new WebauthnJacksonModule();
    }

    @Bean
    WebAuthnRelyingPartyOperations webAuthnRelyingPartyOperations(
            PublicKeyCredentialUserEntityRepository userEntityRepository,
            UserCredentialRepository credentialRepository) {
        PublicKeyCredentialRpEntity rpEntity = PublicKeyCredentialRpEntity.builder()
            .id(rpId)
            .name("Beanstash")
            .build();
        return new Webauthn4JRelyingPartyOperations(
            userEntityRepository, credentialRepository, rpEntity, Set.of(websiteOrigin));
    }
}
