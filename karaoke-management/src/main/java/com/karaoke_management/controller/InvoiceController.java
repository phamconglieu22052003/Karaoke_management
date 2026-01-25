package com.karaoke_management.controller;

import com.karaoke_management.entity.Invoice;
import com.karaoke_management.repository.InvoiceRepository;
import com.karaoke_management.service.InvoiceService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/invoice")
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceRepository invoiceRepository, InvoiceService invoiceService) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceService = invoiceService;
    }

    // ===================== LIST =====================
    @GetMapping
    public String list(Model model) {
        model.addAttribute("invoices", invoiceRepository.findAllByOrderByIdDesc());
        return "invoice/invoice-list";
    }

    // ===================== DETAIL =====================
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Invoice inv = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
        model.addAttribute("invoice", inv);
        return "invoice/invoice-detail";
    }

    // ===================== CREATE FROM ROOM SESSION =====================
    /**
     * Tạo hóa đơn theo RoomSession (sessionId)
     * URL bạn đang dùng: /invoice/create/{roomSessionId}
     * - Nếu đã có hóa đơn cho session này -> trả lại hóa đơn cũ
     * - Nhận cả GET/POST để tránh lỗi 405 khi gọi từ form hoặc gõ URL
     */
    @RequestMapping(value = "/create/{roomSessionId}", method = {RequestMethod.GET, RequestMethod.POST})
    public String createBySession(@PathVariable Long roomSessionId) {
        Invoice inv = invoiceService.createOrGetBySession(roomSessionId);
        return "redirect:/invoice/" + inv.getId();
    }

    /**
     * (Tuỳ chọn) /invoice/create?roomSessionId=17
     * Nhận cả GET/POST để tránh 405 khi submit form
     */
    @RequestMapping(value = "/create", method = {RequestMethod.GET, RequestMethod.POST})
    public String createBySessionQuery(@RequestParam("roomSessionId") Long roomSessionId) {
        Invoice inv = invoiceService.createOrGetBySession(roomSessionId);
        return "redirect:/invoice/" + inv.getId();
    }
}
        