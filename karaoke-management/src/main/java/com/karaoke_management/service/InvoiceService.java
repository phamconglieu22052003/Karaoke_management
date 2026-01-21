package com.karaoke_management.service;

import com.karaoke_management.entity.Invoice;

import java.util.List;

public interface InvoiceService {
    Invoice createOrGetBySession(Long roomSessionId);
    Invoice getRequired(Long id);
    List<Invoice> findAll();
}
