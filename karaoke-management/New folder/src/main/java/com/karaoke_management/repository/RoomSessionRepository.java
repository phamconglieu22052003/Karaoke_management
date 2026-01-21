package com.karaoke_management.repository;

import com.karaoke_management.entity.RoomSession;
import com.karaoke_management.entity.RoomSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomSessionRepository extends JpaRepository<RoomSession, Long> {

    boolean existsByRoom_IdAndStatus(Long roomId, RoomSessionStatus status);

    List<RoomSession> findAllByStatusOrderByStartTimeDesc(RoomSessionStatus status);

    List<RoomSession> findAllByOrderByStartTimeDesc();
}
