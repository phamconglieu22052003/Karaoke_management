package com.karaoke_management.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vnpay")
public class VnpayConfig {

    private String payUrl;      // https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
    private String returnUrl;   // ví dụ: http://localhost:8080/payment/vnpay/return
    private String ipnUrl;      // ví dụ: https://<public-domain>/payment/vnpay/ipn (localhost không được VNPay gọi)
    private String tmnCode;
    private String hashSecret;

    // default theo spec
    private String version = "2.1.0";
    private String command = "pay";
    private String locale = "vn";
    private String currCode = "VND";

    // optional
    private String bankCode;

    // ===== getter/setter =====
    public String getPayUrl() { return payUrl; }
    public void setPayUrl(String payUrl) { this.payUrl = payUrl; }

    public String getReturnUrl() { return returnUrl; }
    public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }

    public String getIpnUrl() { return ipnUrl; }
    public void setIpnUrl(String ipnUrl) { this.ipnUrl = ipnUrl; }

    public String getTmnCode() { return tmnCode; }
    public void setTmnCode(String tmnCode) { this.tmnCode = tmnCode; }

    public String getHashSecret() { return hashSecret; }
    public void setHashSecret(String hashSecret) { this.hashSecret = hashSecret; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }

    public String getCurrCode() { return currCode; }
    public void setCurrCode(String currCode) { this.currCode = currCode; }

    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
}
