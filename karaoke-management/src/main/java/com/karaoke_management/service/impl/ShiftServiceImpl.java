package com.karaoke_management.service.impl;

import com.karaoke_management.entity.Shift;
import com.karaoke_management.enums.ShiftStatus;
import com.karaoke_management.repository.ShiftRepository;
import com.karaoke_management.service.ShiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service cho module ca làm.
 *
 * Lưu ý: Tính toán doanh thu/breakdown đang được thực hiện ở ShiftController
 * (để hiển thị báo cáo). Entity Shift hiện tại chỉ lưu thông tin mở/đóng ca,
 * tiền đầu ca, tiền khai báo cuối ca và ghi chú.
 */
@Service
@RequiredArgsConstructor
public class ShiftServiceImpl implements ShiftService {

    private final ShiftRepository shiftRepository;

    @Override
    public Optional<Shift> getCurrentOpenShift() {
        return shiftRepository.findTopByStatusOrderByOpenedAtDesc(ShiftStatus.OPEN);
    }

    @Override
    @Transactional
    public Shift openShift(BigDecimal openingCash, String openedBy) {
        if (shiftRepository.existsByStatus(ShiftStatus.OPEN)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đang có ca đang mở, không thể mở ca mới");
        }

        Shift s = new Shift();
        s.setStatus(ShiftStatus.OPEN);
        s.setOpenedAt(LocalDateTime.now());
        s.setOpenedBy(openedBy == null ? "system" : openedBy);
        s.setOpeningCash(openingCash == null ? BigDecimal.ZERO : openingCash);
        return shiftRepository.save(s);
    }

    @Override
    @Transactional
    public Shift closeShift(Long shiftId, BigDecimal closingCashDeclared, String closedBy) {
        Shift s = getById(shiftId);
        if (s.getStatus() == ShiftStatus.CLOSED) {
            return s;
        }

        s.setStatus(ShiftStatus.CLOSED);
        s.setClosedAt(LocalDateTime.now());
        s.setClosedBy(closedBy == null ? "system" : closedBy);
        s.setClosingCashDeclared(closingCashDeclared);
        return shiftRepository.save(s);
    }

    @Override
    public Shift getById(Long shiftId) {
        return shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy ca"));
    }
}
