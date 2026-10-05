package com.github.embedd_data_intake.server.exceptions.impl;

import com.github.embedd_data_intake.server.exceptions.RestException;
import org.springframework.http.HttpStatus;

public class NotFoundException extends RestException {
    public NotFoundException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
