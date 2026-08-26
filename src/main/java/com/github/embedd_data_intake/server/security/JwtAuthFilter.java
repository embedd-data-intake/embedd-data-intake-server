package com.github.embedd_data_intake.server.security;

import com.github.embedd_data_intake.server.exceptions.UnauthorizedException;
import com.github.embedd_data_intake.server.repository.RefreshTokenRepository;
import com.github.embedd_data_intake.server.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final HandlerExceptionResolver resolver;

    public JwtAuthFilter(
            JwtService jwtService,
            RefreshTokenRepository refreshTokenRepository,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.resolver = resolver;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/v1/auth/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new UnauthorizedException("Authorization header is missing or malformed.");
            }

            String token = authHeader.substring(7);

            UUID sid = jwtService.extractSessionId(token);

            if (Objects.isNull(sid) || !refreshTokenRepository.existsById(sid)) {
                throw new UnauthorizedException("Invalid or expired access token.");
            }

            UUID userId = jwtService.extractUserId(token);

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            UnauthorizedException unauthorizedEx = (e instanceof UnauthorizedException)
                    ? (UnauthorizedException) e
                    : new UnauthorizedException("Invalid or expired access token.");

            resolver.resolveException(request, response, null, unauthorizedEx);
        }
    }
}
