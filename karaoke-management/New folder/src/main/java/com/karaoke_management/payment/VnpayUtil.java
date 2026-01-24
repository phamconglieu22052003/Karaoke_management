package com.karaoke_management.payment;

import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class VnpayUtil {

    private VnpayUtil() {}

    // ✅ Lấy IP đúng cách (VNPay khuyến nghị)
    public static String getIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // lấy IP đầu tiên
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) return xRealIp.trim();

        return request.getRemoteAddr();
    }

    // ✅ flatten parameterMap (Map<String,String[]> -> Map<String,String>)
    public static Map<String, String> flattenParams(Map<String, String[]> parameterMap) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> e : parameterMap.entrySet()) {
            String k = e.getKey();
            String[] v = e.getValue();
            if (v != null && v.length > 0) out.put(k, v[0]);
        }
        return out;
    }

    public static String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] bytes = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception ex) {
            throw new RuntimeException("HMAC SHA512 error", ex);
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.US_ASCII);
    }

    // ✅ HashData: sort key, bỏ vnp_SecureHash, vnp_SecureHashType
    public static String buildHashData(Map<String, String> params) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);

        StringBuilder sb = new StringBuilder();
        for (String k : keys) {
            if ("vnp_SecureHash".equals(k) || "vnp_SecureHashType".equals(k)) continue;
            String v = params.get(k);
            if (v == null || v.isBlank()) continue;

            if (sb.length() > 0) sb.append('&');
            sb.append(k).append('=').append(enc(v));
        }
        return sb.toString();
    }

    // ✅ QueryString: sort key, bỏ vnp_SecureHash, vnp_SecureHashType
    public static String buildQueryString(Map<String, String> params) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);

        StringBuilder sb = new StringBuilder();
        for (String k : keys) {
            if ("vnp_SecureHash".equals(k) || "vnp_SecureHashType".equals(k)) continue;
            String v = params.get(k);
            if (v == null || v.isBlank()) continue;

            if (sb.length() > 0) sb.append('&');
            sb.append(enc(k)).append('=').append(enc(v));
        }
        return sb.toString();
    }

    // ✅ verify chữ ký trả về từ VNPay
    public static boolean verifySecureHash(Map<String, String> allParams, String hashSecret) {
        String received = allParams.get("vnp_SecureHash");
        if (received == null || received.isBlank()) return false;

        Map<String, String> cloned = new LinkedHashMap<>(allParams);
        cloned.remove("vnp_SecureHash");
        cloned.remove("vnp_SecureHashType");

        String hashData = buildHashData(cloned);
        String expected = hmacSHA512(hashSecret, hashData);
        return expected.equalsIgnoreCase(received);
    }
}
