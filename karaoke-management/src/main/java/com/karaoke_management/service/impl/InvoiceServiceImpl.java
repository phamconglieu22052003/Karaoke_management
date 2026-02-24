package com.karaoke_management.service.impl;

import com.karaoke_management.entity.Invoice;
import com.karaoke_management.entity.InvoiceStatus;
import com.karaoke_management.entity.InventoryReceipt;
import com.karaoke_management.entity.InventoryReceiptLine;
import com.karaoke_management.entity.InventoryStock;
import com.karaoke_management.entity.Order;
import com.karaoke_management.entity.OrderItem;
import com.karaoke_management.entity.RoomSession;
import com.karaoke_management.entity.Product;
import com.karaoke_management.entity.StockMovement;
import com.karaoke_management.entity.InvoiceLine;
import com.karaoke_management.enums.InventoryReceiptStatus;
import com.karaoke_management.enums.InventoryReceiptType;
import com.karaoke_management.enums.StockRefType;
import com.karaoke_management.enums.InvoiceLineType;
import com.karaoke_management.repository.InvoiceRepository;
import com.karaoke_management.repository.InvoiceLineRepository;
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
import java.math.BigDecimal;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final RoomSessionRepository roomSessionRepository;

    // inventory + order for auto deduction
    private final OrderRepository orderRepository;
    private final InventoryReceiptRepository inventoryReceiptRepository;
    private final InventoryStockRepository inventoryStockRepository;
    private final StockMovementRepository stockMovementRepository;

    public InvoiceServiceImpl(InvoiceRepository invoiceRepository,
                              InvoiceLineRepository invoiceLineRepository,
                              RoomSessionRepository roomSessionRepository,
                              OrderRepository orderRepository,
                              InventoryReceiptRepository inventoryReceiptRepository,
                              InventoryStockRepository inventoryStockRepository,
                              StockMovementRepository stockMovementRepository) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceLineRepository = invoiceLineRepository;
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
        // Nếu invoice đã tồn tại, đảm bảo totalAmount được cập nhật đúng (tiền phòng + tiền gọi món)
        // nhưng KHÔNG tự ý thay đổi invoice đã PAID để tránh sai lệch lịch sử.
        if (existed.isPresent()) {
            Invoice inv = existed.get();
            if (inv.getStatus() != InvoiceStatus.PAID) {
                // ✅ Snapshot lại chi tiết hóa đơn + cập nhật tổng tiền (tiền phòng + tiền món)
                BigDecimal refreshedTotal = rebuildSnapshotAndReturnTotal(inv, session);
                if (inv.getTotalAmount() == null || inv.getTotalAmount().compareTo(refreshedTotal) != 0) {
                    inv.setTotalAmount(refreshedTotal);
                }
                return invoiceRepository.save(inv);
            }
            return inv;
        }

        if (session.getRoom() == null || session.getRoom().getRoomType() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Thiếu thông tin loại phòng để tính tiền"
            );
        }

        // Tạo invoice trước để có invoice_id cho snapshot lines
        Invoice inv = new Invoice();
        inv.setRoomSession(session);
        inv.setStatus(InvoiceStatus.UNPAID);

        // FIX UNIQUE NULL (SQL Server unique không cho nhiều NULL)
        inv.setVnpTxnRef("INV-" + roomSessionId + "-" + System.currentTimeMillis());
        inv.setTotalAmount(BigDecimal.ZERO);
        inv = invoiceRepository.save(inv);

        // Snapshot lines + total
        BigDecimal totalAmount = rebuildSnapshotAndReturnTotal(inv, session);
        inv.setTotalAmount(totalAmount);
        return invoiceRepository.save(inv);
    }

    /**
     * Rebuild snapshot lines (ROOM + ITEM) cho invoice chưa PAID và trả về total = sum(lines.amount).
     */
    private BigDecimal rebuildSnapshotAndReturnTotal(Invoice inv, RoomSession session) {
        if (inv == null || inv.getId() == null || session == null) return BigDecimal.ZERO;

        // Xóa snapshot cũ (nếu có)
        invoiceLineRepository.deleteByInvoice_Id(inv.getId());

        BigDecimal roomCharge = calculateRoomCharge(session).setScale(0, RoundingMode.HALF_UP);

        // 1) ROOM line
        InvoiceLine roomLine = new InvoiceLine();
        roomLine.setInvoice(inv);
        roomLine.setLineType(InvoiceLineType.ROOM);

        long minutes = (session.getTotalMinutes() != null && session.getTotalMinutes() > 0)
                ? session.getTotalMinutes()
                : (session.getStartTime() != null && session.getEndTime() != null)
                    ? Duration.between(session.getStartTime(), session.getEndTime()).toMinutes()
                    : 0;
        if (minutes < 0) minutes = 0;

        String roomName = (session.getRoom() != null && session.getRoom().getName() != null)
                ? session.getRoom().getName() : "";
        roomLine.setDescription("Tiền phòng " + roomName + " (" + minutes + " phút)");
        roomLine.setQuantity((int) Math.min(Integer.MAX_VALUE, minutes));
        if (session.getRoom() != null && session.getRoom().getRoomType() != null) {
            roomLine.setUnitPrice(session.getRoom().getRoomType().getPricePerHour());
        }
        roomLine.setAmount(roomCharge);
        invoiceLineRepository.save(roomLine);

        // 2) ITEM lines (group by product + unitPrice + note)
        List<Order> orders = orderRepository.findAllByRoomSession_IdOrderByCreatedAtDesc(session.getId());
        Map<String, InvoiceLine> bucket = new LinkedHashMap<>();

        for (Order o : orders) {
            if (o == null || o.getItems() == null) continue;
            for (OrderItem it : o.getItems()) {
                if (it == null || it.getProduct() == null || it.getProduct().getId() == null) continue;
                int qty = (it.getQuantity() == null) ? 0 : it.getQuantity();
                if (qty <= 0) continue;

                BigDecimal unit = (it.getUnitPrice() != null) ? it.getUnitPrice()
                        : (it.getProduct().getPrice() != null ? it.getProduct().getPrice() : BigDecimal.ZERO);
                String note = (it.getNote() == null) ? "" : it.getNote().trim();
                String key = it.getProduct().getId() + "|" + unit.toPlainString() + "|" + note;

                InvoiceLine line = bucket.get(key);
                if (line == null) {
                    line = new InvoiceLine();
                    line.setInvoice(inv);
                    line.setLineType(InvoiceLineType.ITEM);
                    line.setProduct(it.getProduct());
                    line.setDescription(it.getProduct().getName() != null ? it.getProduct().getName() : "Sản phẩm");
                    line.setUnit(it.getProduct().getUnit());
                    line.setUnitPrice(unit);
                    line.setQuantity(0);
                    line.setAmount(BigDecimal.ZERO);
                    if (!note.isBlank()) line.setNote(note);
                    bucket.put(key, line);
                }

                line.setQuantity(line.getQuantity() + qty);
                line.setAmount(line.getAmount().add(unit.multiply(BigDecimal.valueOf(qty))));
            }
        }

        BigDecimal itemSum = BigDecimal.ZERO;
        for (InvoiceLine l : bucket.values()) {
            l.setAmount(l.getAmount().setScale(0, RoundingMode.HALF_UP));
            invoiceLineRepository.save(l);
            itemSum = itemSum.add(l.getAmount());
        }

        return roomCharge.add(itemSum).setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTotal(RoomSession session) {
        if (session == null || session.getId() == null) return BigDecimal.ZERO;

        BigDecimal roomCharge = calculateRoomCharge(session);
        BigDecimal orderCharge = calculateOrderCharge(session.getId());

        // Tổng = tiền phòng + tiền gọi món
        // VND => làm tròn 0 chữ số để đồng nhất UI (format 0 decimal) và VNPay (longValue)
        return roomCharge
                .add(orderCharge)
                .setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Tính tiền phòng theo thời gian (VND, làm tròn 0 chữ số).
     */
    private BigDecimal calculateRoomCharge(RoomSession session) {
        if (session == null || session.getRoom() == null || session.getRoom().getRoomType() == null) {
            return BigDecimal.ZERO;
        }

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

    /**
     * Tính tiền gọi món (tổng order_items của TẤT CẢ order thuộc session).
     * Bao gồm cả OPEN/CLOSED để tránh trường hợp nhân viên đã "chốt" rồi thoát ra vào lại.
     * VND => làm tròn 0 chữ số.
     */
    private BigDecimal calculateOrderCharge(Long roomSessionId) {
        if (roomSessionId == null) return BigDecimal.ZERO;

        List<Order> orders = orderRepository.findAllByRoomSession_IdOrderByCreatedAtDesc(roomSessionId);
        if (orders == null || orders.isEmpty()) return BigDecimal.ZERO;

        BigDecimal sum = BigDecimal.ZERO;

        for (Order o : orders) {
            if (o == null || o.getItems() == null) continue;
            for (OrderItem it : o.getItems()) {
                if (it == null) continue;

                BigDecimal line = it.getLineAmount();
                if (line == null) {
                    BigDecimal unit = it.getUnitPrice();
                    Integer qty = it.getQuantity();
                    if (unit == null || qty == null) continue;
                    if (qty <= 0) continue;
                    line = unit.multiply(BigDecimal.valueOf(qty));
                }
                sum = sum.add(line);
            }
        }

        return sum.setScale(0, RoundingMode.HALF_UP);
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

            // ✅ FIX: Trừ kho theo TẤT CẢ order của session (OPEN + CLOSED)
            // Trước đây chỉ lấy order OPEN => nếu nhân viên đã "chốt" (CLOSED) trước khi thanh toán
            // thì sẽ KHÔNG trừ kho nhưng vẫn set inventoryDeducted=true.

            List<Order> orders = orderRepository.findAllByRoomSession_IdOrderByCreatedAtDesc(sessionId);

            // gom qty theo product (giữ thứ tự ổn định)
            Map<Long, Integer> qtyByProductId = new LinkedHashMap<>();
            Map<Long, Product> productById = new LinkedHashMap<>();

            for (Order order : orders) {
                if (order == null) continue;
                if (order.getItems() == null) continue;

                for (OrderItem it : order.getItems()) {
                    if (it == null || it.getProduct() == null || it.getProduct().getId() == null) continue;
                    int q = (it.getQuantity() == null) ? 0 : it.getQuantity();
                    if (q <= 0) continue;
                    Long pid = it.getProduct().getId();
                    qtyByProductId.put(pid, qtyByProductId.getOrDefault(pid, 0) + q);
                    productById.putIfAbsent(pid, it.getProduct());
                }
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

            inv.setInventoryDeducted(true);
            inv.setInventoryDeductedAt(LocalDateTime.now());
        }

        return invoiceRepository.save(inv);
    }
}
