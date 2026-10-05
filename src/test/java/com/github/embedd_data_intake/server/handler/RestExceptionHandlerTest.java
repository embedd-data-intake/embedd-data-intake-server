package com.github.embedd_data_intake.server.handler;

import com.github.embedd_data_intake.server.dto.ExceptionDetailsDto;
import com.github.embedd_data_intake.server.exceptions.impl.NotFoundException;
import com.github.embedd_data_intake.server.exceptions.impl.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RestExceptionHandlerTest {
    @Mock
    private WebRequest webRequest;

    @InjectMocks
    private RestExceptionHandler restExceptionHandler;

    private static final String MOCK_PATH = "uri=/api/test";

    @BeforeEach
    void setUp() {
        given(webRequest.getDescription(false)).willReturn(MOCK_PATH);
    }

    @Nested
    @DisplayName("handleUnauthorized()")
    class HandleUnauthorizedTests {
        @Test
        void handleUnauthorized_Returns401AndExceptionMessage() {
            String errorMessage = "User is not authenticated";
            UnauthorizedException exception = new UnauthorizedException(errorMessage);

            ResponseEntity<ExceptionDetailsDto> response = restExceptionHandler.handleRestException(exception, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo(errorMessage);
            assertThat(response.getBody().getDetails()).isEqualTo(MOCK_PATH);
            assertThat(response.getBody().getDateTime()).isNotNull();
        }
    }

    @Nested
    @DisplayName("handleNotFound()")
    class HandleNotFoundTests {

        @Test
        void handleNotFound_Returns404AndExceptionMessage() {
            String errorMessage = "Device not found";
            NotFoundException exception = new NotFoundException(errorMessage);

            ResponseEntity<ExceptionDetailsDto> response = restExceptionHandler.handleRestException(exception, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo(errorMessage);
            assertThat(response.getBody().getDetails()).isEqualTo(MOCK_PATH);
            assertThat(response.getBody().getDateTime()).isNotNull();
        }
    }

    @Nested
    @DisplayName("handleDefault()")
    class HandleDefaultTests {
        @Test
        void handleDefault_Returns500AndGenericInternalServerErrorMessage() {
            Exception exception = new RuntimeException("Unexpected NullPointerException inside application");

            ResponseEntity<ExceptionDetailsDto> response = restExceptionHandler.handleDefault(exception, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("Internal server error");
            assertThat(response.getBody().getDetails()).isEqualTo(MOCK_PATH);
            assertThat(response.getBody().getDateTime()).isNotNull();
        }
    }

    @Nested
    @DisplayName("handleMethodArgumentNotValid()")
    class HandleMethodArgumentNotValidTests {
        // Dummy method used to create a realistic MethodParameter instance
        private static void dummyMethod(String param) {}

        @Test
        void handleMethodArgumentNotValid_Returns400AndExceptionMessage() throws Exception {
            Method javaMethod = HandleMethodArgumentNotValidTests.class.getDeclaredMethod("dummyMethod", String.class);
            MethodParameter parameter = new MethodParameter(javaMethod, 0);
            BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");

            MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

            ResponseEntity<Object> response = restExceptionHandler.handleMethodArgumentNotValid(
                    exception,
                    new HttpHeaders(),
                    HttpStatus.BAD_REQUEST,
                    webRequest
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isInstanceOf(ExceptionDetailsDto.class);

            ExceptionDetailsDto details = (ExceptionDetailsDto) response.getBody();
            assertThat(details).isNotNull();
            assertThat(details.getDetails()).isEqualTo(MOCK_PATH);
            assertThat(details.getDateTime()).isNotNull();
        }
    }

    @Nested
    @DisplayName("handleExceptionInternal()")
    class HandleExceptionInternalTests {
        @Test
        void handleExceptionInternal_ReturnsGivenStatusCodeAndInternalServerErrorDetails() {
            Exception exception = new Exception("Framework internal exception");
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-Custom-Header", "Value");
            HttpStatus status = HttpStatus.BAD_GATEWAY;

            ResponseEntity<Object> response = restExceptionHandler.handleExceptionInternal(
                    exception,
                    null,
                    headers,
                    status,
                    webRequest
            );

            assertThat(response.getStatusCode()).isEqualTo(status);
            assertThat(response.getHeaders().getFirst("X-Custom-Header")).isEqualTo("Value");
            assertThat(response.getBody()).isInstanceOf(ExceptionDetailsDto.class);

            ExceptionDetailsDto details = (ExceptionDetailsDto) response.getBody();
            assertThat(details).isNotNull();
            assertThat(details.getMessage()).isEqualTo("Internal server error");
            assertThat(details.getDetails()).isEqualTo(MOCK_PATH);
            assertThat(details.getDateTime()).isNotNull();
        }
    }
}