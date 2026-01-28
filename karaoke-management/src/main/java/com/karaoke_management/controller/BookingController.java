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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Controller
@RequestMapping("/booking")
public class BookingController {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;

    // ✅ format VN
    private static final DateTimeFormatter VN_DTF = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    public BookingController(BookingRepository bookingRepository,
                             RoomRepository roomRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
    }

    // ================= LIST (+ FILTER) =================
    // /booking?from=26/01/2026 10:00&to=26/01/2026 22:00
    @GetMapping
    public String list(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            Model model
    ) {
        LocalDateTime fromDt = parseVnDateTimeOrNull(from);
        LocalDateTime toDt = parseVnDateTimeOrNull(to);

        List<Booking> bookings;
        if (fromDt == null && toDt == null) {
            bookings = bookingRepository.findAll();
        } else {
            bookings = bookingRepository.filterByTimeRange(fromDt, toDt);
        }

        model.addAttribute("bookings", bookings);

        // ✅ giữ chuỗi VN để đổ lại input (type=text)
        model.addAttribute("from", from == null ? "" : from);
        model.addAttribute("to", to == null ? "" : to);

        // để hiển thị hint format
        model.addAttribute("dtPattern", "HH:mm dd/MM/yyyy");

        return "booking/booking-list";
    }

