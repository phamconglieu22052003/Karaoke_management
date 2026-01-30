package com.karaoke_management.controller;

import com.karaoke_management.repository.InvoiceRepository;
import com.karaoke_management.repository.RoomRepository;
import com.karaoke_management.service.InvoiceService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.karaoke_management.entity.Invoice;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Controller
@RequestMapping("/invoice")
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;
    private final RoomRepository roomRepository;

    // ✅ Format VN: giờ/ngày/tháng/năm
    private static final DateTimeFormatter VN_DTF = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    public InvoiceController(InvoiceRepository invoiceRepository,
                             InvoiceService invoiceService,
                             RoomRepository roomRepository) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceService = invoiceService;
        this.roomRepository = roomRepository;
    }

    // /invoice?from=...&to=...&min=...&max=...&roomId=...
    @GetMapping
    public String list(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) BigDecimal min,
            @RequestParam(required = false) BigDecimal max,
            @RequestParam(required = false) Long roomId,
            Model model
    ) {
        LocalDateTime fromDt = parseVnDateTimeOrNull(from);
        LocalDateTime toDt = parseVnDateTimeOrNull(to);

        model.addAttribute("invoices",
                invoiceRepository.filterInvoices(fromDt, toDt, min, max, roomId)
        );

        model.addAttribute("from", from == null ? "" : from);
        model.addAttribute("to", to == null ? "" : to);
        model.addAttribute("min", min);
        model.addAttribute("max", max);
        model.addAttribute("roomId", roomId); // ✅ để giữ lại dropdown phòng
        model.addAttribute("rooms", roomRepository.findAll());

        model.addAttribute("dtPattern", "HH:mm dd/MM/yyyy");
        return "invoice/invoice-list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Invoice inv = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
        model.addAttribute("invoice", inv);
        return "invoice/invoice-detail";
    }

    @GetMapping("/create/{roomSessionId}")
    public String createBySessionGet(@PathVariable Long roomSessionId) {
        Invoice inv = invoiceService.createOrGetBySession(roomSessionId);
        return "redirect:/invoice/" + inv.getId();
    }

    @PostMapping("/create/{roomSessionId}")
    public String createBySessionPost(@PathVariable Long roomSessionId) {
        Invoice inv = invoiceService.createOrGetBySession(roomSessionId);
        return "redirect:/invoice/" + inv.getId();
    }

    @GetMapping("/create")
    public String createBySessionQueryGet(@RequestParam("roomSessionId") Long roomSessionId) {
        Invoice inv = invoiceService.createOrGetBySession(roomSessionId);
        return "redirect:/invoice/" + inv.getId();
    }

    @PostMapping("/create")
    public String createBySessionQueryPost(@RequestParam("roomSessionId") Long roomSessionId) {
        Invoice inv = invoiceService.createOrGetBySession(roomSessionId);
        return "redirect:/invoice/" + inv.getId();
    }

    private LocalDateTime parseVnDateTimeOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDateTime.parse(s.trim(), VN_DTF);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
