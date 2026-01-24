package com.karaoke_management.service.impl;

import com.karaoke_management.entity.Room;
import com.karaoke_management.repository.RoomRepository;
import com.karaoke_management.service.RoomService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;

    public RoomServiceImpl(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override
    public List<Room> findAll() {
        return roomRepository.findAll();
    }

    @Override
    public Room findById(Long id) {
        return roomRepository.findById(id).orElseThrow();
    }

    @Override
    public Room save(Room room) {          
        return roomRepository.save(room);
    }

    @Override
    public void deleteById(Long id) {  
        roomRepository.deleteById(id);
    }
    
}
