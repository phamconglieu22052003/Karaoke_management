package com.karaoke_management.controller;

import com.karaoke_management.entity.Room;
import com.karaoke_management.entity.RoomStatus;
import com.karaoke_management.repository.RoomRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Technician module ("Kỹ thuật"):
 * - View room status
 * - Mark room as MAINTENANCE / AVAILABLE
 */
@Controller
@RequestMapping("/tech")
public class TechController {

    private final RoomRepository roomRepository;

    public TechController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @GetMapping("/rooms")
    public String rooms(Model model) {
        List<Room> rooms = roomRepository.findAll();
        model.addAttribute("rooms", rooms);
        return "tech/rooms";
    }

    @PostMapping("/rooms/{id}/maintenance")
    @Transactional
    public String setMaintenance(@PathVariable Long id) {
        Room r = roomRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        r.setStatus(RoomStatus.MAINTENANCE);
        roomRepository.save(r);
        return "redirect:/tech/rooms";
    }

    @PostMapping("/rooms/{id}/available")
    @Transactional
    public String setAvailable(@PathVariable Long id) {
        Room r = roomRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        r.setStatus(RoomStatus.AVAILABLE);
        roomRepository.save(r);
        return "redirect:/tech/rooms";
    }
}
