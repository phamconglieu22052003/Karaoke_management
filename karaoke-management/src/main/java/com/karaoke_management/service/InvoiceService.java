package com.karaoke_management.service;

import com.karaoke_management.entity.Invoice;

import java.util.List;

public interface InvoiceService {
    Invoice createOrGetBySession(Long roomSessionId);
    Invoice getRequired(Long id);
    List<Invoice> findAll();

    /**
     * Đánh dấu hóa đơn đã thanh toán và tự động trừ kho theo OrderItem của phiên phòng.
     * Idempotent: nếu đã trừ kho thì không trừ lại.
     */
    Invoice markPaidAndDeductInventory(Long invoiceId, String paymentMethod, String transactionNo);
}
