package com.karaoke_management.service.impl;

import com.karaoke_management.entity.Invoice;
import com.karaoke_management.entity.InvoiceStatus;
import com.karaoke_management.entity.InventoryReceipt;
import com.karaoke_management.entity.InventoryReceiptLine;
import com.karaoke_management.entity.InventoryStock;
import com.karaoke_management.entity.Order;
import com.karaoke_management.entity.OrderItem;
import com.karaoke_management.entity.OrderStatus;
import com.karaoke_management.entity.RoomSession;
import com.karaoke_management.entity.Product;
import com.karaoke_management.entity.StockMovement;
import com.karaoke_management.enums.InventoryReceiptStatus;
import com.karaoke_management.enums.InventoryReceiptType;
import com.karaoke_management.enums.StockRefType;
import com.karaoke_management.repository.InvoiceRepository;
import com.karaoke_management.repository.InventoryReceiptRepository;
import com.karaoke_management.repository.InventoryStockRepository;
import com.karaoke_management.repository.OrderRepository;
import com.karaoke_management.repository.StockMovementRepository;
import com.karaoke_management.repository.RoomSessionRepository;
import com.karaoke_management.service.InvoiceService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Sort;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final RoomSessionRepository roomSessionRepository;

    // inventory + order for auto deduction
    private final OrderRepository orderRepository;
    private final InventoryReceiptRepository inventoryReceiptRepository;
    private final InventoryStockRepository inventoryStockRepository;
    private final StockMovementRepository stockMovementRepository;

    public InvoiceServiceImpl(InvoiceRepository invoiceRepository,
                              RoomSessionRepository roomSessionRepository,
                              OrderRepository orderRepository,
                              InventoryReceiptRepository inventoryReceiptRepository,
                              InventoryStockRepository inventoryStockRepository,
                              StockMovementRepository stockMovementRepository) {
        this.invoiceRepository = invoiceRepository;
        this.roomSessionRepository = roomSessionRepository;
        this.orderRepository = orderRepository;
        this.inventoryReceiptRepository = inventoryReceiptRepository;
        this.inventoryStockRepository = inventoryStockRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    @Override
    public List<Invoice> findAll() {
        return invoiceRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @Override
    public Invoice getRequired(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy hóa đơn ID = " + id
                ));
    }

    @Override
    @Transactional
    public Invoice createOrGetBySession(Long roomSessionId) {

        RoomSession session = roomSessionRepository.findById(roomSessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy phiên phòng ID = " + roomSessionId
                ));

        // chỉ tạo hóa đơn khi đã checkout
        if (session.getEndTime() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Phiên phòng chưa kết thúc, không thể tạo hóa đơn"
            );
        }

        Optional<Invoice> existed = invoiceRepository.findByRoomSessionId(roomSessionId);
        if (existed.isPresent()) return existed.get();

        if (session.getRoom() == null || session.getRoom().getRoomType() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Thiếu thông tin loại phòng để tính tiền"
            );
        }

        BigDecimal totalAmount = calculateTotal(session);

        Invoice inv = new Invoice();
        inv.setRoomSession(session);
        inv.setTotalAmount(totalAmount);
        inv.setStatus(InvoiceStatus.UNPAID);

        // ✅ FIX UNIQUE NULL (SQL Server unique không cho nhiều NULL)
        // Đảm bảo vnpTxnRef luôn có giá trị và unique
        inv.setVnpTxnRef("INV-" + roomSessionId + "-" + System.currentTimeMillis());

        return invoiceRepository.save(inv);
    }

    private BigDecimal calculateTotal(RoomSession session) {
        BigDecimal pricePerHour = session.getRoom().getRoomType().getPricePerHour();
        if (pricePerHour == null) return BigDecimal.ZERO;

        long minutes;
        if (session.getTotalMinutes() != null && session.getTotalMinutes() > 0) {
            minutes = session.getTotalMinutes();
        } else {
            if (session.getStartTime() == null || session.getEndTime() == null) return BigDecimal.ZERO;
            minutes = Duration.between(session.getStartTime(), session.getEndTime()).toMinutes();
        }
        if (minutes < 0) minutes = 0;

        return pricePerHour
                .multiply(BigDecimal.valueOf(minutes))
                .divide(BigDecimal.valueOf(60), 0, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional
    public Invoice markPaidAndDeductInventory(Long invoiceId, String paymentMethod, String transactionNo) {
        Invoice inv = getRequired(invoiceId);

        // 1) Mark PAID (idempotent)
        if (inv.getStatus() != InvoiceStatus.PAID) {
            inv.setStatus(InvoiceStatus.PAID);
        }
        if (inv.getPaidAt() == null) {
            inv.setPaidAt(LocalDateTime.now());
        }
        if (paymentMethod != null && !paymentMethod.isBlank()) {
            inv.setPaymentMethod(paymentMethod);
        }
        if (transactionNo != null && !transactionNo.isBlank()) {
            inv.setVnpTransactionNo(transactionNo);
        }

        // 2) Deduct inventory only once
        if (!inv.isInventoryDeducted()) {
            Long sessionId = (inv.getRoomSession() != null) ? inv.getRoomSession().getId() : null;
            if (sessionId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invoice thiếu room session");
            }

            // lấy order OPEN gần nhất của session
            Optional<Order> orderOpt = orderRepository
                    .findFirstByRoomSession_IdAndStatusOrderByCreatedAtDesc(sessionId, OrderStatus.OPEN);

            // nếu không có order -> vẫn đánh dấu deducted để tránh chạy lại
            if (orderOpt.isPresent()) {
                Order order = orderOpt.get();

                // gom qty theo product (giữ thứ tự ổn định)
                Map<Long, Integer> qtyByProductId = new LinkedHashMap<>();
                Map<Long, Product> productById = new LinkedHashMap<>();

                for (OrderItem it : order.getItems()) {
                    if (it == null || it.getProduct() == null || it.getProduct().getId() == null) continue;
                    int q = (it.getQuantity() == null) ? 0 : it.getQuantity();
                    if (q <= 0) continue;
                    Long pid = it.getProduct().getId();
                    qtyByProductId.put(pid, qtyByProductId.getOrDefault(pid, 0) + q);
                    productById.putIfAbsent(pid, it.getProduct());
                }

                if (!qtyByProductId.isEmpty()) {
                    // 2.1) Check tồn trước khi trừ (không để âm)
                    for (var e : qtyByProductId.entrySet()) {
                        Long pid = e.getKey();
                        int q = e.getValue();
                        InventoryStock s = inventoryStockRepository.findByProduct_Id(pid)
                                .orElseThrow(() -> new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Chưa có tồn kho cho sản phẩm: " + productById.get(pid).getName()
                                ));
                        int onHand = (s.getQtyOnHand() == null) ? 0 : s.getQtyOnHand();
                        if (onHand - q < 0) {
                            throw new ResponseStatusException(
                                    HttpStatus.BAD_REQUEST,
                                    "Không đủ tồn kho cho sản phẩm: " + productById.get(pid).getName()
                            );
                        }
                    }

                    // 2.2) Tạo phiếu xuất kho auto (APPROVED)
                    InventoryReceipt receipt = new InventoryReceipt();
                    receipt.setType(InventoryReceiptType.OUT);
                    receipt.setStatus(InventoryReceiptStatus.APPROVED);
                    receipt.setNote("Xuất kho tự động theo hóa đơn #" + invoiceId);
                    receipt.setCreatedAt(LocalDateTime.now());
                    receipt.setApprovedAt(LocalDateTime.now());

                    for (var e : qtyByProductId.entrySet()) {
                        Long pid = e.getKey();
                        int q = e.getValue();
                        Product p = productById.get(pid);

                        InventoryReceiptLine line = new InventoryReceiptLine();
                        line.setProduct(p);
                        line.setQty(q);
                        line.setUnitPrice(null);
                        receipt.addLine(line);
                    }

                    receipt = inventoryReceiptRepository.save(receipt);
                    inv.setInventoryReceiptId(receipt.getId());

                    // 2.3) Apply stock + movements (ref = INVOICE)
                    for (var e : qtyByProductId.entrySet()) {
                        Long pid = e.getKey();
                        int q = e.getValue();
                        Product p = productById.get(pid);

                        InventoryStock s = inventoryStockRepository.findByProduct_Id(pid)
                                .orElseThrow(() -> new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Chưa có tồn kho cho sản phẩm: " + p.getName()
                                ));

                        int onHand = (s.getQtyOnHand() == null) ? 0 : s.getQtyOnHand();
                        s.setQtyOnHand(onHand - q);
                        inventoryStockRepository.save(s);

                        StockMovement m = new StockMovement();
                        m.setProduct(p);
                        m.setChangeQty(-q);
                        m.setRefType(StockRefType.INVOICE);
                        m.setRefId(invoiceId);
                        m.setNote("Bán hàng - Hóa đơn #" + invoiceId);
                        stockMovementRepository.save(m);
                    }
                }
            }

            inv.setInventoryDeducted(true);
            inv.setInventoryDeductedAt(LocalDateTime.now());
        }

        return invoiceRepository.save(inv);
    }
}
