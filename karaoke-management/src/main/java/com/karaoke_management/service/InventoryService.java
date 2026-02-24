package com.karaoke_management.service;

import com.karaoke_management.entity.InventoryReceipt;
import com.karaoke_management.entity.InventoryStock;
import com.karaoke_management.entity.StockMovement;
import com.karaoke_management.enums.InventoryReceiptType;

import java.math.BigDecimal;
import java.util.List;

public interface InventoryService {

    List<InventoryStock> listStocks();

    InventoryStock updateMinQty(Long stockId, Integer minQty);

    InventoryReceipt createOrUpdateReceipt(Long receiptId,
                                          InventoryReceiptType type,
                                          String note,
                                          Long createdByUserId,
                                          List<Long> productIds,
                                          List<Integer> qtys,
                                          List<BigDecimal> unitPrices,
                                          boolean submit);

    InventoryReceipt getReceiptWithLines(Long receiptId);

    void submitReceipt(Long receiptId, Long userId);

    void approveReceipt(Long receiptId, Long approverUserId);

    void rejectReceipt(Long receiptId, Long approverUserId, String reason);

    List<InventoryReceipt> listReceipts(String status);

    List<StockMovement> listMovements(Long productId);
}
