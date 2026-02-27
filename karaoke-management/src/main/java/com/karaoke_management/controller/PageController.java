package com.karaoke_management.controller;

import com.karaoke_management.entity.BookingStatus;
import com.karaoke_management.entity.InvoiceStatus;
import com.karaoke_management.entity.RoomStatus;
import com.karaoke_management.enums.ShiftStatus;
import com.karaoke_management.repository.BookingRepository;
import com.karaoke_management.repository.InvoiceRepository;
import com.karaoke_management.repository.RoomRepository;
import com.karaoke_management.repository.ShiftRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Controller
public class PageController {

    private final InvoiceRepository invoiceRepository;
    private final ShiftRepository shiftRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;

    public PageController(InvoiceRepository invoiceRepository,
                          ShiftRepository shiftRepository,
                          RoomRepository roomRepository,
                          BookingRepository bookingRepository) {
        this.invoiceRepository = invoiceRepository;
        this.shiftRepository = shiftRepository;
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        LocalDateTime now = LocalDateTime.now();

        // Doanh thu hôm nay (theo paidAt)
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        BigDecimal revenueToday = invoiceRepository.sumPaidAmountBetween(startOfDay, endOfDay, InvoiceStatus.PAID);

        // Ca hiện tại + doanh thu ca hiện tại
        var openShiftOpt = shiftRepository.findTopByStatusOrderByOpenedAtDesc(ShiftStatus.OPEN);
        BigDecimal revenueShift = BigDecimal.ZERO;
        if (openShiftOpt.isPresent() && openShiftOpt.get().getOpenedAt() != null) {
            revenueShift = invoiceRepository.sumPaidAmountBetween(openShiftOpt.get().getOpenedAt(), now, InvoiceStatus.PAID);
        }

        // Phòng đang OCCUPIED
        long occupiedRooms = roomRepository.countByStatus(RoomStatus.OCCUPIED);

        // Booking sắp tới (mặc định 24h)
        int upcomingHours = 24;
        LocalDateTime to = now.plusHours(upcomingHours);
        long upcomingBookingCount = bookingRepository.countByStartTimeBetweenAndStatusNot(now, to, BookingStatus.CANCELLED);
        var upcomingBookings = bookingRepository.findTop5ByStartTimeAfterAndStatusNotOrderByStartTimeAsc(now, BookingStatus.CANCELLED);

        model.addAttribute("now", now);
        model.addAttribute("revenueToday", revenueToday);
        model.addAttribute("openShift", openShiftOpt.orElse(null));
        model.addAttribute("revenueCurrentShift", revenueShift);
        model.addAttribute("occupiedRooms", occupiedRooms);
        model.addAttribute("upcomingBookingCount", upcomingBookingCount);
        model.addAttribute("upcomingBookings", upcomingBookings);
        model.addAttribute("upcomingHours", upcomingHours);
        return "dashboard";
    }
    // @GetMapping("/invoice")
    // public String invoice() {
    //     return "invoice/invoice-list";
    // }
}
