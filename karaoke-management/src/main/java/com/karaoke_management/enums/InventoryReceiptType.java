package com.karaoke_management.enums;

public enum InventoryReceiptType {
    IN("Nhập kho"),
    OUT("Xuất kho");

    private final String label;

    InventoryReceiptType(String label) {
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
