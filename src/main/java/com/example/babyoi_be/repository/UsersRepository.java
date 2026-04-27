package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsersRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByEmail(String email);

    Boolean existsByEmail(String email);

    Boolean existsByUserName(String userName);
}
