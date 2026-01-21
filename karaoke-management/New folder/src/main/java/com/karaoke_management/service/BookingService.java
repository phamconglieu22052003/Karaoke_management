package com.karaoke_management.service;

import com.karaoke_management.entity.Booking;

import java.util.List;
import java.util.Optional;

public interface BookingService {
    List<Booking> findAll();
    Optional<Booking> findById(Long id);
    Booking save(Booking booking);
    void deleteById(Long id);
}
