package com.example.babyoi_be.domain.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseMessage<T> {
    private Boolean success;
    private String status;
    private String message;
    private T data;
    private Map<String, String> errors;
}
