package com.karaoke_management.service.impl;

import com.karaoke_management.entity.Invoice;
import com.karaoke_management.entity.InvoiceStatus;
import com.karaoke_management.entity.RoomSession;
import com.karaoke_management.entity.RoomSessionStatus;
import com.karaoke_management.repository.InvoiceRepository;
import com.karaoke_management.repository.RoomSessionRepository;
import com.karaoke_management.service.InvoiceService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final RoomSessionRepository roomSessionRepository;

    public InvoiceServiceImpl(InvoiceRepository invoiceRepository,
                              RoomSessionRepository roomSessionRepository) {
        this.invoiceRepository = invoiceRepository;
        this.roomSessionRepository = roomSessionRepository;
    }

    @Override
    public Invoice createOrGetBySession(Long roomSessionId) {

        // ✅ FIX: dùng Optional rõ ràng
        Optional<Invoice> existed = invoiceRepository.findByRoomSession_Id(roomSessionId);
        if (existed.isPresent()) {
            return existed.get();
        }

        RoomSession session = roomSessionRepository.findById(roomSessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "RoomSession not found"
                ));

        if (session.getStatus() != RoomSessionStatus.CLOSED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Session chưa đóng phòng, không thể tạo hóa đơn"
            );
        }

        BigDecimal total = session.getTotalAmount();
        if (total == null) total = BigDecimal.ZERO;

        Invoice inv = new Invoice();
        inv.setRoomSession(session);
        inv.setTotalAmount(total);
        inv.setStatus(InvoiceStatus.UNPAID);

        return invoiceRepository.save(inv);
    }

    @Override
    @Transactional(readOnly = true)
    public Invoice getRequired(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invoice not found"
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Invoice> findAll() {
        return invoiceRepository.findAll();
    }
}
