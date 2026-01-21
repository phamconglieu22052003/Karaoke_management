package com.karaoke_management.service.impl;

import com.karaoke_management.entity.Booking;
import com.karaoke_management.repository.BookingRepository;
import com.karaoke_management.service.BookingService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;

    public BookingServiceImpl(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Override
    public List<Booking> findAll() {
        return bookingRepository.findAll();
    }

    @Override
    public Booking findById(Long id) {
        // Nếu bạn bị lỗi .orElseThrow() thì dùng kiểu này luôn cho chắc
        return bookingRepository.findById(id).orElse(null);
    }

    @Override
    public Booking save(Booking booking) {
        // 1) Validate dữ liệu bắt buộc
        if (booking.getRoom() == null || booking.getRoom().getId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn phòng");
        }
        if (booking.getStartTime() == null) {
            throw new IllegalArgumentException("Vui lòng chọn giờ bắt đầu");
        }
        if (booking.getEndTime() == null) {
            throw new IllegalArgumentException("Vui lòng chọn giờ kết thúc");
        }

        // 2) end_time phải sau start_time
        if (!booking.getEndTime().isAfter(booking.getStartTime())) {
            throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu");
        }

        // 3) Chặn trùng giờ
        Long excludeId = (booking.getId() == null) ? -1L : booking.getId();
        boolean conflict = bookingRepository.existsBookingConflict(
                booking.getRoom().getId(),
                excludeId,
                booking.getStartTime(),
                booking.getEndTime()
        );

        if (conflict) {
            throw new IllegalArgumentException("Phòng đã được đặt trong khoảng thời gian này");
        }

        // 4) Save
        return bookingRepository.save(booking);
    }

    @Override
    public void deleteById(Long id) {
        bookingRepository.deleteById(id);
    }
}
