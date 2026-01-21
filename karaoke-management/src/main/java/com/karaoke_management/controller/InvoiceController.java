package com.karaoke_management.controller;

import com.karaoke_management.service.InvoiceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/invoice")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    // List invoices
    @GetMapping
    public String list(Model model) {
        model.addAttribute("invoices", invoiceService.findAll());
        return "invoice/invoice-list";
    }

    // Create invoice from session
    @PostMapping("/create/{sessionId}")
    public String create(@PathVariable Long sessionId) {
        var inv = invoiceService.createOrGetBySession(sessionId);
        return "redirect:/invoice/" + inv.getId();
    }

    // View invoice detail
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("invoice", invoiceService.getRequired(id));
        return "invoice/invoice-detail";
    }
}
