package com.karaoke_management.payment;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class VnpayService {

    private final VnpayConfig cfg;

    public VnpayService(VnpayConfig cfg) {
        this.cfg = cfg;
    }

    public String createPaymentUrl(Long invoiceId, long amountVnd, HttpServletRequest request) {
        // TxnRef bạn dùng invoiceId cho dễ parse
        String txnRef = String.valueOf(invoiceId);

        String ipAddr = VnpayUtil.getIpAddress(request);
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Version", cfg.getVersion());
        params.put("vnp_Command", cfg.getCommand());
        params.put("vnp_TmnCode", cfg.getTmnCode());

        // VNPay dùng đơn vị nhỏ nhất => *100
        params.put("vnp_Amount", String.valueOf(amountVnd * 100));

        params.put("vnp_CurrCode", cfg.getCurrCode()); // VND
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", "Thanh toan hoa don " + invoiceId);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", cfg.getLocale());
        params.put("vnp_ReturnUrl", cfg.getReturnUrl());
        params.put("vnp_IpAddr", ipAddr);
        params.put("vnp_CreateDate", now);

        // bankCode optional
        if (cfg.getBankCode() != null && !cfg.getBankCode().isBlank()) {
            params.put("vnp_BankCode", cfg.getBankCode());
        }

        String hashData = VnpayUtil.buildHashData(params);
        String secureHash = VnpayUtil.hmacSHA512(cfg.getHashSecret(), hashData);

        String query = VnpayUtil.buildQueryString(params);
        return cfg.getPayUrl() + "?" + query + "&vnp_SecureHash=" + secureHash;
    }

    public boolean verifyReturn(Map<String, String> allParams) {
        return VnpayUtil.verifySecureHash(allParams, cfg.getHashSecret());
    }
}
