package com.karaoke_management.enums;

public enum StockRefType {
    INVENTORY_RECEIPT("Phiếu kho"),
    POS_ORDER("Đơn hàng POS"),
    INVOICE("Hóa đơn");

    private final String label;

    StockRefType(String label) {
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
