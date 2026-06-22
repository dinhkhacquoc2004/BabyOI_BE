package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.common.Constants;
import com.example.babyoi_be.common.VaccineRuleConstants;
import com.example.babyoi_be.domain.dto.request.VaccineRecordRequest;
import com.example.babyoi_be.domain.dto.respone.VaccineRecordResponse;
import com.example.babyoi_be.domain.entity.Profile;
import com.example.babyoi_be.domain.entity.Users;
import com.example.babyoi_be.domain.entity.VaccineRecord;
import com.example.babyoi_be.repository.ChildVaccineDiseaseRepository;
import com.example.babyoi_be.repository.ProfileRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VaccineRecordServiceImplTest {

    private static final Long USER_ID = 10L;
    private static final Long PROFILE_ID = 20L;

    @Mock
    private VaccineRecordRepository vaccineRecordRepository;

    @Mock
    private VaccineRepository vaccineRepository;

    @Mock
    private ChildVaccineDiseaseRepository childVaccineDiseaseRepository;

    @Mock
    private ProfileRepository profileRepository;

    @InjectMocks
    private VaccineRecordServiceImpl vaccineRecordService;

    @BeforeEach
    void setUpSecurityContext() {
        Users user = Users.builder()
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
        when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(Profile.builder()
                .id(PROFILE_ID)
                .user(user)
                .name("Baby")
                .build()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createVaccineRecordKeepsCanceledStatusForFutureDose() {
        LocalDate futureDate = LocalDate.now().plusMonths(1);
        VaccineRecordRequest request = VaccineRecordRequest.builder()
                .profileId(PROFILE_ID)
                .doseOrder(2)
                .source(VaccineRuleConstants.RECORD_SOURCE.STANDARD)
                .injectionDate(futureDate)
                .status(Constants.TABLE_STATUS.CANCELED)
                .build();

        when(vaccineRecordRepository.save(any(VaccineRecord.class))).thenAnswer(invocation -> {
            VaccineRecord record = invocation.getArgument(0);
            record.setId(100L);
            return record;
        });

        VaccineRecordResponse response = vaccineRecordService.createVaccineRecord(request);

        assertThat(response.getStatus()).isEqualTo(Constants.TABLE_STATUS.CANCELED);
        assertThat(response.getInjectionDate()).isEqualTo(futureDate);
        assertThat(response.getActualInjectionDate()).isNull();
    }

    @Test
    void updateVaccineRecordKeepsCanceledStatusForFutureDose() {
        LocalDate futureDate = LocalDate.now().plusMonths(1);
        VaccineRecord existingRecord = VaccineRecord.builder()
                .id(100L)
                .profileId(PROFILE_ID)
                .doseOrder(2)
                .source(VaccineRuleConstants.RECORD_SOURCE.STANDARD)
                .injectionDate(futureDate)
                .status(Constants.TABLE_STATUS.PENDING)
                .build();
        VaccineRecordRequest request = VaccineRecordRequest.builder()
                .profileId(PROFILE_ID)
                .doseOrder(2)
                .source(VaccineRuleConstants.RECORD_SOURCE.STANDARD)
                .injectionDate(futureDate)
                .status(Constants.TABLE_STATUS.CANCELED)
                .build();

        when(vaccineRecordRepository.findById(100L)).thenReturn(Optional.of(existingRecord));
        when(vaccineRecordRepository.save(any(VaccineRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VaccineRecordResponse response = vaccineRecordService.updateVaccineRecord(100L, request);

        assertThat(response.getStatus()).isEqualTo(Constants.TABLE_STATUS.CANCELED);
        assertThat(response.getInjectionDate()).isEqualTo(futureDate);
        assertThat(response.getActualInjectionDate()).isNull();
    }
}
