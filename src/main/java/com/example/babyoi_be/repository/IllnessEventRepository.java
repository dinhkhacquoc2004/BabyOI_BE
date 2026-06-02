package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.IllnessEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface IllnessEventRepository extends JpaRepository<IllnessEvent, Long> {
    @Query("""
            select event from IllnessEvent event
            where event.profile.id = :profileId
              and event.startAt <= :toDate
              and coalesce(event.endAt, event.startAt) >= :fromDate
              and (:status is null or event.status = :status)
            order by event.startAt desc, event.id desc
            """)
    List<IllnessEvent> findByProfileAndDateRange(
            @Param("profileId") Long profileId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("status") Long status
    );

    @Query("""
            select count(event) from IllnessEvent event
            where event.profile.id = :profileId
              and event.startAt <= :toDate
              and coalesce(event.endAt, event.startAt) >= :fromDate
            """)
    long countByProfileAndDateRange(
            @Param("profileId") Long profileId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
