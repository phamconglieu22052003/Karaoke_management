package com.karaoke_management.repository;

import com.karaoke_management.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByRoomSession_Id(Long roomSessionId);
    Optional<Invoice> findByVnpTxnRef(String vnpTxnRef);
}
