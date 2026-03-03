package com.karaoke_management.controller;

import com.karaoke_management.entity.RoomSessionStatus;
import com.karaoke_management.repository.RoomRepository;
import com.karaoke_management.service.RoomSessionService;
import com.karaoke_management.util.FilterUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;

@Controller
@RequestMapping("/room-sessions")
public class RoomSessionController {

    private final RoomSessionService roomSessionService;
    private final RoomRepository roomRepository;

    private static final DateTimeFormatter VN_DTF = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DateTimeFormatter VN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public RoomSessionController(RoomSessionService roomSessionService, RoomRepository roomRepository) {
        this.roomSessionService = roomSessionService;
        this.roomRepository = roomRepository;
    }

    // Danh sách session
    @GetMapping
    public String list(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) RoomSessionStatus status,
            @RequestParam(required = false) Long roomId,
            Model model
    ) {
        LocalDateTime fromDt = FilterUtils.parseFlexibleDateOrDateTimeOrNull(from, true);
        LocalDateTime toDt = FilterUtils.parseFlexibleDateOrDateTimeOrNull(to, false);

        // Default 30 ngày gần nhất nếu bỏ trống cả 2
        LocalDateTime defaultFrom = LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime defaultTo = LocalDateTime.now();
        var range = FilterUtils.normalizeDateTimeRange(fromDt, toDt, defaultFrom, defaultTo);

        model.addAttribute("sessions", roomSessionService.filter(range.from(), range.to(), status, roomId));
        model.addAttribute("from", from == null ? "" : from);
        model.addAttribute("to", to == null ? "" : to);
        model.addAttribute("status", status);
        model.addAttribute("roomId", roomId);
        model.addAttribute("rooms", roomRepository.findAll());
        model.addAttribute("dtPattern", "HH:mm dd/MM/yyyy");
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

        // ✅ Theo UC-SESSION-02 (Đóng phòng) có include UC-INVOICE-01 (Tạo/Xem hóa đơn):
        // Đóng phòng xong thì chuyển luôn sang tạo/xem hóa đơn của session.
        return "redirect:/invoice/create/" + sessionId;
    }

    // parse/filter helpers moved to FilterUtils
}
