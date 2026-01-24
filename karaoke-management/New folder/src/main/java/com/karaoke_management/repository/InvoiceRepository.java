package com.karaoke_management.repository;

import com.karaoke_management.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByVnpTxnRef(String vnpTxnRef);
    Optional<Invoice> findByRoomSession_Id(Long roomSessionId);
    List<Invoice> findAllByOrderByIdDesc();
}
