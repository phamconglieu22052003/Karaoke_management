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

    private static final DateTimeFormatter VNP_DATE =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public VnpayService(VnpayConfig cfg) {
        this.cfg = cfg;
    }

    public String createPaymentUrl(Long invoiceId, long amountVnd, HttpServletRequest request) {
        // Validate cấu hình tối thiểu (tránh silent fail)
        if (cfg.getTmnCode() == null || cfg.getTmnCode().isBlank()
                || cfg.getHashSecret() == null || cfg.getHashSecret().isBlank()
                || cfg.getPayUrl() == null || cfg.getPayUrl().isBlank()
                || cfg.getReturnUrl() == null || cfg.getReturnUrl().isBlank()) {
            throw new IllegalStateException("VNPAY config thiếu (tmnCode/hashSecret/payUrl/returnUrl). Kiểm tra application.properties");
        }

        String txnRef = String.valueOf(invoiceId); // nên unique theo invoice
        String ipAddr = VnpayUtil.getIpAddress(request);

        LocalDateTime now = LocalDateTime.now();
        String createDate = now.format(VNP_DATE);
        // expire 15 phút
        String expireDate = now.plusMinutes(15).format(VNP_DATE);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Version", cfg.getVersion());
        params.put("vnp_Command", cfg.getCommand());
        params.put("vnp_TmnCode", cfg.getTmnCode());

        // Amount: VNPay yêu cầu đơn vị "xu" => *100
        params.put("vnp_Amount", String.valueOf(amountVnd * 100));
        params.put("vnp_CurrCode", "VND");

        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", "Thanh toan hoa don " + invoiceId);
        params.put("vnp_OrderType", "other");

        params.put("vnp_Locale", cfg.getLocale());
        params.put("vnp_ReturnUrl", cfg.getReturnUrl());
        params.put("vnp_IpAddr", ipAddr);

        params.put("vnp_CreateDate", createDate);
        params.put("vnp_ExpireDate", expireDate);

        // ép QR nếu cấu hình
        if (cfg.getBankCode() != null && !cfg.getBankCode().isBlank()) {
            params.put("vnp_BankCode", cfg.getBankCode()); // VNPAYQR
        }

        // thêm loại hash (nhiều hệ thống VNPay chấp nhận có/không; có thì rõ ràng hơn)
        params.put("vnp_SecureHashType", "HmacSHA512");

        String hashData = VnpayUtil.buildHashData(params);
        String secureHash = VnpayUtil.hmacSHA512(cfg.getHashSecret(), hashData);

        String query = VnpayUtil.buildQueryString(params);
        return cfg.getPayUrl() + "?" + query + "&vnp_SecureHash=" + secureHash;
    }

    public boolean verifyReturn(Map<String, String> allParams) {
        return VnpayUtil.verifySecureHash(allParams, cfg.getHashSecret());
    }
}
