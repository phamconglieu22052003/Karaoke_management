package com.karaoke_management.service;

import com.karaoke_management.entity.RoomSession;
import java.util.List;

public interface RoomSessionService {
    List<RoomSession> findAll();
    List<RoomSession> findActive();

    RoomSession checkIn(Long roomId);
    RoomSession checkOut(Long sessionId);
}
