package com.karaoke_management.service.impl;

import com.karaoke_management.entity.Booking;
import com.karaoke_management.repository.BookingRepository;
import com.karaoke_management.service.BookingService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;

    @PersistenceContext
    private EntityManager em;

    public BookingServiceImpl(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> findAll() {
        return bookingRepository.findAll();
    }

    @Override
    public Booking save(Booking booking) {
        return bookingRepository.save(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public Booking findById(Long id) {
        return bookingRepository.findById(id).orElse(null);
    }

    @Override
    public void deleteById(Long id) {
        bookingRepository.deleteById(id);
    }

    /**
     * Check trùng giờ đặt phòng:
     * Overlap nếu: startTime < existing.endTime AND endTime > existing.startTime
     * excludeBookingId: dùng khi update (loại trừ chính booking đang sửa), tạo mới thì null.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean hasBookingConflict(Long roomId,
                                      LocalDateTime startTime,
                                      LocalDateTime endTime,
                                      Long excludeBookingId) {

        if (roomId == null || startTime == null || endTime == null) return false;

        // thời gian không hợp lệ thì coi như conflict để chặn
        if (!startTime.isBefore(endTime)) return true;

        String jpql =
                "select count(b) " +
                "from Booking b " +
                "where b.room.id = :roomId " +
                "  and b.startTime < :endTime " +
                "  and b.endTime > :startTime " +
                "  and (:excludeId is null or b.id <> :excludeId)";

        Long cnt = em.createQuery(jpql, Long.class)
                .setParameter("roomId", roomId)
                .setParameter("startTime", startTime)
                .setParameter("endTime", endTime)
                .setParameter("excludeId", excludeBookingId)
                .getSingleResult();

        return cnt != null && cnt > 0;
    }
}
