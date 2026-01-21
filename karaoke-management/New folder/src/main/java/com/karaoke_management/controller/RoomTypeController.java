package com.karaoke_management.controller;

import com.karaoke_management.entity.RoomType;
import com.karaoke_management.service.RoomTypeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/room-types")
public class RoomTypeController {

    private final RoomTypeService roomTypeService;

    public RoomTypeController(RoomTypeService roomTypeService) {
        this.roomTypeService = roomTypeService;
    }

    // Danh sách
    @GetMapping
    public String list(Model model) {
        model.addAttribute("roomTypes", roomTypeService.findAll());
        return "roomtype/room-types";
    }

    // Form thêm mới
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("roomType", new RoomType());
        return "roomtype/room-type-form";
    }

    // Submit thêm mới
    @PostMapping
    public String create(@ModelAttribute RoomType roomType) {
        roomTypeService.save(roomType);
        return "redirect:/room-types";
    }
}
