package com.karaoke_management.entity;

public enum InvoiceStatus {
    UNPAID("Chưa thanh toán"),
    PAID("Đã thanh toán"),
    FAILED("Thanh toán thất bại");

    private final String label;

    InvoiceStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        // Giữ giá trị enum ổn định (phục vụ binding/form/DB). UI dùng getLabel() để hiển thị tiếng Việt.
        return name();
    }
}
