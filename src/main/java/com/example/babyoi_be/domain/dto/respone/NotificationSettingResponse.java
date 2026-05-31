package com.example.babyoi_be.domain.dto.respone;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingResponse {
    private Boolean pushEnabled;
    private Boolean vaccineEnabled;
    private Boolean appointmentEnabled;
    private Boolean chatEnabled;
    private Boolean promotionEnabled;
    private Boolean systemEnabled;
}
