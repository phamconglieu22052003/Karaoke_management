package com.karaoke_management.enums;

/**
 * Loại dòng chi tiết hóa đơn (snapshot).
 */
public enum InvoiceLineType {
    ROOM("Phòng"),
    ITEM("Hàng hóa/Dịch vụ");

    private final String label;

    InvoiceLineType(String label) {
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
