package com.example.babyoi_be.domain.dto.respone;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaccineProgressResponse {
    private Long diseaseId;
    private String diseaseCode;
    private String diseaseName;
    private Long groupId;
    private String groupCode;
    private String groupName;
    private Integer totalDoses;
    private Integer completedDoses;
    private Integer pendingDoses;
    private Integer overdueDoses;
    private Integer upcomingDoses;
    private Integer currentDoseOrder;
    private Integer nextDoseOrder;
    private LocalDate nextInjectionDate;
    private Boolean stopped;
    private Long diseaseScheduleStatus;
    private LocalDateTime stoppedAt;
    private Integer stoppedDoseOrder;
    private Long interchangeRule;
    private String interchangeRuleCode;
    private List<String> productFamilyCodes;
    private List<String> warnings;
    private List<VaccineProgressDoseResponse> doses;
}
