package com.karaoke_management.repository;

import com.karaoke_management.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Check booking conflict theo quy tắc:
     * existing.start < new.end AND existing.end > new.start
     *
     * - Khi tạo mới: excludeId = -1
     * - Khi update: excludeId = booking.getId()
     */
    @Query("""
        SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
        FROM Booking b
        WHERE b.room.id = :roomId
          AND (:excludeId = -1 OR b.id <> :excludeId)
          AND b.startTime < :endTime
          AND b.endTime > :startTime
    """)
    boolean existsBookingConflict(
            @Param("roomId") Long roomId,
            @Param("excludeId") Long excludeId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
    @Query("""
        select (count(b) > 0)
        from Booking b
        where b.room.id = :roomId
          and (:excludeId is null or b.id <> :excludeId)
          and (:start < b.endTime and :end > b.startTime)
    """)
    boolean existsOverlap(
            @Param("roomId") Long roomId,
            @Param("excludeId") Long excludeId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
