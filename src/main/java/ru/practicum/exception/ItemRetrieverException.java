package ru.practicum.exception;

import java.net.URISyntaxException;

public class ItemRetrieverException extends RuntimeException {

    public ItemRetrieverException(String message, Throwable e) {
        super(message, e);
    }

    public ItemRetrieverException(String message) {
        super(message);
    }
}
