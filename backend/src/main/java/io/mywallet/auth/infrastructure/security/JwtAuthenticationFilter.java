package io.mywallet.auth.infrastructure.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Reads {@code Authorization: Bearer <token>}, validates it via {@link JwtTokenProvider},
 * and - if valid - populates the {@code SecurityContext} with an authentication whose
 * principal name is the user's UUID (this is what {@code JpaAuditingConfig.auditorAware()}
 * reads to fill {@code createdBy}/{@code updatedBy}) and whose authorities are the user's
 * roles, prefixed {@code ROLE_} as Spring Security expects for {@code hasRole(...)} checks.
 *
 * <p>Silently does nothing on a missing/invalid token - downstream, Spring Security's
 * standard access-denied handling takes over for endpoints that require authentication.</p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        extractToken(request)
            .flatMap(jwtTokenProvider::parse)
            .ifPresent(this::authenticate);
        chain.doFilter(request, response);
    }

    private Optional<String> extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return Optional.of(header.substring(BEARER_PREFIX.length()));
        }
        return Optional.empty();
    }

    private void authenticate(Claims claims) {
        String userId = claims.getSubject();
        List<GrantedAuthority> authorities = jwtTokenProvider.rolesFrom(claims).stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .map(GrantedAuthority.class::cast)
            .toList();

        var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
