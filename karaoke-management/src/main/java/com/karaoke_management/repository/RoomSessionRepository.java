package com.karaoke_management.repository;

import com.karaoke_management.entity.RoomSession;
import com.karaoke_management.entity.RoomSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RoomSessionRepository extends JpaRepository<RoomSession, Long> {

    // lấy session mới nhất lên trước
    List<RoomSession> findAllByOrderByStartTimeDesc();

    // dùng để chặn mở phòng trùng
    boolean existsByRoom_IdAndStatus(Long roomId, RoomSessionStatus status);

    // Lọc lịch sử mở/đóng phòng
    @Query("""
        SELECT rs
        FROM RoomSession rs
        WHERE (:from IS NULL OR rs.startTime >= :from)
          AND (:to IS NULL OR rs.startTime <= :to)
          AND (:status IS NULL OR rs.status = :status)
          AND (:roomId IS NULL OR rs.room.id = :roomId)
        ORDER BY rs.startTime DESC
    """)
    List<RoomSession> filterSessions(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("status") RoomSessionStatus status,
            @Param("roomId") Long roomId
    );

    // Đếm nhanh các session đang hoạt động để kiểm tra đóng ca
    long countByStatusIn(List<RoomSessionStatus> statuses);

    // List các session đang hoạt động để hiển thị cảnh báo khi đóng ca
    List<RoomSession> findByStatusInOrderByStartTimeDesc(List<RoomSessionStatus> statuses);

    // Session đã đóng nhưng chưa lập hóa đơn (để tránh lệch doanh thu khi đóng ca)
    @Query("""
        select rs
        from RoomSession rs
        join fetch rs.room r
        where rs.status = :closedStatus
          and rs.endTime is not null
          and rs.endTime >= :from
          and rs.endTime <= :to
          and not exists (
              select 1 from Invoice i where i.roomSession = rs
          )
        order by rs.endTime desc
    """)
    List<RoomSession> findClosedWithoutInvoiceBetween(
            @Param("closedStatus") RoomSessionStatus closedStatus,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
        select count(rs)
        from RoomSession rs
        where rs.status = :closedStatus
          and rs.endTime is not null
          and rs.endTime >= :from
          and rs.endTime <= :to
          and not exists (
              select 1 from Invoice i where i.roomSession = rs
          )
    """)
    long countClosedWithoutInvoiceBetween(
            @Param("closedStatus") RoomSessionStatus closedStatus,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
