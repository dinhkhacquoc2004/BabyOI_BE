package com.example.babyoi_be.controller;

import com.example.babyoi_be.domain.message.ResponseMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static com.example.babyoi_be.common.Constants.STATUS_COMMON.RESPONSE_STATUS_FALSE;
import static com.example.babyoi_be.common.Constants.STATUS_COMMON.RESPONSE_STATUS_TRUE;

public abstract class CommonController {

    protected <T> ResponseEntity<ResponseMessage<T>> toSuccessResult(
            T data,
            String returnCode,
            String successMessage,
            HttpStatus httpStatus
    ) {
        ResponseMessage<T> message = ResponseMessage.<T>builder()
                .success(RESPONSE_STATUS_TRUE)
                .status(returnCode)
                .message(successMessage)
                .data(data)
                .build();

        return new ResponseEntity<>(message, httpStatus);
    }

    protected ResponseEntity<ResponseMessage<Object>> toExceptionResult(
            String errorMessage,
            String returnCode,
            HttpStatus httpStatus,
            Map<String, String> errors
    ) {
        ResponseMessage<Object> message = ResponseMessage.builder()
                .success(RESPONSE_STATUS_FALSE)
                .status(returnCode)
                .message(errorMessage)
                .errors(errors)
                .build();

        return new ResponseEntity<>(message, httpStatus);
    }
}
