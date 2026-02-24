package com.karaoke_management.service.impl;

import com.karaoke_management.entity.*;
import com.karaoke_management.enums.InventoryReceiptStatus;
import com.karaoke_management.enums.InventoryReceiptType;
import com.karaoke_management.enums.StockRefType;
import com.karaoke_management.repository.*;
import com.karaoke_management.service.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryStockRepository stockRepository;
    private final InventoryReceiptRepository receiptRepository;
    private final StockMovementRepository movementRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public InventoryServiceImpl(InventoryStockRepository stockRepository,
                                InventoryReceiptRepository receiptRepository,
                                StockMovementRepository movementRepository,
                                ProductRepository productRepository,
                                UserRepository userRepository) {
        this.stockRepository = stockRepository;
        this.receiptRepository = receiptRepository;
        this.movementRepository = movementRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<InventoryStock> listStocks() {
        // Ensure every product has a stock row (lazy init)
        List<Product> products = productRepository.findAll();
        for (Product p : products) {
            stockRepository.findByProduct_Id(p.getId()).orElseGet(() -> {
                InventoryStock s = new InventoryStock();
                s.setProduct(p);
                s.setQtyOnHand(0);
                s.setMinQty(0);
                return stockRepository.save(s);
            });
        }
        return stockRepository.findAllWithProduct();
    }

    @Override
    @Transactional
    public InventoryStock updateMinQty(Long stockId, Integer minQty) {
        InventoryStock s = stockRepository.findById(stockId)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found"));
        s.setMinQty(minQty == null ? 0 : Math.max(0, minQty));
        return stockRepository.save(s);
    }

    @Override
    @Transactional
    public InventoryReceipt createOrUpdateReceipt(Long receiptId,
                                                  InventoryReceiptType type,
                                                  String note,
                                                  Long createdByUserId,
                                                  List<Long> productIds,
                                                  List<Integer> qtys,
                                                  List<BigDecimal> unitPrices,
                                                  boolean submit) {

        if (type == null) throw new IllegalArgumentException("Type is required");

        User creator = userRepository.findById(createdByUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        InventoryReceipt receipt;
        if (receiptId == null) {
            receipt = new InventoryReceipt();
            receipt.setType(type);
            receipt.setCreatedBy(creator);
            receipt.setStatus(InventoryReceiptStatus.DRAFT);
        } else {
            receipt = receiptRepository.findById(receiptId)
                    .orElseThrow(() -> new IllegalArgumentException("Receipt not found"));
            if (receipt.getStatus() != InventoryReceiptStatus.DRAFT) {
                throw new IllegalStateException("Chỉ được sửa phiếu ở trạng thái DRAFT");
            }
            receipt.setType(type);
        }

        receipt.setNote(note);
        receipt.setRejectedReason(null);

        // Replace lines
        receipt.clearLines();
        List<InventoryReceiptLine> lines = buildLines(productIds, qtys, unitPrices);
        for (InventoryReceiptLine l : lines) {
            receipt.addLine(l);
        }

        if (submit) {
            receipt.setStatus(InventoryReceiptStatus.PENDING);
        }

        return receiptRepository.save(receipt);
    }

    private List<InventoryReceiptLine> buildLines(List<Long> productIds,
                                                  List<Integer> qtys,
                                                  List<BigDecimal> unitPrices) {
        List<InventoryReceiptLine> res = new ArrayList<>();
        if (productIds == null || qtys == null) return res;
        for (int i = 0; i < productIds.size(); i++) {
            Long pid = productIds.get(i);
            if (pid == null) continue;
            Integer q = (i < qtys.size()) ? qtys.get(i) : null;
            if (q == null || q <= 0) continue;
            BigDecimal up = (unitPrices != null && i < unitPrices.size()) ? unitPrices.get(i) : null;
            Product p = productRepository.findById(pid)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + pid));

            InventoryReceiptLine line = new InventoryReceiptLine();
            line.setProduct(p);
            line.setQty(q);
            line.setUnitPrice(up);
            res.add(line);
        }
        return res;
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryReceipt getReceiptWithLines(Long receiptId) {
        InventoryReceipt r = receiptRepository.findHeaderById(receiptId);
        if (r == null) throw new IllegalArgumentException("Receipt not found");
        // init lines + product
        r.getLines().size();
        for (InventoryReceiptLine l : r.getLines()) {
            l.getProduct().getName();
        }
        return r;
    }

    @Override
    @Transactional
    public void submitReceipt(Long receiptId, Long userId) {
        InventoryReceipt r = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new IllegalArgumentException("Receipt not found"));
        if (r.getStatus() != InventoryReceiptStatus.DRAFT) {
            throw new IllegalStateException("Chỉ được gửi duyệt phiếu ở trạng thái DRAFT");
        }
        if (r.getLines() == null || r.getLines().isEmpty()) {
            throw new IllegalStateException("Phiếu phải có ít nhất 1 dòng hàng");
        }
        r.setStatus(InventoryReceiptStatus.PENDING);
        receiptRepository.save(r);
    }

    @Override
    @Transactional
    public void approveReceipt(Long receiptId, Long approverUserId) {
        InventoryReceipt r = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new IllegalArgumentException("Receipt not found"));
        if (r.getStatus() != InventoryReceiptStatus.PENDING) {
            throw new IllegalStateException("Chỉ được duyệt phiếu ở trạng thái PENDING");
        }

        User approver = userRepository.findById(approverUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Apply stock + movements
        for (InventoryReceiptLine line : r.getLines()) {
            Product p = line.getProduct();
            InventoryStock s = stockRepository.findByProduct_Id(p.getId()).orElseGet(() -> {
                InventoryStock ns = new InventoryStock();
                ns.setProduct(p);
                ns.setQtyOnHand(0);
                ns.setMinQty(0);
                return stockRepository.save(ns);
            });

            int delta = (r.getType() == InventoryReceiptType.IN) ? line.getQty() : -line.getQty();
            int newQty = (s.getQtyOnHand() == null ? 0 : s.getQtyOnHand()) + delta;
            if (newQty < 0) {
                throw new IllegalStateException("Không đủ tồn kho cho sản phẩm: " + p.getName());
            }
            s.setQtyOnHand(newQty);
            stockRepository.save(s);

            StockMovement m = new StockMovement();
            m.setProduct(p);
            m.setChangeQty(delta);
            m.setRefType(StockRefType.INVENTORY_RECEIPT);
            m.setRefId(r.getId());
            m.setCreatedBy(approver);
            m.setNote(r.getType() == InventoryReceiptType.IN ? "Nhập kho" : "Xuất kho");
            movementRepository.save(m);
        }

        r.setStatus(InventoryReceiptStatus.APPROVED);
        r.setApprovedBy(approver);
        r.setApprovedAt(LocalDateTime.now());
        r.setRejectedReason(null);
        receiptRepository.save(r);
    }

    @Override
    @Transactional
    public void rejectReceipt(Long receiptId, Long approverUserId, String reason) {
        InventoryReceipt r = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new IllegalArgumentException("Receipt not found"));
        if (r.getStatus() != InventoryReceiptStatus.PENDING) {
            throw new IllegalStateException("Chỉ được từ chối phiếu ở trạng thái PENDING");
        }
        User approver = userRepository.findById(approverUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        r.setStatus(InventoryReceiptStatus.REJECTED);
        r.setApprovedBy(approver);
        r.setApprovedAt(LocalDateTime.now());
        r.setRejectedReason(reason);
        receiptRepository.save(r);
    }

    @Override
    public List<InventoryReceipt> listReceipts(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return receiptRepository.findAllByOrderByCreatedAtDesc();
        }
        InventoryReceiptStatus st = InventoryReceiptStatus.valueOf(status.toUpperCase());
        return receiptRepository.findByStatusOrderByCreatedAtDesc(st);
    }

    @Override
    public List<StockMovement> listMovements(Long productId) {
        if (productId == null) {
            return movementRepository.findAllWithProduct();
        }
        return movementRepository.findByProductIdWithProduct(productId);
    }
}
