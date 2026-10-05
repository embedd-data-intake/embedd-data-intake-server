package com.github.embedd_data_intake.server.security;

import com.github.embedd_data_intake.server.annotation.ApplyAuth;
import com.github.embedd_data_intake.server.exceptions.impl.UnauthorizedException;
import com.github.embedd_data_intake.server.service.JwtService;
import com.github.embedd_data_intake.server.service.RefreshTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {
    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private HandlerExceptionResolver resolver;

    @Mock
    private RequestMappingHandlerMapping handlerMapping;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // Dummy controllers for testing @ApplyAuth method/class level detection
    @ApplyAuth
    static class AnnotatedController {
        public void dummyMethod() {}
    }

    static class UnannotatedController {
        @ApplyAuth
        public void annotatedMethod() {}

        public void plainMethod() {}
    }

    @Nested
    @DisplayName("shouldNotFilter()")
    class ShouldNotFilterTests {
        @Test
        void shouldNotFilter_WhenMethodHasApplyAuthAnnotation_ReturnsFalse() throws Exception {
            HandlerMethod handlerMethod = new HandlerMethod(
                    new UnannotatedController(),
                    UnannotatedController.class.getMethod("annotatedMethod")
            );
            HandlerExecutionChain chain = new HandlerExecutionChain(handlerMethod);

            given(request.getServletPath()).willReturn("/api/test");
            given(handlerMapping.getHandler(request)).willReturn(chain);

            boolean result = jwtAuthFilter.shouldNotFilter(request);

            assertThat(result).isFalse();
        }

        @Test
        void shouldNotFilter_WhenClassHasApplyAuthAnnotation_ReturnsFalse() throws Exception {
            HandlerMethod handlerMethod = new HandlerMethod(
                    new AnnotatedController(),
                    AnnotatedController.class.getMethod("dummyMethod")
            );
            HandlerExecutionChain chain = new HandlerExecutionChain(handlerMethod);

            given(request.getServletPath()).willReturn("/api/annotated");
            given(handlerMapping.getHandler(request)).willReturn(chain);

            boolean result = jwtAuthFilter.shouldNotFilter(request);

            assertThat(result).isFalse();
        }

        @Test
        void shouldNotFilter_WhenNoApplyAuthAnnotationPresent_ReturnsTrue() throws Exception {
            HandlerMethod handlerMethod = new HandlerMethod(
                    new UnannotatedController(),
                    UnannotatedController.class.getMethod("plainMethod")
            );
            HandlerExecutionChain chain = new HandlerExecutionChain(handlerMethod);

            given(request.getServletPath()).willReturn("/api/public");
            given(handlerMapping.getHandler(request)).willReturn(chain);

            boolean result = jwtAuthFilter.shouldNotFilter(request);

            assertThat(result).isTrue();
        }

        @Test
        void shouldNotFilter_WhenHandlerExecutionChainIsNull_ReturnsTrue() throws Exception {
            given(request.getServletPath()).willReturn("/api/unknown");
            given(handlerMapping.getHandler(request)).willReturn(null);

            boolean result = jwtAuthFilter.shouldNotFilter(request);

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("doFilterInternal()")
    class DoFilterInternalTests {

        @Test
        void doFilterInternal_WithValidBearerTokenAndExistingSession_AuthenticatesUserAndContinuesChain()
                throws ServletException, IOException {
            String token = "valid.jwt.token";
            UUID sid = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            given(request.getHeader("Authorization")).willReturn("Bearer " + token);
            given(jwtService.extractSessionId(token)).willReturn(sid);
            given(refreshTokenService.existsById(sid)).willReturn(true);
            given(jwtService.extractUserId(token)).willReturn(userId);

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getPrincipal()).isEqualTo(userId);

            verify(filterChain).doFilter(request, response);
            verify(resolver, never()).resolveException(any(), any(), any(), any());
        }

        @Test
        void doFilterInternal_WhenAuthorizationHeaderIsMissing_DelegatesUnauthorizedExceptionToResolver()
                throws ServletException, IOException {
            given(request.getHeader("Authorization")).willReturn(null);

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain, never()).doFilter(any(), any());
            verify(resolver).resolveException(
                    eq(request),
                    eq(response),
                    isNull(),
                    argThat(ex -> ex instanceof UnauthorizedException &&
                            "Authorization header is missing or malformed.".equals(ex.getMessage()))
            );
        }

        @Test
        void doFilterInternal_WhenAuthorizationHeaderDoesNotStartWithBearer_DelegatesToResolver()
                throws ServletException, IOException {
            given(request.getHeader("Authorization")).willReturn("Basic someBase64String");

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain, never()).doFilter(any(), any());
            verify(resolver).resolveException(
                    eq(request),
                    eq(response),
                    isNull(),
                    argThat(ex -> ex instanceof UnauthorizedException &&
                            "Authorization header is missing or malformed.".equals(ex.getMessage()))
            );
        }

        @Test
        void doFilterInternal_WhenSessionIdIsNull_DelegatesToResolver() throws ServletException, IOException {
            String token = "invalid.token";
            given(request.getHeader("Authorization")).willReturn("Bearer " + token);
            given(jwtService.extractSessionId(token)).willReturn(null);

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain, never()).doFilter(any(), any());
            verify(resolver).resolveException(
                    eq(request),
                    eq(response),
                    isNull(),
                    argThat(ex -> ex instanceof UnauthorizedException &&
                            "Invalid or expired access token.".equals(ex.getMessage()))
            );
        }

        @Test
        void doFilterInternal_WhenRefreshTokenDoesNotExistInDb_DelegatesToResolver()
                throws ServletException, IOException {
            String token = "revoked.session.token";
            UUID sid = UUID.randomUUID();

            given(request.getHeader("Authorization")).willReturn("Bearer " + token);
            given(jwtService.extractSessionId(token)).willReturn(sid);
            given(refreshTokenService.existsById(sid)).willReturn(false);

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain, never()).doFilter(any(), any());
            verify(resolver).resolveException(
                    eq(request),
                    eq(response),
                    isNull(),
                    argThat(ex -> ex instanceof UnauthorizedException &&
                            "Invalid or expired access token.".equals(ex.getMessage()))
            );
        }

        @Test
        void doFilterInternal_WhenJwtParsingThrowsUnexpectedException_WrapsAndDelegatesToResolver()
                throws ServletException, IOException {
            String token = "malformed.jwt.structure";

            given(request.getHeader("Authorization")).willReturn("Bearer " + token);
            given(jwtService.extractSessionId(token)).willThrow(new RuntimeException("Malformed JWT"));

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain, never()).doFilter(any(), any());
            verify(resolver).resolveException(
                    eq(request),
                    eq(response),
                    isNull(),
                    argThat(ex -> ex instanceof UnauthorizedException &&
                            "Invalid or expired access token.".equals(ex.getMessage()))
            );
        }
    }
}