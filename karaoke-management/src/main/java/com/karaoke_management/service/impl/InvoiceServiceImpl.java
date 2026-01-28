package com.karaoke_management.service.impl;

import com.karaoke_management.entity.Invoice;
import com.karaoke_management.entity.InvoiceStatus;
import com.karaoke_management.entity.RoomSession;
import com.karaoke_management.repository.InvoiceRepository;
import com.karaoke_management.repository.RoomSessionRepository;
import com.karaoke_management.service.InvoiceService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Sort;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final RoomSessionRepository roomSessionRepository;

    public InvoiceServiceImpl(InvoiceRepository invoiceRepository,
                              RoomSessionRepository roomSessionRepository) {
        this.invoiceRepository = invoiceRepository;
        this.roomSessionRepository = roomSessionRepository;
    }

    @Override
    public List<Invoice> findAll() {
        return invoiceRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @Override
    public Invoice getRequired(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy hóa đơn ID = " + id
                ));
    }

    @Override
    @Transactional
    public Invoice createOrGetBySession(Long roomSessionId) {

        RoomSession session = roomSessionRepository.findById(roomSessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy phiên phòng ID = " + roomSessionId
                ));

        // chỉ tạo hóa đơn khi đã checkout
        if (session.getEndTime() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Phiên phòng chưa kết thúc, không thể tạo hóa đơn"
            );
        }

        Optional<Invoice> existed = invoiceRepository.findByRoomSessionId(roomSessionId);
        if (existed.isPresent()) return existed.get();

        if (session.getRoom() == null || session.getRoom().getRoomType() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Thiếu thông tin loại phòng để tính tiền"
            );
        }

        BigDecimal totalAmount = calculateTotal(session);

        Invoice inv = new Invoice();
        inv.setRoomSession(session);
        inv.setTotalAmount(totalAmount);
        inv.setStatus(InvoiceStatus.UNPAID);

        // ✅ FIX UNIQUE NULL (SQL Server unique không cho nhiều NULL)
        // Đảm bảo vnpTxnRef luôn có giá trị và unique
        inv.setVnpTxnRef("INV-" + roomSessionId + "-" + System.currentTimeMillis());

        return invoiceRepository.save(inv);
    }

    private BigDecimal calculateTotal(RoomSession session) {
        BigDecimal pricePerHour = session.getRoom().getRoomType().getPricePerHour();
        if (pricePerHour == null) return BigDecimal.ZERO;

        long minutes;
        if (session.getTotalMinutes() != null && session.getTotalMinutes() > 0) {
            minutes = session.getTotalMinutes();
        } else {
            if (session.getStartTime() == null || session.getEndTime() == null) return BigDecimal.ZERO;
            minutes = Duration.between(session.getStartTime(), session.getEndTime()).toMinutes();
        }
        if (minutes < 0) minutes = 0;

        return pricePerHour
                .multiply(BigDecimal.valueOf(minutes))
                .divide(BigDecimal.valueOf(60), 0, RoundingMode.HALF_UP);
    }
}
