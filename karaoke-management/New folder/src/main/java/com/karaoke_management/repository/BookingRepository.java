package com.karaoke_management.repository;

import com.karaoke_management.entity.Booking;
import com.karaoke_management.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Check overlap:
    // Overlap khi: start < existingEnd AND end > existingStart
    @Query("""
        select count(b) > 0
        from Booking b
        where b.room.id = :roomId
          and b.status = :status
          and :startTime < b.endTime
          and :endTime > b.startTime
    """)
    boolean existsOverlappingBooking(
            @Param("roomId") Long roomId,
            @Param("status") BookingStatus status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
