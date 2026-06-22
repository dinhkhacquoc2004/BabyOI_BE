package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.domain.entity.BabyRoutineEntry;
import com.example.babyoi_be.repository.BabyRoutineEntryRepository;
import com.example.babyoi_be.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BabyRoutineServiceImplTest {

    private final BabyRoutineServiceImpl service = new BabyRoutineServiceImpl(
            mock(BabyRoutineEntryRepository.class),
            mock(ProfileRepository.class)
    );

    @Test
    void calculatesPositiveDelayWhenNightRoutineFinishesAfterMidnight() {
        Long result = ReflectionTestUtils.invokeMethod(
                service,
                "routineTimeDeltaMinutes",
                LocalTime.of(19, 30),
                LocalTime.of(0, 10)
        );

        assertThat(result).isEqualTo(280L);
    }

    @Test
    void excludesAutoCompletedEntriesFromTomorrowShift() {
        BabyRoutineEntry manuallyCompleted = BabyRoutineEntry.builder()
                .plannedTime(LocalTime.of(8, 0))
                .actualTime(LocalTime.of(8, 20))
                .source("AGE_GUIDELINE")
                .build();
        BabyRoutineEntry autoCompleted = BabyRoutineEntry.builder()
                .plannedTime(LocalTime.of(12, 0))
                .actualTime(LocalTime.of(12, 0))
                .source("AUTO_OVERDUE")
                .build();

        Integer result = ReflectionTestUtils.invokeMethod(
                service,
                "calculateRoutineShiftMinutes",
                List.of(manuallyCompleted, autoCompleted)
        );

        assertThat(result).isEqualTo(20);
    }
}
