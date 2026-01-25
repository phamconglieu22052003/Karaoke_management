package com.karaoke_management.controller;

import com.karaoke_management.entity.Invoice;
import com.karaoke_management.entity.InvoiceStatus;
import com.karaoke_management.repository.InvoiceRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Controller
@RequestMapping("/payment/vnpay/mock")
public class PaymentVnpayMockQrController {

    private final InvoiceRepository invoiceRepository;

    public PaymentVnpayMockQrController(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    /**
     * Trang hiển thị QR để khách quét
     * URL: /payment/vnpay/mock/qr?invoiceId=17
     */
    @GetMapping("/qr")
    public String showQr(@RequestParam("invoiceId") Long invoiceId,
                         HttpServletRequest request,
                         Model model) {

        Invoice inv = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        // Tạo URL public dựa trên request hiện tại (nếu mở bằng ngrok -> QR sẽ là link ngrok)
        String baseUrl = request.getScheme() + "://" + request.getServerName()
                + ((request.getServerPort() == 80 || request.getServerPort() == 443) ? "" : (":" + request.getServerPort()));

        // Link mà khách quét sẽ mở -> cập nhật PAID
        String confirmUrl = baseUrl + "/payment/vnpay/mock/confirm?invoiceId=" + invoiceId;

        model.addAttribute("invoice", inv);
        model.addAttribute("confirmUrl", confirmUrl);

        return "payment/vnpay-mock-qr";
    }

    /**
     * Khách quét QR -> mở link này -> hệ thống set PAID
     * URL: /payment/vnpay/mock/confirm?invoiceId=17
     */
    @GetMapping("/confirm")
    @Transactional
    public String confirmPaid(@RequestParam("invoiceId") Long invoiceId, Model model) {
        Invoice inv = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        // Nếu đã paid thì thôi
        if (inv.getStatus() != InvoiceStatus.PAID) {
            inv.setStatus(InvoiceStatus.PAID);
            inv.setPaidAt(LocalDateTime.now());
            inv.setVnpTransactionNo("MOCKQR-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
            invoiceRepository.save(inv);
        }

        model.addAttribute("invoiceId", invoiceId);
        model.addAttribute("transactionNo", inv.getVnpTransactionNo());
        model.addAttribute("success", true);

        return "payment/vnpay-mock-return";
    }
}
