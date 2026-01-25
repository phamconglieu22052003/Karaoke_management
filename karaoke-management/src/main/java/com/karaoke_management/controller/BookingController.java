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
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/booking")
public class BookingController {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;

    public BookingController(BookingRepository bookingRepository,
                             RoomRepository roomRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
    }

    // ================= LIST =================
    @GetMapping
    public String list(Model model) {
        model.addAttribute("bookings", bookingRepository.findAll());
        return "booking/booking-list";
    }

    // ================= NEW FORM =================
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("booking", new Booking());
        model.addAttribute("rooms", roomRepository.findAll());
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
        return "booking/booking-form";
    }

    // ================= SAVE (CREATE) =================
    @PostMapping("/save")
    @Transactional
    public String save(
            @RequestParam String customerName,
            // form cũ đang gửi name="phone"; form mới gửi name="customerPhone"
            @RequestParam(required = false) String customerPhone,
            @RequestParam(required = false, name = "phone") String phone,
            @RequestParam Long roomId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime endTime,
            @RequestParam BookingStatus status,
            Model model
    ) {
        // validate
        if (customerName == null || customerName.isBlank()) {
            return backToFormWithError("Vui lòng nhập tên khách", model, new Booking());
        }

        String resolvedPhone = (customerPhone != null && !customerPhone.isBlank())
                ? customerPhone
                : (phone != null ? phone : "");

        if (resolvedPhone.isBlank()) {
            return backToFormWithError("Vui lòng nhập số điện thoại", model, new Booking());
        }

        if (endTime.isBefore(startTime) || endTime.equals(startTime)) {
            return backToFormWithError("Giờ kết thúc phải sau giờ bắt đầu", model, new Booking());
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        // ====== CHỐNG TRÙNG GIỜ ======
        boolean overlapped = bookingRepository.existsOverlap(
                roomId, null, startTime, endTime, BookingStatus.CANCELLED
        );
        if (overlapped) {
            return backToFormWithError(
                    "Phòng đã có người đặt trong khung giờ này",
                    model,
                    new Booking()
            );
        }

        Booking booking = new Booking();
        booking.setCustomerName(customerName.trim());
        booking.setCustomerPhone(resolvedPhone.trim());
        // giữ tương thích với cột phone cũ trong DB
        booking.setPhone(resolvedPhone.trim());
        booking.setRoom(room);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
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
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime endTime,
            @RequestParam BookingStatus status,
            Model model
    ) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (customerName == null || customerName.isBlank()) {
            return backToFormWithError("Vui lòng nhập tên khách", model, booking);
        }

        String resolvedPhone = (customerPhone != null && !customerPhone.isBlank())
                ? customerPhone
                : (phone != null ? phone : "");

        if (resolvedPhone.isBlank()) {
            return backToFormWithError("Vui lòng nhập số điện thoại", model, booking);
        }

        if (endTime.isBefore(startTime) || endTime.equals(startTime)) {
            return backToFormWithError("Giờ kết thúc phải sau giờ bắt đầu", model, booking);
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        // ====== CHỐNG TRÙNG GIỜ (loại trừ chính nó) ======
        boolean overlapped = bookingRepository.existsOverlap(
                roomId, id, startTime, endTime, BookingStatus.CANCELLED
        );
        if (overlapped) {
            return backToFormWithError(
                    "Phòng đã có người đặt trong khung giờ này",
                    model,
                    booking
            );
        }

        booking.setCustomerName(customerName.trim());
        booking.setCustomerPhone(resolvedPhone.trim());
        booking.setPhone(resolvedPhone.trim());
        booking.setRoom(room);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setStatus(status);

        bookingRepository.save(booking);
        return "redirect:/booking";
    }

    // ================= DELETE (giữ đúng link trong booking-list.html) =================
    @PostMapping("/delete/{id}")
    @Transactional
    public String delete(@PathVariable Long id) {
        if (!bookingRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
        }
        bookingRepository.deleteById(id);
        return "redirect:/booking";
    }

    // ================= HELPER =================
    private String backToFormWithError(String error, Model model, Booking booking) {
        model.addAttribute("error", error);
        model.addAttribute("booking", booking);
        model.addAttribute("rooms", roomRepository.findAll());
        return "booking/booking-form";
    }
}
