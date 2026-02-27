package com.karaoke_management.controller;

import com.karaoke_management.entity.Invoice;
import com.karaoke_management.entity.InvoiceLine;
import com.karaoke_management.enums.InvoiceLineType;
import com.karaoke_management.entity.InvoiceStatus;
import com.karaoke_management.entity.Shift;
import com.karaoke_management.enums.ShiftStatus;
import com.karaoke_management.repository.InvoiceRepository;
import com.karaoke_management.repository.ShiftRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/shift")
public class ShiftController {

    private final ShiftRepository shiftRepository;
    private final InvoiceRepository invoiceRepository;

    public ShiftController(ShiftRepository shiftRepository, InvoiceRepository invoiceRepository) {
        this.shiftRepository = shiftRepository;
        this.invoiceRepository = invoiceRepository;
    }

    // ====== Open shift ======
    @GetMapping("/open")
    public String openForm(@RequestParam(value = "returnUrl", required = false) String returnUrl,
                           Model model) {
        model.addAttribute("hasOpenShift", shiftRepository.existsByStatus(ShiftStatus.OPEN));
        model.addAttribute("openingCash", "0");
        model.addAttribute("returnUrl", safeReturnUrlOrEmpty(returnUrl));
        return "shift/shift-open";
    }

    @PostMapping("/open")
    public String openShift(
            @RequestParam(value = "openingCash", required = false) String openingCash,
            @RequestParam(value = "note", required = false) String note,
            @RequestParam(value = "returnUrl", required = false) String returnUrl,
            Authentication authentication
    ) {
        if (shiftRepository.existsByStatus(ShiftStatus.OPEN)) {
            // Đã có ca mở -> quay lại trang trước đó (nếu có), hoặc chuyển tới ca hiện tại
            String safe = safeReturnUrlOrEmpty(returnUrl);
            if (!safe.isBlank()) return "redirect:" + safe;
            return "redirect:/shift/current";
        }

        BigDecimal cash = parseMoney(openingCash);
        LocalDateTime now = LocalDateTime.now();

        Shift shift = new Shift();
        shift.setStatus(ShiftStatus.OPEN);
        shift.setOpenedAt(now);
        shift.setOpenedBy(authentication != null ? authentication.getName() : "system");
        shift.setOpeningCash(cash);
        shift.setNote(note != null ? note.trim() : null);

        // ===== Fix lỗi 500 (DB yêu cầu shift_name NOT NULL) =====
        // Tạo tên ca tự động để không bắt người dùng nhập.
        // Format ngắn gọn để không vượt quá nvarchar(80).
        String autoName = "Ca " + now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        shift.setShiftName(autoName);

        // Schema hiện tại có start_time/end_time/created_at NOT NULL.
        // Khi mở ca: start_time = giờ mở; end_time tạm = giờ mở (sẽ cập nhật khi đóng ca).
        LocalTime t = now.toLocalTime().withNano(0);
        shift.setStartTime(t);
        shift.setEndTime(t);
        shift.setCreatedAt(now);
        shiftRepository.save(shift);

        String safe = safeReturnUrlOrEmpty(returnUrl);
        if (!safe.isBlank()) return "redirect:" + safe;
        return "redirect:/shift/current";
    }

    // ====== Current / detail ======
    @GetMapping({"", "/current"})
    public String currentShift(Model model) {
        Shift shift = shiftRepository.findTopByStatusOrderByOpenedAtDesc(ShiftStatus.OPEN)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chưa có ca đang mở"));
        return detail(shift.getId(), model);
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy ca"));

        LocalDateTime from = shift.getOpenedAt();
        LocalDateTime to = shift.getClosedAt() != null ? shift.getClosedAt() : LocalDateTime.now();

        List<Invoice> invoices = invoiceRepository.findPaidWithLinesBetween(from, to, InvoiceStatus.PAID);
        ShiftSummary summary = buildSummary(shift, invoices);

        model.addAttribute("shift", shift);
        model.addAttribute("summary", summary);
        model.addAttribute("invoices", invoices);
        model.addAttribute("rangeFrom", from);
        model.addAttribute("rangeTo", to);
        return "shift/shift-detail";
    }

