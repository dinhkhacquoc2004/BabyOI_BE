package com.example.babyoi_be.domain.dto.respone;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthMonthlyDetailResponse {
    private HealthRecordResponse healthRecord;
    private List<IllnessEventResponse> illnesses;
}
