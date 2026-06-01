package com.example.babyoi_be.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceTokenRequest {
    @NotBlank(message = "Device token is required")
    private String token;

    @NotBlank(message = "Platform is required")
    private String platform;

    @NotBlank(message = "Device ID is required")
    private String deviceId;

    private String appVersion;
}