    // ====== Close shift ======
    @PostMapping("/{id}/close")
    public String closeShift(
            @PathVariable("id") Long id,
            @RequestParam(value = "closingCashDeclared", required = false) String closingCashDeclared,
            @RequestParam(value = "note", required = false) String note,
            Authentication authentication
    ) {
        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy ca"));

        if (shift.getStatus() == ShiftStatus.CLOSED) {
            return "redirect:/shift/" + id;
        }

        shift.setStatus(ShiftStatus.CLOSED);
        LocalDateTime now = LocalDateTime.now();
        shift.setClosedAt(now);
        shift.setClosedBy(authentication != null ? authentication.getName() : "system");

        // Update end_time để tương thích schema (TIME NOT NULL)
        shift.setEndTime(now.toLocalTime().withNano(0));

        BigDecimal declared = parseMoneyNullable(closingCashDeclared);
        shift.setClosingCashDeclared(declared);
        if (note != null && !note.trim().isEmpty()) {
            shift.setNote(note.trim());
        }
        shiftRepository.save(shift);
        return "redirect:/shift/" + id;
    }

    // ====== Print ======
    @GetMapping("/{id}/print")
    public String print(@PathVariable("id") Long id, Model model) {
        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy ca"));

        LocalDateTime from = shift.getOpenedAt();
        LocalDateTime to = shift.getClosedAt() != null ? shift.getClosedAt() : LocalDateTime.now();
        List<Invoice> invoices = invoiceRepository.findPaidWithLinesBetween(from, to, InvoiceStatus.PAID);
        ShiftSummary summary = buildSummary(shift, invoices);

        model.addAttribute("shift", shift);
        model.addAttribute("summary", summary);
        model.addAttribute("invoices", invoices);
        model.addAttribute("rangeFrom", from);
        model.addAttribute("rangeTo", to);
        return "shift/shift-print";
    }

    // ===== Helpers =====
    private static BigDecimal parseMoney(String s) {
        if (s == null) return BigDecimal.ZERO;
        String clean = s.trim().replace(",", "");
        if (clean.isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(clean);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private static BigDecimal parseMoneyNullable(String s) {
        if (s == null) return null;
        String clean = s.trim().replace(",", "");
        if (clean.isEmpty()) return null;
        try {
            return new BigDecimal(clean);
        } catch (Exception e) {
            return null;
        }
    }

    private static ShiftSummary buildSummary(Shift shift, List<Invoice> invoices) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal cash = BigDecimal.ZERO;
        BigDecimal vnpay = BigDecimal.ZERO;
        BigDecimal room = BigDecimal.ZERO;
        BigDecimal items = BigDecimal.ZERO;

        for (Invoice inv : invoices) {
            if (inv.getTotalAmount() != null) total = total.add(inv.getTotalAmount());
            String pm = inv.getPaymentMethod() != null ? inv.getPaymentMethod().toUpperCase(Locale.ROOT) : "";
            if (pm.contains("CASH") || pm.contains("TIEN") || pm.contains("TIỀN")) {
                if (inv.getTotalAmount() != null) cash = cash.add(inv.getTotalAmount());
            } else {
                if (inv.getTotalAmount() != null) vnpay = vnpay.add(inv.getTotalAmount());
            }

            // Breakdown từ snapshot lines
            if (inv.getLines() != null) {
                for (InvoiceLine line : inv.getLines()) {
                    if (line.getAmount() == null) continue;
                    if (line.getLineType() == InvoiceLineType.ROOM) room = room.add(line.getAmount());
                    if (line.getLineType() == InvoiceLineType.ITEM) items = items.add(line.getAmount());
                }
            }
        }

        ShiftSummary s = new ShiftSummary();
        s.shiftId = shift.getId();
        s.invoiceCount = invoices.size();
        s.totalAmount = total;
        s.cashAmount = cash;
        s.vnpayAmount = vnpay;
        s.roomAmount = room;
        s.itemAmount = items;

        // Kỳ vọng tiền mặt = openingCash + cashAmount
        BigDecimal expected = (shift.getOpeningCash() != null ? shift.getOpeningCash() : BigDecimal.ZERO)
                .add(cash);
        s.expectedCashInDrawer = expected;
        if (shift.getClosingCashDeclared() != null) {
            s.cashDifference = shift.getClosingCashDeclared().subtract(expected);
        }

        return s;
    }

    // Chống open redirect: chỉ cho phép returnUrl dạng path nội bộ bắt đầu bằng '/'
    private static String safeReturnUrlOrEmpty(String returnUrl) {
        if (returnUrl == null) return "";
        String s = returnUrl.trim();
        if (s.isEmpty()) return "";
        if (!s.startsWith("/")) return "";
        if (s.startsWith("//")) return "";
        if (s.contains("://")) return "";
        return s;
    }

    public static class ShiftSummary {
        public Long shiftId;
        public int invoiceCount;
        public BigDecimal totalAmount;
        public BigDecimal cashAmount;
        public BigDecimal vnpayAmount;
        public BigDecimal roomAmount;
        public BigDecimal itemAmount;
        public BigDecimal expectedCashInDrawer;
        public BigDecimal cashDifference;
    }
}
