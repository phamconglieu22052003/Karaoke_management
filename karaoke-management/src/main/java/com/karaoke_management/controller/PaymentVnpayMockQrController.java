package com.karaoke_management.controller;

import com.karaoke_management.entity.Invoice;
import com.karaoke_management.entity.InvoiceStatus;
import com.karaoke_management.repository.InvoiceRepository;
import com.karaoke_management.repository.ShiftRepository;
import com.karaoke_management.enums.ShiftStatus;
import com.karaoke_management.service.InvoiceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.UUID;

@Controller
@RequestMapping("/payment/vnpay/mock")
public class PaymentVnpayMockQrController {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;
    private final ShiftRepository shiftRepository;

    public PaymentVnpayMockQrController(InvoiceRepository invoiceRepository, InvoiceService invoiceService, ShiftRepository shiftRepository) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceService = invoiceService;
        this.shiftRepository = shiftRepository;
    }

    /**
     * Trang hiển thị QR để khách quét
     * URL: /payment/vnpay/mock/qr?invoiceId=17
     */
    @GetMapping("/qr")
    public String showQr(@RequestParam("invoiceId") Long invoiceId,
                         HttpServletRequest request,
                         Model model,
                         RedirectAttributes ra) {
        // ✅ Yêu cầu mở ca trước khi tạo QR thanh toán
        if (!shiftRepository.existsByStatus(ShiftStatus.OPEN)) {
            ra.addFlashAttribute("error", "Bạn cần mở ca trước khi tạo QR thanh toán.");
            return "redirect:/shift/open?returnUrl=/invoice/" + invoiceId;
        }


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

        // Nếu đã paid thì thôi (nhưng vẫn đảm bảo trừ kho idempotent)
        String txn = inv.getVnpTransactionNo();
        if (txn == null || txn.isBlank()) {
            txn = "MOCKQR-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        }

        // ✅ Mark PAID + tự động trừ kho (chống trừ 2 lần)
        inv = invoiceService.markPaidAndDeductInventory(invoiceId, "MOCK_QR", txn);

        model.addAttribute("invoiceId", invoiceId);
        model.addAttribute("transactionNo", inv.getVnpTransactionNo());
        model.addAttribute("success", true);

        return "payment/vnpay-mock-return";
    }
}
