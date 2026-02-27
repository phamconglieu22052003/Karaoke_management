package com.karaoke_management.repository;

import com.karaoke_management.entity.Invoice;
import com.karaoke_management.entity.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    // ✅ cần cho InvoiceServiceImpl.createOrGetBySession(...)
    Optional<Invoice> findByRoomSessionId(Long roomSessionId);

    @Query("""
        select i from Invoice i
        join i.roomSession rs
        join rs.room r
        where (:from is null or i.createdAt >= :from)
          and (:to is null or i.createdAt <= :to)
          and (:min is null or i.totalAmount >= :min)
          and (:max is null or i.totalAmount <= :max)
          and (:roomId is null or r.id = :roomId)
        order by i.id desc
    """)
    List<Invoice> filterInvoices(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("min") BigDecimal min,
            @Param("max") BigDecimal max,
            @Param("roomId") Long roomId
    );

    /**
     * Dùng cho module ca làm (Shift): lấy các hóa đơn đã thanh toán trong khoảng thời gian,
     * đồng thời fetch lines + room/session để hiển thị & tính breakdown.
     */
    @Query("""
        select distinct i from Invoice i
        left join fetch i.lines l
        join fetch i.roomSession rs
        join fetch rs.room r
        where i.status = :status
          and i.paidAt is not null
          and i.paidAt >= :from
          and i.paidAt <= :to
        order by i.paidAt asc
    """)
    List<Invoice> findPaidWithLinesBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("status") InvoiceStatus status
    );
}
