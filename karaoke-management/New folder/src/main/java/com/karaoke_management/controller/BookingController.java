package com.karaoke_management.controller;

import com.karaoke_management.entity.Booking;
import com.karaoke_management.entity.BookingStatus;
import com.karaoke_management.entity.Room;
import com.karaoke_management.service.BookingService;
import com.karaoke_management.service.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/booking")
public class BookingController {

    private final BookingService bookingService;
    private final RoomService roomService;

    public BookingController(BookingService bookingService, RoomService roomService) {
        this.bookingService = bookingService;
        this.roomService = roomService;
    }

    // LIST
    @GetMapping
    public String list(Model model) {
        model.addAttribute("bookings", bookingService.findAll());
        return "booking/booking-list";
    }

    // SHOW FORM CREATE
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        Booking booking = new Booking();
        if (booking.getStatus() == null) {
            booking.setStatus(BookingStatus.BOOKED);
        }

        model.addAttribute("booking", booking);
        model.addAttribute("rooms", roomService.findAll());
        model.addAttribute("statuses", BookingStatus.values());
        return "booking/booking-form";
    }

    // SAVE CREATE
    @PostMapping
    public String create(
            @RequestParam("roomId") Long roomId,
            @ModelAttribute("booking") Booking booking,
            Model model
    ) {
        try {
            Room room = roomService.findById(roomId);
            if (room == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
            }
            booking.setRoom(room);

            bookingService.save(booking);
            return "redirect:/booking";
        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("booking", booking);
            model.addAttribute("rooms", roomService.findAll());
            model.addAttribute("statuses", BookingStatus.values());
            return "booking/booking-form";
        }
    }

    // SHOW FORM EDIT
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Booking booking = bookingService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        model.addAttribute("booking", booking);
        model.addAttribute("rooms", roomService.findAll());
        model.addAttribute("statuses", BookingStatus.values());
        return "booking/booking-form";
    }

    // UPDATE
    @PostMapping("/update")
    public String update(
            @RequestParam("roomId") Long roomId,
            @ModelAttribute("booking") Booking booking,
            Model model
    ) {
        try {
            // đảm bảo booking tồn tại
            bookingService.findById(booking.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

            Room room = roomService.findById(roomId);
            if (room == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
            }
            booking.setRoom(room);

            bookingService.save(booking);
            return "redirect:/booking";
        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("booking", booking);
            model.addAttribute("rooms", roomService.findAll());
            model.addAttribute("statuses", BookingStatus.values());
            return "booking/booking-form";
        }
    }

    // DELETE
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        bookingService.deleteById(id);
        return "redirect:/booking";
    }
}