    // ================= NEW FORM =================
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("booking", new Booking());
        model.addAttribute("rooms", roomRepository.findAll());
        model.addAttribute("dtPattern", "HH:mm dd/MM/yyyy");
        return "booking/booking-form";
    }

    // ================= EDIT FORM =================
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        model.addAttribute("booking", booking);
        model.addAttribute("rooms", roomRepository.findAll());

        // ✅ đưa ra chuỗi VN cho input text
        model.addAttribute("startTimeStr", booking.getStartTime() == null ? "" : VN_DTF.format(booking.getStartTime()));
        model.addAttribute("endTimeStr", booking.getEndTime() == null ? "" : VN_DTF.format(booking.getEndTime()));
        model.addAttribute("dtPattern", "HH:mm dd/MM/yyyy");

        return "booking/booking-form";
    }

    // ================= SAVE (CREATE) =================
    @PostMapping("/save")
    @Transactional
    public String save(
            @RequestParam String customerName,
            @RequestParam(required = false) String customerPhone,
            @RequestParam(required = false, name = "phone") String phone,
            @RequestParam Long roomId,
            @RequestParam String startTime,   // ✅ nhận chuỗi VN
            @RequestParam String endTime,     // ✅ nhận chuỗi VN
            @RequestParam BookingStatus status,
            Model model
    ) {
        Booking formBooking = new Booking();

        if (customerName == null || customerName.isBlank()) {
            return backToFormWithError("Vui lòng nhập tên khách", model, formBooking, startTime, endTime);
        }

        String resolvedPhone = resolvePhone(customerPhone, phone);
        if (resolvedPhone.isBlank()) {
            return backToFormWithError("Vui lòng nhập số điện thoại", model, formBooking, startTime, endTime);
        }

        LocalDateTime startDt = parseVnDateTimeOrNull(startTime);
        LocalDateTime endDt = parseVnDateTimeOrNull(endTime);
        if (startDt == null || endDt == null) {
            return backToFormWithError("Sai định dạng thời gian. Đúng: HH:mm dd/MM/yyyy (VD: 26/01/2026 20:30)", model, formBooking, startTime, endTime);
        }
        if (!endDt.isAfter(startDt)) {
            return backToFormWithError("Giờ kết thúc phải sau giờ bắt đầu", model, formBooking, startTime, endTime);
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        boolean overlapped = bookingRepository.existsOverlapExcludingStatus(
                roomId, null, startDt, endDt, BookingStatus.CANCELLED
        );
        if (overlapped) {
            formBooking.setCustomerName(customerName);
            formBooking.setCustomerPhone(resolvedPhone);
            formBooking.setPhone(resolvedPhone);
            formBooking.setRoom(room);
            formBooking.setStatus(status);
            return backToFormWithError("Phòng đã có người đặt trong khung giờ này", model, formBooking, startTime, endTime);
        }

        Booking booking = new Booking();
        booking.setCustomerName(customerName.trim());
        booking.setCustomerPhone(resolvedPhone.trim());
        booking.setPhone(resolvedPhone.trim());
        booking.setRoom(room);
        booking.setStartTime(startDt);
        booking.setEndTime(endDt);
        booking.setStatus(status);

        bookingRepository.save(booking);
        return "redirect:/booking";
    }

    // ================= UPDATE =================
    @PostMapping("/update")
    @Transactional
    public String update(
            @RequestParam Long id,
            @RequestParam String customerName,
            @RequestParam(required = false) String customerPhone,
            @RequestParam(required = false, name = "phone") String phone,
            @RequestParam Long roomId,
            @RequestParam String startTime, // ✅ chuỗi VN
            @RequestParam String endTime,   // ✅ chuỗi VN
            @RequestParam BookingStatus status,
            Model model
    ) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (customerName == null || customerName.isBlank()) {
            return backToFormWithError("Vui lòng nhập tên khách", model, booking, startTime, endTime);
        }

        String resolvedPhone = resolvePhone(customerPhone, phone);
        if (resolvedPhone.isBlank()) {
            return backToFormWithError("Vui lòng nhập số điện thoại", model, booking, startTime, endTime);
        }

        LocalDateTime startDt = parseVnDateTimeOrNull(startTime);
        LocalDateTime endDt = parseVnDateTimeOrNull(endTime);
        if (startDt == null || endDt == null) {
            return backToFormWithError("Sai định dạng thời gian. Đúng: HH:mm dd/MM/yyyy (VD: 26/01/2026 20:30)", model, booking, startTime, endTime);
        }
        if (!endDt.isAfter(startDt)) {
            return backToFormWithError("Giờ kết thúc phải sau giờ bắt đầu", model, booking, startTime, endTime);
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        boolean overlapped = bookingRepository.existsOverlapExcludingStatus(
                roomId, id, startDt, endDt, BookingStatus.CANCELLED
        );
        if (overlapped) {
            booking.setCustomerName(customerName);
            booking.setCustomerPhone(resolvedPhone);
            booking.setPhone(resolvedPhone);
            booking.setRoom(room);
            booking.setStatus(status);
            return backToFormWithError("Phòng đã có người đặt trong khung giờ này", model, booking, startTime, endTime);
        }

        booking.setCustomerName(customerName.trim());
        booking.setCustomerPhone(resolvedPhone.trim());
        booking.setPhone(resolvedPhone.trim());
        booking.setRoom(room);
        booking.setStartTime(startDt);
        booking.setEndTime(endDt);
        booking.setStatus(status);

        bookingRepository.save(booking);
        return "redirect:/booking";
    }

    // ================= DELETE =================
    @PostMapping("/delete/{id}")
    @Transactional
    public String delete(@PathVariable Long id) {
        if (!bookingRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
        }
        bookingRepository.deleteById(id);
        return "redirect:/booking";
    }

    // ================= HELPERS =================
    private LocalDateTime parseVnDateTimeOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDateTime.parse(s.trim(), VN_DTF);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String resolvePhone(String customerPhone, String phone) {
        String resolved = (customerPhone != null && !customerPhone.isBlank())
                ? customerPhone
                : (phone != null ? phone : "");
        return resolved == null ? "" : resolved.trim();
    }

    private String backToFormWithError(String error, Model model, Booking booking, String startTimeStr, String endTimeStr) {
        model.addAttribute("error", error);
        model.addAttribute("booking", booking);
        model.addAttribute("rooms", roomRepository.findAll());
        model.addAttribute("startTimeStr", startTimeStr == null ? "" : startTimeStr);
        model.addAttribute("endTimeStr", endTimeStr == null ? "" : endTimeStr);
        model.addAttribute("dtPattern", "HH:mm dd/MM/yyyy");
        return "booking/booking-form";
    }
}
