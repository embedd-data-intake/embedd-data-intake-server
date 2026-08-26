package com.github.embedd_data_intake.server.security;

import com.github.embedd_data_intake.server.annotation.ApplyAuth;
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
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final HandlerExceptionResolver resolver;
    private final RequestMappingHandlerMapping handlerMapping;

    // Apply path filters for Authorization filtering s -> s.startsWith("/v3/api-docs")
    private static final List<Predicate<String>> EXPLICIT_FILTERS = List.of();

    public JwtAuthFilter(
            JwtService jwtService,
            RefreshTokenRepository refreshTokenRepository,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver,
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.resolver = resolver;
        this.handlerMapping = handlerMapping;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        if (EXPLICIT_FILTERS.stream().anyMatch(filter -> filter.test(path))) {
            return false;
        }

        try {
            // Find the handler mapping for the current request
            HandlerExecutionChain handlerChain = handlerMapping.getHandler(request);

            if (handlerChain != null && handlerChain.getHandler() instanceof HandlerMethod handlerMethod) {
                // Check if annotation exists on the method OR on the class (controller)
                boolean hasMethodAnnotation = handlerMethod.hasMethodAnnotation(ApplyAuth.class);
                boolean hasClassAnnotation = handlerMethod.getBeanType().isAnnotationPresent(ApplyAuth.class);

                // Apply doFilterInternal
                if (hasMethodAnnotation || hasClassAnnotation) {
                    return false;
                }
            }
        } catch (ServletException _) {
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return true;
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

            // Check if refresh token id is valid and present in the db
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
