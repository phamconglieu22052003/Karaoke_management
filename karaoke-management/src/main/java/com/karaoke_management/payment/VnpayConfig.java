package com.karaoke_management.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vnpay")
public class VnpayConfig {

    private String payUrl;
    private String returnUrl;
    private String tmnCode;
    private String hashSecret;

    // default theo VNPay
    private String version = "2.1.0";
    private String command = "pay";
    private String locale = "vn";

    // optional
    private String bankCode;

    public String getPayUrl() {
        return payUrl;
    }

    public void setPayUrl(String payUrl) {
        this.payUrl = payUrl;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public String getTmnCode() {
        return tmnCode;
    }

    public void setTmnCode(String tmnCode) {
        this.tmnCode = tmnCode;
    }

    public String getHashSecret() {
        return hashSecret;
    }

    public void setHashSecret(String hashSecret) {
        this.hashSecret = hashSecret;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        if (version != null && !version.isBlank()) this.version = version;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        if (command != null && !command.isBlank()) this.command = command;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        if (locale != null && !locale.isBlank()) this.locale = locale;
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }
}
