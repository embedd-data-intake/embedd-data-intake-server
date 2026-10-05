package com.github.embedd_data_intake.server.exceptions.impl;

import com.github.embedd_data_intake.server.exceptions.RestException;
import org.springframework.http.HttpStatus;

public class BadRequestException extends RestException {
    public BadRequestException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
