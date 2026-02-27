package com.karaoke_management.service;

import com.karaoke_management.entity.RoomSession;
import com.karaoke_management.entity.RoomSessionStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface RoomSessionService {
    RoomSession checkIn(Long roomId, String createdBy);
    RoomSession checkOut(Long sessionId, String checkedOutBy);
    List<RoomSession> findAll();

    List<RoomSession> filter(LocalDateTime from, LocalDateTime to, RoomSessionStatus status, Long roomId);
}
