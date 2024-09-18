package ru.practicum.common.advice;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.practicum.common.LaterCommonException;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private static final Object[] EMPTY_ARGS = new Object[0];
    private final MessageSource messageSource;

    @ExceptionHandler(LaterCommonException.class)
    public ProblemDetail handleLaterCommonException(LaterCommonException e) {
        String localizedMessage = messageSource.getMessage(e.getMessage(), EMPTY_ARGS, e.getMessage(), LocaleContextHolder.getLocale());
        return ProblemDetail.forStatusAndDetail(e.getHttpStatus(), localizedMessage);
    }
}
