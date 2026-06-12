package com.nickmous.beanstash.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Adjusts the effective authorities of the current request before authorization
 * runs. This is per-request and ephemeral: it never touches the persisted roles
 * in the database, only what this one request is allowed to do.
 *
 * <p>For now it only adds a default authority to authenticated requests. The
 * IP-based withdraw/add logic is left commented out below so it can be enabled
 * later (see the SecurityConfig wiring and the forwarded-headers note).</p>
 */
public class IpAuthorityFilter extends OncePerRequestFilter {

    // Authority granted by default to authenticated requests. Enforced by the
    // /api/v1/user/** matcher and UserController's @PreAuthorize. Anonymous requests
    // get it via SecurityConfig's .anonymous() configuration instead.
    private static final String DEFAULT_AUTHORITY = "user:read";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();

        // Anonymous requests are handled separately (custom AnonymousAuthenticationFilter);
        // here we only adjust already-authenticated principals.
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {

            Set<GrantedAuthority> adjusted = new LinkedHashSet<>(authentication.getAuthorities());

            // Default behaviour: grant the authority to every authenticated request.
            boolean changed = adjusted.add(new SimpleGrantedAuthority(DEFAULT_AUTHORITY));

            // --- Enable later: withdraw (or add) the authority based on source IP ---
            // Requires `server.forward-headers-strategy=framework` and trusting
            // X-Forwarded-For only from your proxy, otherwise request.getRemoteAddr()
            // is the ingress IP and this is meaningless.
            //
            // private static final List<IpAddressMatcher> UNTRUSTED = List.of(
            //     new IpAddressMatcher("198.51.100.0/24"));
            //
            // boolean untrusted = UNTRUSTED.stream().anyMatch(m -> m.matches(request));
            // if (untrusted) {
            //     changed |= adjusted.removeIf(a -> a.getAuthority().equals(DEFAULT_AUTHORITY));
            // }

            if (changed) {
                UsernamePasswordAuthenticationToken updated = new UsernamePasswordAuthenticationToken(
                    authentication.getPrincipal(), authentication.getCredentials(), adjusted);
                updated.setDetails(authentication.getDetails());
                context.setAuthentication(updated);
            }
        }

        chain.doFilter(request, response);
    }
}
