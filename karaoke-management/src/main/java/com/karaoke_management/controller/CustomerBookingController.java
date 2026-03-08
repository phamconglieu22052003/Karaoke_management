package com.karaoke_management.controller;

import com.karaoke_management.entity.Booking;
import com.karaoke_management.entity.BookingStatus;
import com.karaoke_management.entity.Room;
import com.karaoke_management.repository.BookingRepository;
import com.karaoke_management.repository.RoomRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.Map;

/**
 * Public (anonymous) booking flow for "Khách hàng".
 * - GET  /customer/booking/new : form đặt phòng online
 * - POST /customer/booking     : submit
 * - GET  /customer/booking/{id}?phone=... : tra cứu booking theo mã + SĐT
 */
@Controller
@RequestMapping("/customer/booking")
public class CustomerBookingController {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;

    private static final DateTimeFormatter VN_DTF = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DateTimeFormatter HTML_DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    public CustomerBookingController(BookingRepository bookingRepository, RoomRepository roomRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(name = "success", required = false) Long successId, Model model) {
        model.addAttribute("rooms", roomRepository.findAll());
        if (successId != null) {
            bookingRepository.findById(successId).ifPresent(booking -> model.addAttribute("successBooking", booking));
        }
        populateFormState(model, "", "", null, null, null);
        return "customer/booking-new";
    }

    @PostMapping
    @Transactional
    public String create(
            @RequestParam String customerName,
            @RequestParam String phone,
            @RequestParam Long roomId,
            @RequestParam String startTime,
            @RequestParam String endTime,
            Model model
    ) {
        if (customerName == null || customerName.isBlank()) {
            return backWithError("Vui lòng nhập họ tên", model, customerName, phone, roomId, startTime, endTime);
        }
        if (phone == null || phone.isBlank()) {
            return backWithError("Vui lòng nhập số điện thoại", model, customerName, phone, roomId, startTime, endTime);
        }

        LocalDateTime startDt = parseCustomerDateTimeOrNull(startTime);
        LocalDateTime endDt = parseCustomerDateTimeOrNull(endTime);
        if (startDt == null || endDt == null) {
            return backWithError("Vui lòng chọn ngày giờ hợp lệ", model,
                    customerName, phone, roomId, startTime, endTime);
        }
        if (startDt.isBefore(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))) {
            return backWithError("Không thể chọn giờ bắt đầu trong quá khứ", model,
                    customerName, phone, roomId, startTime, endTime);
        }
        if (!endDt.isAfter(startDt)) {
            return backWithError("Giờ kết thúc phải sau giờ bắt đầu", model, customerName, phone, roomId, startTime, endTime);
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        boolean overlapped = bookingRepository.existsOverlapExcludingStatus(
                roomId, null, startDt, endDt, BookingStatus.CANCELLED
        );
        if (overlapped) {
            return backWithError("Khung giờ này đã có người đặt phòng. Vui lòng chọn giờ khác.", model,
                    customerName, phone, roomId, startTime, endTime);
        }

        Booking b = new Booking();
        b.setCustomerName(customerName.trim());
        b.setCustomerPhone(phone.trim());
        b.setPhone(phone.trim());
        b.setRoom(room);
        b.setStartTime(startDt);
        b.setEndTime(endDt);
        b.setStatus(BookingStatus.BOOKED);

        bookingRepository.save(b);
        return "redirect:/customer/booking/new?success=" + b.getId();
    }

    @GetMapping("/created/{id}")
    public String created(@PathVariable Long id, Model model) {
        Booking b = bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        model.addAttribute("booking", b);
        model.addAttribute("dt", VN_DTF);
        return "customer/booking-created";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, @RequestParam String phone, Model model) {
        if (phone == null || phone.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing phone");
        }
        Optional<Booking> opt = bookingRepository.findById(id);
        if (opt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
        }
        Booking b = opt.get();
        String p = (b.getPhone() == null || b.getPhone().isBlank()) ? b.getCustomerPhone() : b.getPhone();
        if (p == null || !p.trim().equals(phone.trim())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Phone does not match");
        }
        model.addAttribute("booking", b);
        model.addAttribute("dt", VN_DTF);
        return "customer/booking-detail";
    }

    private String backWithError(String error, Model model, String customerName, String phone, Long roomId,
                                 String startTime, String endTime) {
        model.addAttribute("error", error);
        model.addAttribute("rooms", roomRepository.findAll());
        populateFormState(model, customerName, phone, roomId, startTime, endTime);
        return "customer/booking-new";
    }

    private void populateFormState(Model model, String customerName, String phone, Long roomId,
                                   String startTime, String endTime) {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime suggestedStart = now.plusMinutes(30 - (now.getMinute() % 30 == 0 ? 0 : now.getMinute() % 30));
        if (suggestedStart.isBefore(now)) {
            suggestedStart = now.plusMinutes(30);
        }
        LocalDateTime suggestedEnd = suggestedStart.plusHours(2);

        String resolvedStartTime = (startTime == null || startTime.isBlank()) ? HTML_DTF.format(suggestedStart) : startTime;
        String resolvedEndTime = (endTime == null || endTime.isBlank()) ? HTML_DTF.format(suggestedEnd) : endTime;
        Map<String, String> startParts = splitDateTimeValue(resolvedStartTime, suggestedStart);
        Map<String, String> endParts = splitDateTimeValue(resolvedEndTime, suggestedEnd);

        model.addAttribute("customerName", customerName == null ? "" : customerName);
        model.addAttribute("phone", phone == null ? "" : phone);
        model.addAttribute("roomId", roomId);
        model.addAttribute("startTime", resolvedStartTime);
        model.addAttribute("endTime", resolvedEndTime);
        model.addAttribute("startDate", startParts.get("date"));
        model.addAttribute("startClock", startParts.get("time"));
        model.addAttribute("endDate", endParts.get("date"));
        model.addAttribute("endClock", endParts.get("time"));
        model.addAttribute("minDateTime", HTML_DTF.format(now));
        model.addAttribute("minDate", now.toLocalDate().toString());
        model.addAttribute("minTimeToday", now.toLocalTime().toString().substring(0, 5));
    }

    private LocalDateTime parseCustomerDateTimeOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDateTime.parse(s.trim(), HTML_DTF);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(s.trim(), VN_DTF);
            } catch (DateTimeParseException ex) {
                return null;
            }
        }
    }

    private Map<String, String> splitDateTimeValue(String rawValue, LocalDateTime fallback) {
        LocalDateTime parsed = parseCustomerDateTimeOrNull(rawValue);
        if (parsed == null) {
            parsed = fallback;
        }
        return Map.of(
                "date", parsed.toLocalDate().toString(),
                "time", parsed.toLocalTime().toString().substring(0, 5)
        );
    }
}
