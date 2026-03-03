package com.karaoke_management.entity;

public enum OrderStatus {
    OPEN("Đang mở"),
    CLOSED("Đã đóng");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return name();
    }
}
