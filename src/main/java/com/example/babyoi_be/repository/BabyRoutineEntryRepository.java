package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.BabyRoutineEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface BabyRoutineEntryRepository extends JpaRepository<BabyRoutineEntry, Long> {
    List<BabyRoutineEntry> findByProfile_IdAndRoutineDateAndStatusOrderByPlannedTimeAscIdAsc(
            Long profileId,
            LocalDate routineDate,
            Long status
    );

    List<BabyRoutineEntry> findByProfile_IdAndRoutineDateOrderByPlannedTimeAscIdAsc(
            Long profileId,
            LocalDate routineDate
    );

    List<BabyRoutineEntry> findByRoutineDateBeforeAndStatusAndCompletedFalseOrderByRoutineDateAscPlannedTimeAscIdAsc(
            LocalDate routineDate,
            Long status
    );

    List<BabyRoutineEntry> findByRoutineDateAndStatusAndCompletedFalseAndPlannedTimeLessThanEqualOrderByPlannedTimeAscIdAsc(
            LocalDate routineDate,
            Long status,
            LocalTime plannedTime
    );

    List<BabyRoutineEntry> findByRoutineDateAndStatusAndPlannedTimeGreaterThanEqualAndPlannedTimeBeforeOrderByPlannedTimeAscIdAsc(
            LocalDate routineDate,
            Long status,
            LocalTime fromTime,
            LocalTime toTime
    );
}
