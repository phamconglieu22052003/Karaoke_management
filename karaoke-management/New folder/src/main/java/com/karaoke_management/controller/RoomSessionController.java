package com.karaoke_management.controller;

import com.karaoke_management.entity.Room;
import com.karaoke_management.entity.RoomStatus;
import com.karaoke_management.service.RoomService;
import com.karaoke_management.service.RoomSessionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/room-sessions")
public class RoomSessionController {

    private final RoomSessionService roomSessionService;
    private final RoomService roomService;

    public RoomSessionController(RoomSessionService roomSessionService, RoomService roomService) {
        this.roomSessionService = roomSessionService;
        this.roomService = roomService;
    }

    // Trang danh sách session + list phòng để check-in
    @GetMapping
    public String list(Model model) {
        model.addAttribute("sessions", roomSessionService.findAll());
        model.addAttribute("activeSessions", roomSessionService.findActive());
        model.addAttribute("rooms", roomService.findAll());
        model.addAttribute("RoomStatus", RoomStatus.class); // dùng trong thymeleaf nếu cần
        return "sessions/session-list";
    }

    // Check-in theo roomId
    @PostMapping("/checkin")
    public String checkIn(@RequestParam("roomId") Long roomId,
                          RedirectAttributes ra) {
        try {
            roomSessionService.checkIn(roomId);
            ra.addFlashAttribute("success", "Check-in thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/room-sessions";
    }

    // Check-out theo sessionId
    @PostMapping("/{id}/checkout")
    public String checkOut(@PathVariable("id") Long sessionId,
                           RedirectAttributes ra) {
        try {
            roomSessionService.checkOut(sessionId);
            ra.addFlashAttribute("success", "Check-out thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/room-sessions";
    }
}
