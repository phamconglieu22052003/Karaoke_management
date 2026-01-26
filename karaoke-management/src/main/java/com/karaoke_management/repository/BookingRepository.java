package com.karaoke_management.repository;

import com.karaoke_management.entity.Booking;
import com.karaoke_management.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // ================== LIST CŨ (GIỮ NGUYÊN) ==================
    List<Booking> findAll();

    List<Booking> findAllByOrderByIdDesc();

    // ================== CHỐNG TRÙNG GIỜ (GIỮ NGUYÊN CHỮ KÝ BẠN ĐANG CÓ) ==================
    // overlap nếu: startTime < endTime2 && endTime > startTime2
    @Query("""
        SELECT COUNT(b) > 0
        FROM Booking b
        WHERE b.room.id = :roomId
          AND (:excludeId IS NULL OR b.id <> :excludeId)
          AND b.startTime < :endTime
          AND b.endTime > :startTime
    """)
    boolean existsOverlap(
            @Param("roomId") Long roomId,
            @Param("excludeId") Long excludeId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // ================== (TÙY CHỌN) CHỐNG TRÙNG GIỜ NHƯNG BỎ QUA 1 STATUS (VD: CANCELLED) ==================
    // Nếu controller bạn muốn bỏ qua CANCELLED, dùng hàm này thay vì existsOverlap(...)
    @Query("""
        SELECT COUNT(b) > 0
        FROM Booking b
        WHERE b.room.id = :roomId
          AND (:excludeId IS NULL OR b.id <> :excludeId)
          AND (:excludedStatus IS NULL OR b.status <> :excludedStatus)
          AND b.startTime < :endTime
          AND b.endTime > :startTime
    """)
    boolean existsOverlapExcludingStatus(
            @Param("roomId") Long roomId,
            @Param("excludeId") Long excludeId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludedStatus") BookingStatus excludedStatus
    );

    // ================== LỌC BOOKING THEO KHOẢNG THỜI GIAN (OVERLAP RANGE) ==================
    // Hiển thị tất cả booking có giao với [from, to]
    // Điều kiện overlap: booking.end >= from AND booking.start <= to
    @Query("""
        SELECT b
        FROM Booking b
        WHERE (:from IS NULL OR b.endTime >= :from)
          AND (:to IS NULL OR b.startTime <= :to)
        ORDER BY b.id DESC
    """)
    List<Booking> filterByTimeRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
