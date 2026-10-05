package com.github.embedd_data_intake.server.exceptions.impl;

import com.github.embedd_data_intake.server.exceptions.RestException;
import org.springframework.http.HttpStatus;

public class ConflictException extends RestException {
    public ConflictException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
