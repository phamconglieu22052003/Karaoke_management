package com.karaoke_management.service.impl;

import com.karaoke_management.entity.*;
import com.karaoke_management.repository.RoomRepository;
import com.karaoke_management.repository.RoomSessionRepository;
import com.karaoke_management.service.RoomSessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RoomSessionServiceImpl implements RoomSessionService {

    private final RoomSessionRepository roomSessionRepository;
    private final RoomRepository roomRepository;

    public RoomSessionServiceImpl(RoomSessionRepository roomSessionRepository, RoomRepository roomRepository) {
        this.roomSessionRepository = roomSessionRepository;
        this.roomRepository = roomRepository;
    }

    @Override
    @Transactional
    public RoomSession checkIn(Long roomId, String createdBy) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        // chặn mở phòng nếu đang có session OPEN
        boolean alreadyOpen = roomSessionRepository.existsByRoom_IdAndStatus(roomId, RoomSessionStatus.OPEN);
        if (alreadyOpen) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phòng đang được mở (OPEN) rồi");
        }

        // cập nhật trạng thái phòng
        room.setStatus(RoomStatus.OCCUPIED);
        roomRepository.save(room);

        RoomSession s = new RoomSession();
        s.setRoom(room);
        s.setStartTime(LocalDateTime.now());
        s.setStatus(RoomSessionStatus.OPEN);
        s.setCreatedBy(createdBy);

        return roomSessionRepository.save(s);
    }

    @Override
    @Transactional
    public RoomSession checkOut(Long sessionId, String checkedOutBy) {
        RoomSession s = roomSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (s.getStatus() == RoomSessionStatus.CLOSED) {
            return s; // đóng rồi thì thôi
        }

        LocalDateTime end = LocalDateTime.now();
        s.setEndTime(end);
        s.setStatus(RoomSessionStatus.CLOSED);
        s.setCheckedOutBy(checkedOutBy);

        // tính phút
        LocalDateTime start = s.getStartTime();
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes < 0) minutes = 0;

        s.setTotalMinutes((int) minutes);

        // tính tiền theo giá/giờ (RoomType.pricePerHour)
        BigDecimal pricePerHour = BigDecimal.ZERO;
        Room room = s.getRoom();
        if (room != null && room.getRoomType() != null && room.getRoomType().getPricePerHour() != null) {
            pricePerHour = room.getRoomType().getPricePerHour();
        }

        // total = pricePerHour * (minutes / 60)
        BigDecimal hours = BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);

        BigDecimal total = pricePerHour.multiply(hours).setScale(2, RoundingMode.HALF_UP);
        s.setTotalAmount(total);

        // trả phòng -> AVAILABLE
        if (room != null) {
            room.setStatus(RoomStatus.AVAILABLE);
            roomRepository.save(room);
        }

        return roomSessionRepository.save(s);
    }

    @Override
    public List<RoomSession> findAll() {
        return roomSessionRepository.findAllByOrderByStartTimeDesc();
    }
}
