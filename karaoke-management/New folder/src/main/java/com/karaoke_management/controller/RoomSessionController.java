package com.karaoke_management.controller;

import com.karaoke_management.service.RoomSessionService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/room-sessions")
public class RoomSessionController {

    private final RoomSessionService roomSessionService;

    public RoomSessionController(RoomSessionService roomSessionService) {
        this.roomSessionService = roomSessionService;
    }

    // Danh sách session
    @GetMapping
    public String list(Model model) {
        model.addAttribute("sessions", roomSessionService.findAll());
        return "sessions/session-list";
    }

    // Mở phòng (tạo session)
    @PostMapping("/open")
    public String open(@RequestParam("roomId") Long roomId, Authentication auth) {
        String user = (auth != null) ? auth.getName() : "system";
        roomSessionService.checkIn(roomId, user);
        return "redirect:/rooms";
    }

    // Đóng phòng (tính giờ + tiền)
    @PostMapping("/close/{id}")
    public String close(@PathVariable("id") Long sessionId, Authentication auth) {
        String user = (auth != null) ? auth.getName() : "system";
        roomSessionService.checkOut(sessionId, user);
        return "redirect:/room-sessions";
    }
}
