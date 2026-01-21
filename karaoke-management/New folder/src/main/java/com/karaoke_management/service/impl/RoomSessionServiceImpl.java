package com.karaoke_management.service.impl;

import com.karaoke_management.entity.*;
import com.karaoke_management.repository.RoomRepository;
import com.karaoke_management.repository.RoomSessionRepository;
import com.karaoke_management.service.RoomSessionService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RoomSessionServiceImpl implements RoomSessionService {

    private final RoomSessionRepository roomSessionRepository;
    private final RoomRepository roomRepository;

    public RoomSessionServiceImpl(RoomSessionRepository roomSessionRepository,
                                  RoomRepository roomRepository) {
        this.roomSessionRepository = roomSessionRepository;
        this.roomRepository = roomRepository;
    }

    @Override
    public List<RoomSession> findAll() {
        return roomSessionRepository.findAllByOrderByStartTimeDesc();
    }

    @Override
    public List<RoomSession> findActive() {
        return roomSessionRepository
                .findAllByStatusOrderByStartTimeDesc(RoomSessionStatus.ACTIVE);
    }

    @Override
    @Transactional
    public RoomSession checkIn(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng"));

        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw new IllegalStateException("Phòng không trống");
        }

        if (roomSessionRepository.existsByRoom_IdAndStatus(
                roomId, RoomSessionStatus.ACTIVE)) {
            throw new IllegalStateException("Phòng đã có session đang hoạt động");
        }

        RoomSession session = new RoomSession();
        session.setRoom(room);
        session.setStartTime(LocalDateTime.now());
        session.setStatus(RoomSessionStatus.ACTIVE);
        session.setCreatedBy(getCurrentUsername());

        room.setStatus(RoomStatus.OCCUPIED);

        roomRepository.save(room);
        return roomSessionRepository.save(session);
    }

    @Override
    @Transactional
    public RoomSession checkOut(Long sessionId) {
        RoomSession session = roomSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy session"));

        if (session.getStatus() != RoomSessionStatus.ACTIVE) {
            throw new IllegalStateException("Session đã đóng");
        }

        session.setEndTime(LocalDateTime.now());
        session.setStatus(RoomSessionStatus.CLOSED);
        session.setCheckedOutBy(getCurrentUsername());

        Room room = session.getRoom();
        room.setStatus(RoomStatus.AVAILABLE);

        roomRepository.save(room);
        return roomSessionRepository.save(session);
    }

    private String getCurrentUsername() {
        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();
        return (auth == null) ? "system" : auth.getName();
    }
}
