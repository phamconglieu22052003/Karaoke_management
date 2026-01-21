package com.karaoke_management.service;

import com.karaoke_management.entity.Room;
import java.util.List;

public interface RoomService {
    List<Room> findAll();
    Room findById(Long id);

    Room save(Room room);
    void deleteById(Long id);
}
