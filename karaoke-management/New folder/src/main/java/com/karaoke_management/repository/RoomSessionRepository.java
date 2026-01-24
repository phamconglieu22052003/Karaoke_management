package com.karaoke_management.repository;

import com.karaoke_management.entity.RoomSession;
import com.karaoke_management.entity.RoomSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomSessionRepository extends JpaRepository<RoomSession, Long> {

    // lấy session mới nhất lên trước
    List<RoomSession> findAllByOrderByStartTimeDesc();

    // dùng để chặn mở phòng trùng
    boolean existsByRoom_IdAndStatus(Long roomId, RoomSessionStatus status);
}
