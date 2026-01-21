package com.karaoke_management.payment;

import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class VnpayUtil {

    private VnpayUtil() {}

    // Lấy IP client (ưu tiên header proxy)
    public static String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) return ip.trim();
        return request.getRemoteAddr();
    }

    // Convert request.getParameterMap() -> Map<String,String>
    public static Map<String, String> flattenParams(Map<String, String[]> parameterMap) {
        Map<String, String> result = new HashMap<>();
        if (parameterMap == null) return result;

        for (Map.Entry<String, String[]> e : parameterMap.entrySet()) {
            String key = e.getKey();
            String[] vals = e.getValue();
            if (vals != null && vals.length > 0) {
                result.put(key, vals[0]);
            }
        }
        return result;
    }

    /**
     * Build hash data theo sample VNPay hay dùng:
     * - Loại bỏ vnp_SecureHash, vnp_SecureHashType
     * - Sort key A-Z
     * - URL-encode key & value (đồng bộ với query string)
     * - Ghép key=value bằng dấu &
     */
    public static String buildHashData(Map<String, String> params) {
        if (params == null) return "";

        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);

        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            if (key == null) continue;
            if ("vnp_SecureHash".equals(key) || "vnp_SecureHashType".equals(key)) continue;

            String val = params.get(key);
            if (val == null || val.isBlank()) continue;

            if (sb.length() > 0) sb.append("&");
            sb.append(urlEncode(key)).append("=").append(urlEncode(val));
        }
        return sb.toString();
    }

    /**
     * Build query string gửi sang VNPay:
     * - Sort key
     * - URL encode key & value
     * - Bỏ field null/blank
     */
    public static String buildQueryString(Map<String, String> params) {
        if (params == null) return "";

        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);

        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            String val = params.get(key);
            if (key == null || val == null || val.isBlank()) continue;

            if (sb.length() > 0) sb.append("&");
            sb.append(urlEncode(key)).append("=").append(urlEncode(val));
        }
        return sb.toString();
    }

    public static String hmacSHA512(String secret, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] bytes = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(bytes);
        } catch (Exception e) {
            throw new RuntimeException("Error while signing HmacSHA512", e);
        }
    }

    /**
     * Verify chữ ký VNPay:
     * - allParams phải chứa vnp_SecureHash
     * - Tự build hashData từ allParams (loại vnp_SecureHash/Type)
     */
    public static boolean verifySecureHash(Map<String, String> allParams, String hashSecret) {
        if (allParams == null) return false;

        String vnpSecureHash = allParams.get("vnp_SecureHash");
        if (vnpSecureHash == null || vnpSecureHash.isBlank()) return false;

        String hashData = buildHashData(allParams);
        String calculated = hmacSHA512(hashSecret, hashData);

        return vnpSecureHash.equalsIgnoreCase(calculated);
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
