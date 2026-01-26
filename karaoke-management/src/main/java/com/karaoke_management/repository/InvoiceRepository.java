package com.karaoke_management.repository;

import com.karaoke_management.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    // Giữ chức năng cũ
    List<Invoice> findAllByOrderByIdDesc();

    // Giữ chức năng tạo hóa đơn theo session (nếu bạn đang dùng)
    @Query("select i from Invoice i where i.roomSession.id = :sessionId")
    Optional<Invoice> findByRoomSessionId(@Param("sessionId") Long sessionId);

    // ✅ Lọc theo createdAt + totalAmount (param có thể null)
    @Query("""
        SELECT i
        FROM Invoice i
        WHERE (:from IS NULL OR i.createdAt >= :from)
          AND (:to   IS NULL OR i.createdAt <= :to)
          AND (:min  IS NULL OR i.totalAmount >= :min)
          AND (:max  IS NULL OR i.totalAmount <= :max)
        ORDER BY i.id DESC
    """)
    List<Invoice> filterInvoices(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("min") BigDecimal min,
            @Param("max") BigDecimal max
    );
}
