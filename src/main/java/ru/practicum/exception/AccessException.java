package ru.practicum.exception;

import org.springframework.http.HttpStatus;

public class AccessException extends LaterCommonException {
    public AccessException(HttpStatus httpStatus, String message) {
        super(httpStatus, message);
    }
}
