package com.karaoke_management.service;

import com.karaoke_management.entity.RoomType;

import java.util.List;

public interface RoomTypeService {
    List<RoomType> findAll();
    RoomType save(RoomType roomType);
}
