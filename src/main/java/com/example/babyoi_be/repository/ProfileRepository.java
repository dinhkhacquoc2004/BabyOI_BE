package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.Profile;
import com.example.babyoi_be.domain.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {
    List<Profile> findByUserId(Long userId);
    List<Profile> findByUserIdAndStatus(Long userId, Long status);
    List<Profile> findByProfileType(String profileType);
    List<Profile> findByProfileTypeAndStatus(String profileType, Long status);
    
    long countByUserIdAndStatusIn(Long userId, Collection<Long> statuses);
    long countByUserIdAndProfileTypeAndStatus(Long userId, String profileType, Long status);
    long countByUserIdAndProfileTypeAndStatusAndIdNot(Long userId, String profileType, Long status, Long id);
    boolean existsByUserIdAndStatusIn(Long userId, Collection<Long> statuses);
}
