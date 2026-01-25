package com.karaoke_management.repository;

import com.karaoke_management.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByVnpTxnRef(String vnpTxnRef);
    Optional<Invoice> findByRoomSession_Id(Long roomSessionId);
    List<Invoice> findAllByOrderByIdDesc();

     @Query("select i from Invoice i where i.roomSession.id = :sessionId")
    Optional<Invoice> findByRoomSessionId(@Param("sessionId") Long sessionId);
}
