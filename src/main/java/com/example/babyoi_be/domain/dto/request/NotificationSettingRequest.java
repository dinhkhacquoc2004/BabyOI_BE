package com.example.babyoi_be.domain.dto.request;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingRequest {
    private Boolean pushEnabled;
    private Boolean vaccineEnabled;
    private Boolean appointmentEnabled;
    private Boolean chatEnabled;
    private Boolean promotionEnabled;
    private Boolean systemEnabled;
}
