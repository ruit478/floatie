// backend/src/main/java/com/floatie/config/JwtAuthenticationFilter.java
package com.future.floatie.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Extracts the Bearer token from the Authorization header on every request,
 * validates it, and sets the Spring Security context.
 *
 * <h3>Path filtering</h3>
 * {@link #shouldNotFilter(HttpServletRequest)} skips {@code /auth/register}
 * and {@code /auth/login} (public endpoints) but <em>not</em>
 * {@code /auth/account} (DELETE requires authentication).
 * {@code /api/v1/pet/sprite/generate} and {@code /actuator/health} are
 * permitted at the {@link SecurityConfig} level, not here.
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(jwt);
            } catch (Exception e) {
                logger.error("JWT parsing error: " + e.getMessage());
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            if (jwtUtil.validateToken(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        chain.doFilter(request, response);
        log.debug("JWT authenticated user='{}' for path='{}'", username, request.getRequestURI());
    }

    /**
     * Skip JWT processing for public auth endpoints. {@code /auth/account}
     * (DELETE) is explicitly NOT skipped — it requires authentication.
     * Additional public paths ({@code /actuator/health},
     * {@code /api/v1/pet/sprite/generate}) are permitted at the
     * {@link SecurityConfig} level but still pass through this filter
     * (no token → no context set → request proceeds anonymously).
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean shouldSkip = path.startsWith("/auth/") && !path.equals("/auth/account");

        log.debug("Path='{}' skipJwtFilter={}", path, shouldSkip);
        return shouldSkip;
    }
}