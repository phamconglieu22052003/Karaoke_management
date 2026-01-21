package com.karaoke_management.service;

import com.karaoke_management.entity.Booking;

import java.util.List;

public interface BookingService {
    List<Booking> findAll();
    Booking findById(Long id);
    Booking save(Booking booking);
    void deleteById(Long id);
}
