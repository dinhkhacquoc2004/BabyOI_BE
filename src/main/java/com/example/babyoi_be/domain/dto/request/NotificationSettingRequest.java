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
    private Boolean vaccineInAppEnabled;
    private Boolean vaccinePushEnabled;
    private Boolean appointmentInAppEnabled;
    private Boolean appointmentPushEnabled;
    private Boolean chatInAppEnabled;
    private Boolean chatPushEnabled;
    private Boolean promotionInAppEnabled;
    private Boolean promotionPushEnabled;
    private Boolean systemInAppEnabled;
    private Boolean systemPushEnabled;
}
