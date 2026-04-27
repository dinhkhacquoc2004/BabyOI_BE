package com.example.babyoi_be.controller;

import com.example.babyoi_be.common.utils.MessageUtils;
import com.example.babyoi_be.domain.message.ResponseMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.example.babyoi_be.common.Constants.API_RESPONSE.*;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends CommonController {

    private final MessageUtils messageUtils;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseMessage<Object>> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();

        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            if (!errors.containsKey(fieldError.getField())) {
                errors.put(fieldError.getField(), fieldError.getDefaultMessage());
            }
        }

        return toExceptionResult(
                messageUtils.getMessage("common.validation.failed"),
                RETURN_CODE_BAD_REQUEST,
                HttpStatus.BAD_REQUEST,
                errors
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ResponseMessage<Object>> handleResponseStatus(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        return toExceptionResult(
                messageUtils.getMessage(exception.getReason()),
                mapStatusCode(status),
                status,
                null
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseMessage<Object>> handleOtherExceptions(Exception exception) {
        return toExceptionResult(
                messageUtils.getMessage("common.error.internal"),
                RETURN_CODE_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR,
                null
        );
    }

    private String mapStatusCode(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> RETURN_CODE_BAD_REQUEST;
            case UNAUTHORIZED -> RETURN_CODE_UNAUTHORIZED;
            case FORBIDDEN -> RETURN_CODE_FORBIDDEN;
            case CONFLICT -> RETURN_CODE_CONFLICT;
            default -> String.valueOf(status.value());
        };
    }
}
