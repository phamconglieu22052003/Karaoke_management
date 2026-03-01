package com.karaoke_management.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * Tạo link thanh toán công khai cho khách hàng theo dạng:
 *   /customer/pay/{invoiceId}?e={expiryEpochSeconds}&t={token}
 *
 * - Token là HMAC-SHA256(invoiceId:expiry) với secret cấu hình.
 * - Không lưu DB (không đổi schema).
 */
@Service
public class CustomerPayLinkService {

    private final String secret;
    private final long ttlMinutes;

    public CustomerPayLinkService(
            @Value("${app.customerPay.secret:CHANGE_ME_CUSTOMER_PAY_SECRET}") String secret,
            @Value("${app.customerPay.ttl-minutes:120}") long ttlMinutes
    ) {
        this.secret = secret;
        this.ttlMinutes = ttlMinutes;
    }

    /**
     * Trả về URL tương đối (relative path) để render QR/link trong UI nội bộ.
     */
    public String buildCustomerPayUrl(Long invoiceId) {
        long exp = Instant.now().plusSeconds(ttlMinutes * 60).getEpochSecond();
        String token = sign(invoiceId, exp);
        return "/customer/pay/" + invoiceId + "?e=" + exp + "&t=" + token;
    }

    public boolean isValid(Long invoiceId, Long expEpochSeconds, String token) {
        if (invoiceId == null || expEpochSeconds == null || token == null || token.isBlank()) return false;
        long now = Instant.now().getEpochSecond();
        if (expEpochSeconds < now) return false;
        String expected = sign(invoiceId, expEpochSeconds);
        return constantTimeEquals(expected, token);
    }

    private String sign(Long invoiceId, long expEpochSeconds) {
        String payload = invoiceId + ":" + expEpochSeconds;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            // Fallback: không nên xảy ra, nhưng tránh crash runtime
            return "";
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        if (x.length != y.length) return false;
        int r = 0;
        for (int i = 0; i < x.length; i++) {
            r |= (x[i] ^ y[i]);
        }
        return r == 0;
    }
}
