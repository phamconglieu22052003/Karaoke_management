package com.karaoke_management.enums;

public enum InventoryReceiptStatus {
    DRAFT("Nháp"),
    PENDING("Chờ duyệt"),
    APPROVED("Đã duyệt"),
    REJECTED("Từ chối");

    private final String label;

    InventoryReceiptStatus(String label) {
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
