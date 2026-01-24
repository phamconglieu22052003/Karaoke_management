package com.karaoke_management.service;

import com.karaoke_management.entity.Booking;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingService {
    List<Booking> findAll();

    Booking save(Booking booking);

    Booking findById(Long id);

    void deleteById(Long id);

    /**
     * Kiểm tra trùng giờ đặt phòng (cùng phòng, khoảng thời gian bị chồng lấn).
     * excludeBookingId: dùng khi update (loại trừ chính booking đang sửa). Nếu tạo mới thì truyền null.
     */
    boolean hasBookingConflict(Long roomId, LocalDateTime startTime, LocalDateTime endTime, Long excludeBookingId);
}
