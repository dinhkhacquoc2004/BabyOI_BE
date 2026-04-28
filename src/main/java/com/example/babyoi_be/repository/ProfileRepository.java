package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.Profile;
import com.example.babyoi_be.domain.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {
    List<Profile> findByUser(Users user);
    List<Profile> findByUserId(Long userId);
    List<Profile> findByProfileType(String profileType);
}
