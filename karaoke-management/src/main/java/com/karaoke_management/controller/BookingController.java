package com.karaoke_management.controller;

import com.karaoke_management.entity.Booking;
import com.karaoke_management.entity.BookingStatus;
import com.karaoke_management.entity.Room;
import com.karaoke_management.service.BookingService;
import com.karaoke_management.service.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/booking")
public class BookingController {

    private final BookingService bookingService;
    private final RoomService roomService;

    public BookingController(BookingService bookingService, RoomService roomService) {
        this.bookingService = bookingService;
        this.roomService = roomService;
    }

    // Danh sách booking
    @GetMapping
    public String list(Model model) {
        model.addAttribute("bookings", bookingService.findAll());
        return "booking/booking-list";
    }

    // Form tạo mới
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("booking", new Booking());
        model.addAttribute("rooms", roomService.findAll());
        model.addAttribute("statuses", BookingStatus.values());
        return "booking/booking-form";
    }

    // Lưu tạo mới
    @PostMapping("/save")
    public String save(@RequestParam("roomId") Long roomId,
                       @ModelAttribute Booking booking,
                       Model model) {
        try {
            Room room = roomService.findById(roomId);
            if (room == null) throw new IllegalArgumentException("Phòng không tồn tại");

            booking.setRoom(room);
            bookingService.save(booking);
            return "redirect:/booking";

        } catch (IllegalArgumentException ex) {
            // hiển thị lỗi đẹp
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("booking", booking);
            model.addAttribute("rooms", roomService.findAll());
            model.addAttribute("statuses", BookingStatus.values());
            return "booking/booking-form";
        }
    }

    // Form sửa
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Booking booking = bookingService.findById(id);
        if (booking == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
        }

        model.addAttribute("booking", booking);
        model.addAttribute("rooms", roomService.findAll());
        model.addAttribute("statuses", BookingStatus.values());
        return "booking/booking-form";
    }

    // Lưu sửa
    @PostMapping("/update")
    public String update(@RequestParam("roomId") Long roomId,
                         @ModelAttribute Booking booking,
                         Model model) {
        try {
            // đảm bảo booking tồn tại
            Booking old = bookingService.findById(booking.getId());
            if (old == null) {
                throw new IllegalArgumentException("Booking không tồn tại");
            }

            Room room = roomService.findById(roomId);
            if (room == null) throw new IllegalArgumentException("Phòng không tồn tại");

            booking.setRoom(room);
            bookingService.save(booking);

            return "redirect:/booking";

        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("booking", booking);
            model.addAttribute("rooms", roomService.findAll());
            model.addAttribute("statuses", BookingStatus.values());
            return "booking/booking-form";
        }
    }

    // Xóa
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        bookingService.deleteById(id);
        return "redirect:/booking";
    }
}
