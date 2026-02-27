package com.karaoke_management.service;

import com.karaoke_management.entity.Shift;

import java.math.BigDecimal;
import java.util.Optional;

public interface ShiftService {
    Optional<Shift> getCurrentOpenShift();
    Shift openShift(BigDecimal openingCash, String openedBy);
    Shift closeShift(Long shiftId, BigDecimal closingCash, String closedBy);
    Shift getById(Long shiftId);
}