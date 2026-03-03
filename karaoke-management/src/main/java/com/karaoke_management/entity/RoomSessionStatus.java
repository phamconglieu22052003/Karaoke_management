package com.karaoke_management.entity;

public enum RoomSessionStatus {
    ACTIVE("Đang hoạt động"),
    OPEN("Đang mở"),
    CLOSED("Đã đóng");

    private final String label;

    RoomSessionStatus(String label) {
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
