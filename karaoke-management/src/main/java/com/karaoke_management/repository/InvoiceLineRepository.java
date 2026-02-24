package com.karaoke_management.repository;

import com.karaoke_management.entity.InvoiceLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, Long> {
    List<InvoiceLine> findAllByInvoice_IdOrderByIdAsc(Long invoiceId);

    void deleteByInvoice_Id(Long invoiceId);
}
