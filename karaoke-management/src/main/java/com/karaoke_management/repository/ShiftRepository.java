package com.karaoke_management.repository;

import com.karaoke_management.entity.Shift;
import com.karaoke_management.enums.ShiftStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShiftRepository extends JpaRepository<Shift, Long> {
    boolean existsByStatus(ShiftStatus status);

    Optional<Shift> findTopByStatusOrderByOpenedAtDesc(ShiftStatus status);

    @Query("""
        SELECT s
        FROM Shift s
        WHERE (:status IS NULL OR s.status = :status)
          AND (:from IS NULL OR s.openedAt >= :from)
          AND (:to IS NULL OR s.openedAt <= :to)
        ORDER BY s.openedAt DESC
    """)
    List<Shift> filterShifts(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("status") ShiftStatus status
    );
}
