package com.github.embedd_data_intake.server.handler;

import com.github.embedd_data_intake.server.dto.ExceptionDetailsDto;
import com.github.embedd_data_intake.server.exceptions.RestException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.OffsetDateTime;

@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {
    // TODO: Check and fix exception handling
    @Override
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            @Nullable Object body,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode statusCode,
            WebRequest request) {
        ExceptionDetailsDto details = new ExceptionDetailsDto(
                OffsetDateTime.now(),
                "Internal server error",
                request.getDescription(false)
        );

        return new ResponseEntity<>(details, headers, statusCode);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionDetailsDto> handleDefault(Exception exception, WebRequest request) {
        ExceptionDetailsDto details = new ExceptionDetailsDto(
                OffsetDateTime.now(),
                "Internal server error",
                request.getDescription(false)
        );

        return new ResponseEntity<>(details, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(RestException.class)
    public ResponseEntity<ExceptionDetailsDto> handleRestException(RestException exception, WebRequest request) {
        ExceptionDetailsDto details = new ExceptionDetailsDto(
                OffsetDateTime.now(),
                exception.getMessage(),
                request.getDescription(false)
        );

        return new ResponseEntity<>(details, exception.getStatus());
    }

    @Override
    public ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode code,
            WebRequest request
    ) {
        ExceptionDetailsDto details = new ExceptionDetailsDto(
                OffsetDateTime.now(),
                exception.getMessage(),
                request.getDescription(false)
        );

        return new ResponseEntity<>(details, HttpStatus.BAD_REQUEST);
    }
}
