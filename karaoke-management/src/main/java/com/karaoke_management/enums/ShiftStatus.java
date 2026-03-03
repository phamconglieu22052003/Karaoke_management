package com.karaoke_management.enums;

/**
 * Trạng thái ca làm việc của thu ngân.
 */
public enum ShiftStatus {
    OPEN("Đang mở"),
    CLOSED("Đã đóng");

    private final String label;

    ShiftStatus(String label) {
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
