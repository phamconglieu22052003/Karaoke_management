package com.karaoke_management.service.impl;

import com.karaoke_management.entity.Room;
import com.karaoke_management.repository.RoomRepository;
import com.karaoke_management.service.RoomService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
        validateRoom(room);

        String normalizedName = room.getName().trim();

        // check trùng tên theo case-insensitive
        if (room.getId() == null) {
            if (roomRepository.existsByNameIgnoreCase(normalizedName)) {
                throw new IllegalArgumentException("Tên phòng đã tồn tại. Vui lòng chọn tên khác.");
            }
        } else {
            if (roomRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, room.getId())) {
                throw new IllegalArgumentException("Tên phòng đã tồn tại. Vui lòng chọn tên khác.");
            }
        }

        room.setName(normalizedName);
        return roomRepository.save(room);
    }

    @Override
    public void deleteById(Long id) {
        roomRepository.deleteById(id);
    }

    private void validateRoom(Room room) {
        if (room == null) throw new IllegalArgumentException("Dữ liệu phòng không hợp lệ.");

        if (!StringUtils.hasText(room.getName())) {
            throw new IllegalArgumentException("Vui lòng nhập tên phòng.");
        }
        if (room.getCapacity() == null || room.getCapacity() <= 0) {
            throw new IllegalArgumentException("Sức chứa phải > 0.");
        }
        if (room.getStatus() == null) {
            throw new IllegalArgumentException("Vui lòng chọn trạng thái.");
        }
        if (room.getRoomType() == null || room.getRoomType().getId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn loại phòng.");
        }
    }
}
