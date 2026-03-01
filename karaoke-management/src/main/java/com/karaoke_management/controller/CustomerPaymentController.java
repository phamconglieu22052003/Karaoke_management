package com.karaoke_management.controller;

import com.karaoke_management.entity.Invoice;
import com.karaoke_management.enums.InvoiceLineType;
import com.karaoke_management.repository.InvoiceLineRepository;
import com.karaoke_management.repository.InvoiceRepository;
import com.karaoke_management.service.CustomerPayLinkService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

/**
 * Trang thanh toán công khai cho KHÁCH HÀNG.
 * - Chỉ HIỂN THỊ thông tin + QR (demo), KHÔNG được tự mark PAID.
 * - Bảo vệ bằng token HMAC trên query string.
 */
@Controller
@RequestMapping("/customer/pay")
public class CustomerPaymentController {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final CustomerPayLinkService customerPayLinkService;

    public CustomerPaymentController(
            InvoiceRepository invoiceRepository,
            InvoiceLineRepository invoiceLineRepository,
            CustomerPayLinkService customerPayLinkService
    ) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceLineRepository = invoiceLineRepository;
        this.customerPayLinkService = customerPayLinkService;
    }

    @GetMapping("/{invoiceId}")
    public String payPage(
            @PathVariable Long invoiceId,
            @RequestParam(name = "e", required = false) Long exp,
            @RequestParam(name = "t", required = false) String token,
            Model model
    ) {
        boolean valid = customerPayLinkService.isValid(invoiceId, exp, token);
        model.addAttribute("valid", valid);

        if (!valid) {
            // Không lộ thêm thông tin invoice khi token không hợp lệ
            model.addAttribute("error", "Link thanh toán không hợp lệ hoặc đã hết hạn.");
            return "customer/pay";
        }

        Invoice inv = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        // Snapshot lines để show breakdown (optional)
        var lines = invoiceLineRepository.findAllByInvoice_IdOrderByIdAsc(invoiceId);

        model.addAttribute("invoice", inv);
        model.addAttribute("lines", lines);
        model.addAttribute("roomCharge", sumByType(lines, InvoiceLineType.ROOM));
        model.addAttribute("orderCharge", sumByType(lines, InvoiceLineType.ITEM));

        // QR DATA (demo) - khách quét bằng app ngân hàng => demo
        String amount = inv.getTotalAmount() != null ? inv.getTotalAmount().toPlainString() : "0";
        String qrData = "KARAOKE_PAY|INV:" + invoiceId + "|AMT:" + amount;
        model.addAttribute("qrData", qrData);

        return "customer/pay";
    }

    private static BigDecimal sumByType(java.util.List<com.karaoke_management.entity.InvoiceLine> lines, InvoiceLineType type) {
        if (lines == null || lines.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        for (var l : lines) {
            if (l != null && l.getLineType() == type && l.getAmount() != null) {
                sum = sum.add(l.getAmount());
            }
        }
        return sum;
    }
}
