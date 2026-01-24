package com.karaoke_management.controller;

import com.karaoke_management.entity.Invoice;
import com.karaoke_management.entity.InvoiceStatus;
import com.karaoke_management.payment.VnpayService;
import com.karaoke_management.repository.InvoiceRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/payment/vnpay")
public class PaymentVnpayController {

    private final VnpayService vnpayService;
    private final InvoiceRepository invoiceRepository;

    public PaymentVnpayController(VnpayService vnpayService, InvoiceRepository invoiceRepository) {
        this.vnpayService = vnpayService;
        this.invoiceRepository = invoiceRepository;
    }

    // ===================== A) CREATE PAYMENT URL =====================
    // Cho phép POST từ form hoặc GET từ link cho tiện test
    @RequestMapping(value = "/create", method = {RequestMethod.POST, RequestMethod.GET})
    public String create(@RequestParam("invoiceId") Long invoiceId, HttpServletRequest req) {
        Invoice inv = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        if (inv.getStatus() == InvoiceStatus.PAID) {
            return "redirect:/invoice/" + invoiceId;
        }

        long amountVnd = inv.getTotalAmount().longValue(); // totalAmount đang là VND
        String payUrl = vnpayService.createPaymentUrl(inv.getId(), amountVnd, req);

        // lưu txnRef nếu bạn muốn dùng findByVnpTxnRef (ở đây txnRef = invoiceId)
        inv.setVnpTxnRef(String.valueOf(inv.getId()));
        invoiceRepository.save(inv);

        return "redirect:" + payUrl;
    }

    // ===================== B) RETURN URL (HIỂN THỊ CHO USER) =====================
    @GetMapping("/return")
    public String vnpayReturn(@RequestParam Map<String, String> allParams, Model model) {
        boolean valid = vnpayService.verifyReturn(allParams);

        String txnRef = allParams.get("vnp_TxnRef");            // invoiceId dạng string
        String responseCode = allParams.get("vnp_ResponseCode");// "00" là OK
        String transactionNo = allParams.get("vnp_TransactionNo");

        Long invoiceId = parseInvoiceId(txnRef);

        model.addAttribute("validSignature", valid);
        model.addAttribute("txnRef", txnRef);
        model.addAttribute("invoiceId", invoiceId);
        model.addAttribute("responseCode", responseCode);
        model.addAttribute("transactionNo", transactionNo);
        model.addAttribute("success", valid && "00".equals(responseCode));

        return "payment/vnpay-return";
    }

    // ===================== C) IPN (CẬP NHẬT DB) =====================
    @GetMapping("/ipn")
    @ResponseBody
    @Transactional
    public Map<String, String> ipn(@RequestParam Map<String, String> allParams) {

        if (!vnpayService.verifyReturn(allParams)) {
            return Map.of("RspCode", "97", "Message", "Invalid signature");
        }

        String txnRef = allParams.get("vnp_TxnRef");
        String responseCode = allParams.get("vnp_ResponseCode");
        String transactionNo = allParams.get("vnp_TransactionNo");

        Long invoiceId = parseInvoiceId(txnRef);
        if (invoiceId == null) {
            return Map.of("RspCode", "01", "Message", "Order not found");
        }

        Optional<Invoice> opt = invoiceRepository.findById(invoiceId);
        if (opt.isEmpty()) {
            return Map.of("RspCode", "01", "Message", "Order not found");
        }

        Invoice inv = opt.get();

        if ("00".equals(responseCode)) {
            inv.setStatus(InvoiceStatus.PAID);
            inv.setPaidAt(LocalDateTime.now());
            inv.setVnpTransactionNo(transactionNo);
            invoiceRepository.save(inv);
        } else {
            inv.setStatus(InvoiceStatus.FAILED);
            inv.setVnpTransactionNo(transactionNo);
            invoiceRepository.save(inv);
        }

        // VNPay yêu cầu trả 00 để xác nhận đã nhận IPN
        return Map.of("RspCode", "00", "Message", "Confirm Success");
    }

    private Long parseInvoiceId(String txnRef) {
        if (txnRef == null || txnRef.isBlank()) return null;

        // Nếu bạn dùng INVxxx_... thì parse kiểu đó
        // Nhưng ở đây txnRef = invoiceId => parse thẳng
        try {
            return Long.parseLong(txnRef);
        } catch (Exception ignored) {
            return null;
        }
    }
}
