package com.example.babyoi_be.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpRequest {
    @Email(message = "{auth.email.invalid}")
    @NotBlank(message = "{auth.email.required}")
    private String email;

    @NotBlank(message = "Vui lòng nhập mã xác nhận")
    @Pattern(regexp = "^\\d{6}$", message = "Mã xác nhận phải gồm 6 số")
    private String code;
}
