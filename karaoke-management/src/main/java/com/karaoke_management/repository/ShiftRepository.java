package com.karaoke_management.repository;

import com.karaoke_management.entity.Shift;
import com.karaoke_management.enums.ShiftStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShiftRepository extends JpaRepository<Shift, Long> {
    boolean existsByStatus(ShiftStatus status);

    Optional<Shift> findTopByStatusOrderByOpenedAtDesc(ShiftStatus status);
}
