package com.example.babyoi_be.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialLoginRequest {
    @NotBlank(message = "Vui lòng chọn nhà cung cấp đăng nhập")
    private String provider;

    private String idToken;

    private String accessToken;
}
