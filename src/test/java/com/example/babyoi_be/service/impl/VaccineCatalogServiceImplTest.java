package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.common.Constants;
import com.example.babyoi_be.common.VaccineRuleConstants;
import com.example.babyoi_be.domain.dto.respone.VaccineProgressResponse;
import com.example.babyoi_be.domain.entity.ChildDiseaseDoseSchedule;
import com.example.babyoi_be.domain.entity.ChildVaccineDisease;
import com.example.babyoi_be.domain.entity.Profile;
import com.example.babyoi_be.domain.entity.Users;
import com.example.babyoi_be.domain.entity.Vaccine;
import com.example.babyoi_be.domain.entity.VaccineDiseaseCoverage;
import com.example.babyoi_be.domain.entity.VaccineRecord;
import com.example.babyoi_be.repository.ChildDiseaseDoseScheduleRepository;
import com.example.babyoi_be.repository.ChildDiseaseDoseVaccineOptionRepository;
import com.example.babyoi_be.repository.ChildVaccineDiseaseRepository;
import com.example.babyoi_be.repository.ProfileRepository;
import com.example.babyoi_be.repository.ProfileVaccineDiseaseStatusRepository;
import com.example.babyoi_be.repository.VaccineDiseaseCoverageRepository;
import com.example.babyoi_be.repository.VaccineRecordRepository;
import com.example.babyoi_be.repository.VaccineRepository;
import com.example.babyoi_be.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VaccineCatalogServiceImplTest {

    private static final Long USER_ID = 10L;
    private static final Long PROFILE_ID = 20L;

    @Mock
    private VaccineRepository vaccineRepository;

    @Mock
    private ChildVaccineDiseaseRepository childVaccineDiseaseRepository;

    @Mock
    private VaccineDiseaseCoverageRepository vaccineDiseaseCoverageRepository;

    @Mock
    private ChildDiseaseDoseScheduleRepository childDiseaseDoseScheduleRepository;

    @Mock
    private ChildDiseaseDoseVaccineOptionRepository childDiseaseDoseVaccineOptionRepository;

    @Mock
    private ProfileVaccineDiseaseStatusRepository profileVaccineDiseaseStatusRepository;

    @Mock
    private VaccineRecordRepository vaccineRecordRepository;

    @Mock
    private ProfileRepository profileRepository;

    @InjectMocks
    private VaccineCatalogServiceImpl vaccineCatalogService;

    private Users user;

    @BeforeEach
    void setUpSecurityContext() {
        user = Users.builder()
                .id(USER_ID)
                .email("parent@example.com")
                .userName("parent")
                .passwordHash("password")
                .status(Constants.TABLE_STATUS.ACTIVE)
                .emailVerified(true)
                .build();
        CustomUserDetails userDetails = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getVaccineProgressTreatsNotUpdatedPastDoseAsCompletedForNextDose() {
        LocalDate dateOfBirth = LocalDate.now().minusMonths(3);
        Profile profile = childProfile(dateOfBirth);
        ChildVaccineDisease disease = childDisease(1L, "DTP", "Bach hau - ho ga - uon van");
        List<ChildDiseaseDoseSchedule> schedules = List.of(
                schedule(disease, 1, 1),
                schedule(disease, 2, 4)
        );
        List<VaccineRecord> records = List.of(
                standardRecord(101L, disease, 1, dateOfBirth.plusMonths(1), Constants.TABLE_STATUS.INACTIVE),
                standardRecord(102L, disease, 2, dateOfBirth.plusMonths(4), Constants.TABLE_STATUS.PENDING)
        );

        mockProgressData(profile, List.of(disease), schedules, records, List.of());

        VaccineProgressResponse progress = vaccineCatalogService.getVaccineProgress(PROFILE_ID).get(0);

        assertThat(progress.getCompletedDoses()).isEqualTo(1);
        assertThat(progress.getPendingDoses()).isEqualTo(1);
        assertThat(progress.getNextDoseOrder()).isEqualTo(2);
        assertThat(progress.getDoses()).extracting("status")
                .containsExactly(Constants.TABLE_STATUS.INACTIVE, Constants.TABLE_STATUS.PENDING);
    }

    @Test
    void getVaccineProgressMapsCombinationVaccineToCoveredDiseaseDoseOrder() {
        LocalDate dateOfBirth = LocalDate.now().minusMonths(2);
        Profile profile = childProfile(dateOfBirth);
        ChildVaccineDisease dtp = childDisease(1L, "DTP", "Bach hau - ho ga - uon van");
        ChildVaccineDisease hepatitisB = childDisease(2L, "HEPB", "Viem gan B");
        Vaccine hexa = Vaccine.builder().id(500L).name("Hexa").status(Constants.TABLE_STATUS.ACTIVE).build();
        List<ChildDiseaseDoseSchedule> schedules = List.of(
                schedule(dtp, 1, 2),
                schedule(dtp, 2, 4),
                schedule(hepatitisB, 1, 0),
                schedule(hepatitisB, 2, 2)
        );
        VaccineRecord hexaRecord = VaccineRecord.builder()
                .id(201L)
                .profileId(PROFILE_ID)
                .disease(dtp)
                .doseOrder(1)
                .source(VaccineRuleConstants.RECORD_SOURCE.STANDARD)
                .vaccine(hexa)
                .injectionDate(dateOfBirth.plusMonths(2).plusDays(5))
                .actualInjectionDate(dateOfBirth.plusMonths(2).plusDays(5))
                .status(Constants.TABLE_STATUS.SUCCESS)
                .build();
        List<VaccineDiseaseCoverage> coverages = List.of(
                coverage(hexa, dtp),
                coverage(hexa, hepatitisB)
        );

        mockProgressData(profile, List.of(dtp, hepatitisB), schedules, List.of(hexaRecord), coverages);

        VaccineProgressResponse hepatitisBProgress = vaccineCatalogService.getVaccineProgress(PROFILE_ID)
                .stream()
                .filter(progress -> hepatitisB.getId().equals(progress.getDiseaseId()))
                .findFirst()
                .orElseThrow();

        assertThat(hepatitisBProgress.getCompletedDoses()).isEqualTo(2);
        assertThat(hepatitisBProgress.getDoses()).anySatisfy(dose -> {
            assertThat(dose.getRecordId()).isEqualTo(201L);
            assertThat(dose.getDoseOrder()).isEqualTo(2);
            assertThat(dose.getVaccineName()).isEqualTo("Hexa");
        });
    }

    private void mockProgressData(
            Profile profile,
            List<ChildVaccineDisease> diseases,
            List<ChildDiseaseDoseSchedule> schedules,
            List<VaccineRecord> records,
            List<VaccineDiseaseCoverage> coverages
    ) {
        when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));
        when(childVaccineDiseaseRepository.findByStatusOrderByDisplayOrderAscIdAsc(Constants.TABLE_STATUS.ACTIVE)).thenReturn(diseases);
        when(profileVaccineDiseaseStatusRepository.findByProfileId(PROFILE_ID)).thenReturn(List.of());
        when(vaccineRecordRepository.findByProfileId(PROFILE_ID)).thenReturn(records);
        when(childDiseaseDoseScheduleRepository.findByStatusOrderByDiseaseDisplayOrderAscDoseOrderAsc(Constants.TABLE_STATUS.ACTIVE)).thenReturn(schedules);
        if (records.stream().anyMatch(record -> record.getVaccine() != null)) {
            when(vaccineDiseaseCoverageRepository.findByVaccineIdInAndStatus(anyCollection(), org.mockito.ArgumentMatchers.eq(Constants.TABLE_STATUS.ACTIVE)))
                    .thenReturn(coverages);
        }
    }

    private Profile childProfile(LocalDate dateOfBirth) {
        return Profile.builder()
                .id(PROFILE_ID)
                .user(user)
                .name("Baby")
                .profileType("CHILD")
                .dateOfBirth(dateOfBirth)
                .build();
    }

    private ChildVaccineDisease childDisease(Long id, String code, String name) {
        return ChildVaccineDisease.builder()
                .id(id)
                .code(code)
                .name(name)
                .displayOrder(id.intValue())
                .status(Constants.TABLE_STATUS.ACTIVE)
                .build();
    }

    private ChildDiseaseDoseSchedule schedule(ChildVaccineDisease disease, int doseOrder, int recommendedAgeMonths) {
        return ChildDiseaseDoseSchedule.builder()
                .disease(disease)
                .doseOrder(doseOrder)
                .recommendedAgeMonths(recommendedAgeMonths)
                .doseLabel("Mui " + doseOrder)
                .status(Constants.TABLE_STATUS.ACTIVE)
                .build();
    }

    private VaccineRecord standardRecord(Long id, ChildVaccineDisease disease, int doseOrder, LocalDate injectionDate, Long status) {
        return VaccineRecord.builder()
                .id(id)
                .profileId(PROFILE_ID)
                .disease(disease)
                .doseOrder(doseOrder)
                .source(VaccineRuleConstants.RECORD_SOURCE.STANDARD)
                .injectionDate(injectionDate)
                .status(status)
                .build();
    }

    private VaccineDiseaseCoverage coverage(Vaccine vaccine, ChildVaccineDisease disease) {
        return VaccineDiseaseCoverage.builder()
                .vaccine(vaccine)
                .disease(disease)
                .interchangeRule(VaccineRuleConstants.INTERCHANGE_RULE.PREFER_SAME_PRODUCT)
                .status(Constants.TABLE_STATUS.ACTIVE)
                .build();
    }
}
