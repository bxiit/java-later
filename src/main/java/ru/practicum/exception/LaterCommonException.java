package ru.practicum.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class LaterCommonException extends RuntimeException {
    private final HttpStatus httpStatus;

    protected LaterCommonException(HttpStatus httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }
}
