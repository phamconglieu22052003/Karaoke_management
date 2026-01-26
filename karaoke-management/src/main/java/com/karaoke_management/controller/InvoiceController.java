package com.karaoke_management.controller;

import com.karaoke_management.entity.Invoice;
import com.karaoke_management.repository.InvoiceRepository;
import com.karaoke_management.service.InvoiceService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Controller
@RequestMapping("/invoice")
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;

    // ✅ Format VN: giờ/ngày/tháng/năm
    private static final DateTimeFormatter VN_DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public InvoiceController(InvoiceRepository invoiceRepository, InvoiceService invoiceService) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceService = invoiceService;
    }

    // ✅ LIST + FILTER:
    // /invoice?from=26/01/2026 10:00&to=26/01/2026 22:00&min=100000&max=500000
    // Không truyền param => vẫn list toàn bộ như cũ.
    @GetMapping
    public String list(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) BigDecimal min,
            @RequestParam(required = false) BigDecimal max,
            Model model
    ) {
        LocalDateTime fromDt = parseVnDateTimeOrNull(from);
        LocalDateTime toDt = parseVnDateTimeOrNull(to);

        model.addAttribute("invoices", invoiceRepository.filterInvoices(fromDt, toDt, min, max));

        // ✅ giữ lại input dạng VN để render lại lên form (type=text)
        model.addAttribute("from", from == null ? "" : from);
        model.addAttribute("to", to == null ? "" : to);
        model.addAttribute("min", min);
        model.addAttribute("max", max);

        // để hiển thị hint
        model.addAttribute("dtPattern", "dd/MM/yyyy HH:mm");

        return "invoice/invoice-list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Invoice inv = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
        model.addAttribute("invoice", inv);
        return "invoice/invoice-detail";
    }

    // ✅ Tạo/Xem hóa đơn theo RoomSession (GET) - giữ chức năng
    @GetMapping("/create/{roomSessionId}")
    public String createBySessionGet(@PathVariable Long roomSessionId) {
        Invoice inv = invoiceService.createOrGetBySession(roomSessionId);
        return "redirect:/invoice/" + inv.getId();
    }

    // ✅ FIX 405: UI của bạn đang POST từ session-list.html
    @PostMapping("/create/{roomSessionId}")
    public String createBySessionPost(@PathVariable Long roomSessionId) {
        Invoice inv = invoiceService.createOrGetBySession(roomSessionId);
        return "redirect:/invoice/" + inv.getId();
    }

    // (Tuỳ chọn) /invoice/create?roomSessionId=17 (GET)
    @GetMapping("/create")
    public String createBySessionQueryGet(@RequestParam("roomSessionId") Long roomSessionId) {
        Invoice inv = invoiceService.createOrGetBySession(roomSessionId);
        return "redirect:/invoice/" + inv.getId();
    }

    // ✅ FIX 405 nếu có nơi nào đó POST /invoice/create?roomSessionId=...
    @PostMapping("/create")
    public String createBySessionQueryPost(@RequestParam("roomSessionId") Long roomSessionId) {
        Invoice inv = invoiceService.createOrGetBySession(roomSessionId);
        return "redirect:/invoice/" + inv.getId();
    }

    // ================= HELPER =================
    private LocalDateTime parseVnDateTimeOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDateTime.parse(s.trim(), VN_DTF);
        } catch (DateTimeParseException ex) {
            return null; // không crash, coi như user chưa nhập đúng
        }
    }
}
