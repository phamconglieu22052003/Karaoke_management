package com.karaoke_management.controller;

import com.karaoke_management.entity.Invoice;
import com.karaoke_management.repository.InvoiceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/invoice")
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;

    public InvoiceController(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("invoices", invoiceRepository.findAllByOrderByIdDesc());
        return "invoice/invoice-list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Invoice inv = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
        model.addAttribute("invoice", inv);
        return "invoice/invoice-detail";
    }
}
