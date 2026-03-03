package com.karaoke_management.entity;

public enum RoomStatus {
    AVAILABLE("Phòng trống"),   // phòng trống
    OCCUPIED("Đang sử dụng"),    // đang sử dụng (đã check-in)
    MAINTENANCE("Bảo trì");      // bảo trì (tuỳ chọn)

    private final String label;

    RoomStatus(String label) {
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
