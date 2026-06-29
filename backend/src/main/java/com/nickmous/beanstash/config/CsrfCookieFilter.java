package com.nickmous.beanstash.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Forces the deferred CSRF token to be resolved on every request so the
 * XSRF-TOKEN cookie is written and reaches the single-page app.
 *
 * <p>Spring Security loads CSRF tokens lazily for performance: the cookie is
 * only rendered when something actually reads the token value. A separate
 * front-end has no server-rendered page to trigger that, so without this filter
 * the SPA could never bootstrap a token with a safe GET. Calling
 * {@link CsrfToken#getToken()} here causes {@code CookieCsrfTokenRepository} to
 * emit the cookie on responses to GET requests.</p>
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute("_csrf");
        if (csrfToken != null) {
            // Accessing the value is what triggers the repository to write the cookie.
            csrfToken.getToken();
        }
        chain.doFilter(request, response);
    }
}
