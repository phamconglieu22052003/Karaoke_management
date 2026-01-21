package com.karaoke_management.controller;

import com.karaoke_management.payment.VnpayService;
import com.karaoke_management.payment.VnpayUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/payment/vnpay")
public class PaymentVnpayController {

    private final VnpayService vnpayService;

    public PaymentVnpayController(VnpayService vnpayService) {
        this.vnpayService = vnpayService;
    }

    /**
     * DÙNG CHO VIEW (nút Thanh toán nên submit POST vào đây)
     * POST /payment/vnpay/create
     */
    @PostMapping("/create")
    public String createPost(
            @RequestParam("invoiceId") Long invoiceId,
            @RequestParam(value = "amount", required = false) Long amount, // optional: nếu bạn truyền amount từ view
            HttpServletRequest request
    ) {
        // Nếu bạn chưa nối InvoiceService để lấy total từ DB,
        // thì tạm cho truyền amount từ view. Nếu null thì demo 100k.
        long amountVnd = (amount != null && amount > 0) ? amount : 100000L;

        String payUrl = vnpayService.createPaymentUrl(invoiceId, amountVnd, request);
        return "redirect:" + payUrl;
    }

    /**
     * GET /payment/vnpay/create?invoiceId=1&amount=100000
     */
    @GetMapping("/create")
    public String createGet(
            @RequestParam("invoiceId") Long invoiceId,
            @RequestParam(value = "amount", required = false) Long amount,
            HttpServletRequest request
    ) {
        long amountVnd = (amount != null && amount > 0) ? amount : 100000L;
        String payUrl = vnpayService.createPaymentUrl(invoiceId, amountVnd, request);
        return "redirect:" + payUrl;
    }

    /**
     * VNPay redirect về returnUrl (bạn cấu hình vnpay.return-url trỏ vào đây)
     * GET /payment/vnpay/return?...params...
     */
    @GetMapping("/return")
    public String vnpayReturn(HttpServletRequest request, Model model) {
        Map<String, String> allParams = VnpayUtil.flattenParams(request.getParameterMap());

        boolean validHash = vnpayService.verifyReturn(allParams);

        String rspCode = allParams.getOrDefault("vnp_ResponseCode", "");
        String txnRef = allParams.getOrDefault("vnp_TxnRef", "");
        String orderInfo = allParams.getOrDefault("vnp_OrderInfo", "");
        String amount = allParams.getOrDefault("vnp_Amount", "");

        boolean success = validHash && "00".equals(rspCode);

        model.addAttribute("validHash", validHash);
        model.addAttribute("success", success);
        model.addAttribute("rspCode", rspCode);
        model.addAttribute("txnRef", txnRef);
        model.addAttribute("orderInfo", orderInfo);
        model.addAttribute("amount", amount);

        return "payment/vnpay-return";
    }
}
