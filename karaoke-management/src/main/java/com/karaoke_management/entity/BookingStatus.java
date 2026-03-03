package com.karaoke_management.entity;

public enum BookingStatus {
    BOOKED("Đã đặt"),
    CONFIRMED("Đã xác nhận"),
    CANCELLED("Đã hủy");

    private final String label;

    BookingStatus(String label) {
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
