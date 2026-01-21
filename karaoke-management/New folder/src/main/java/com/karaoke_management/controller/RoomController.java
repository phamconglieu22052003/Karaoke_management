package com.karaoke_management.controller;

import com.karaoke_management.entity.Room;
import com.karaoke_management.entity.RoomStatus;
import com.karaoke_management.service.RoomService;
import com.karaoke_management.service.RoomTypeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Controller
public class RoomController {

    private final RoomService roomService;
    private final RoomTypeService roomTypeService;

    public RoomController(RoomService roomService, RoomTypeService roomTypeService) {
        this.roomService = roomService;
        this.roomTypeService = roomTypeService;
    }

    // Danh sách phòng
    @GetMapping("/rooms")
    public String rooms(Model model) {
        model.addAttribute("rooms", roomService.findAll());
        return "room/rooms";
    }

    // Form thêm phòng
    @GetMapping("/rooms/new")
    public String createRoomForm(Model model) {
        model.addAttribute("room", new Room());
        model.addAttribute("roomTypes", roomTypeService.findAll());
        model.addAttribute("statuses", RoomStatus.values());
        return "room/room-form";
    }

    // Lưu phòng mới
    @PostMapping("/rooms")
    public String saveRoom(@ModelAttribute Room room) {
        roomService.save(room);
        return "redirect:/rooms";
    }

    // Form sửa phòng
    @GetMapping("/rooms/edit/{id}")
    public String editRoomForm(@PathVariable Long id, Model model) {
        Room room = roomService.findById(id);
        model.addAttribute("room", room);
        model.addAttribute("roomTypes", roomTypeService.findAll());
        model.addAttribute("statuses", RoomStatus.values());
        return "room/room-form";
    }

    // Cập nhật phòng
    @PostMapping("/rooms/update")
    public String updateRoom(@ModelAttribute Room room) {
        roomService.save(room);
        return "redirect:/rooms";
    }

    // Xóa phòng
    @PostMapping("/rooms/delete/{id}")
    public String deleteRoom(@PathVariable Long id) {
        roomService.deleteById(id);
        return "redirect:/rooms";
    }
}
