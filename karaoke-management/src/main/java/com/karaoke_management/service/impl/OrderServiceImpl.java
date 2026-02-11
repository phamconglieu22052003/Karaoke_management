package com.karaoke_management.service.impl;

import com.karaoke_management.entity.Order;
import com.karaoke_management.entity.OrderItem;
import com.karaoke_management.entity.Product;
import com.karaoke_management.entity.RoomSession;
import com.karaoke_management.entity.OrderStatus;
import com.karaoke_management.repository.OrderItemRepository;
import com.karaoke_management.repository.OrderRepository;
import com.karaoke_management.repository.ProductRepository;
import com.karaoke_management.repository.RoomSessionRepository;
import com.karaoke_management.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final RoomSessionRepository roomSessionRepository;

    @Override
    @Transactional
    public Order getOrCreateOpenOrder(Long roomSessionId, String username) {

        // 1) nếu đã có OPEN thì lấy
        var openOpt = orderRepository.findFirstByRoomSession_IdAndStatusOrderByCreatedAtDesc(roomSessionId, OrderStatus.OPEN);
        if (openOpt.isPresent()) {
            return openOpt.get();
        }

        // 2) nếu chưa có OPEN -> tạo mới
        RoomSession session = roomSessionRepository.findById(roomSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy session: " + roomSessionId));

        Order order = new Order();
        order.setRoomSession(session);
        order.setStatus(OrderStatus.OPEN);
        order.setCreatedAt(LocalDateTime.now());
        order.setCreatedBy(username);

        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public void addItem(Long roomSessionId, Long productId, Integer quantity, String note, String username) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Số lượng phải > 0");
        }

        Order order = getOrCreateOpenOrder(roomSessionId, username);

        if (order.getStatus() != OrderStatus.OPEN) {
            throw new IllegalArgumentException("Order đã chốt, không thể thêm món.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm: " + productId));

        // Nếu món đã có trong order => cộng dồn
        OrderItem existing = null;
        if (order.getItems() != null) {
            for (OrderItem it : order.getItems()) {
                if (it.getProduct() != null && it.getProduct().getId().equals(productId)) {
                    existing = it;
                    break;
                }
            }
        }

        BigDecimal unitPrice = safeBig(product.getPrice());
        BigDecimal qty = BigDecimal.valueOf(quantity);

        if (existing != null) {
            int newQty = existing.getQuantity() + quantity;
            existing.setQuantity(newQty);

            // note: nếu user nhập note mới thì append
            if (note != null && !note.trim().isEmpty()) {
                String old = existing.getNote();
                if (old == null || old.trim().isEmpty()) existing.setNote(note.trim());
                else existing.setNote(old.trim() + " | " + note.trim());
            }

            existing.setUnitPrice(unitPrice);
            existing.setLineAmount(unitPrice.multiply(BigDecimal.valueOf(existing.getQuantity())));

            orderItemRepository.save(existing);
            return;
        }

        // tạo item mới
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setNote((note == null) ? null : note.trim());
        item.setUnitPrice(unitPrice);
        item.setLineAmount(unitPrice.multiply(qty));

        orderItemRepository.save(item);

        // nếu mapping Order.items không auto add, thì add thủ công để view hiển thị ngay
        if (order.getItems() != null) {
            order.getItems().add(item);
        }
    }

    @Override
    @Transactional
    public void updateItem(Long itemId, Integer quantity, String note) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Số lượng phải > 0");
        }

        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy món trong order: " + itemId));

        Order order = item.getOrder();
        if (order == null) {
            throw new IllegalArgumentException("Order của item không hợp lệ.");
        }
        if (order.getStatus() != OrderStatus.OPEN) {
            throw new IllegalArgumentException("Order đã chốt, không thể sửa.");
        }

        item.setQuantity(quantity);

        // Update note (có thể cho phép rỗng để xóa note)
        if (note != null) {
            String trimmed = note.trim();
            item.setNote(trimmed.isEmpty() ? null : trimmed);
        }

        BigDecimal unitPrice = safeBig(item.getUnitPrice());
        item.setLineAmount(unitPrice.multiply(BigDecimal.valueOf(quantity)));

        orderItemRepository.save(item);
    }

    @Override
    @Transactional
    public void removeItem(Long itemId) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy món trong order: " + itemId));

        Order order = item.getOrder();
        if (order != null && order.getStatus() != OrderStatus.OPEN) {
            throw new IllegalArgumentException("Order đã chốt, không thể xóa món.");
        }

        // remove khỏi collection nếu có
        if (order != null && order.getItems() != null) {
            order.getItems().removeIf(x -> x.getId() != null && x.getId().equals(itemId));
        }

        orderItemRepository.deleteById(itemId);
    }

    @Override
    @Transactional
    public void closeOpenOrder(Long roomSessionId, String username) {
        Order order = orderRepository.findFirstByRoomSession_IdAndStatusOrderByCreatedAtDesc(roomSessionId, OrderStatus.OPEN)
                .orElseThrow(() -> new IllegalArgumentException("Chưa có order OPEN cho session: " + roomSessionId));

        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order chưa có món, không thể chốt.");
        }

        order.setStatus(OrderStatus.CLOSED);
        // nếu bạn có field updatedBy/closedBy/closedAt thì set ở đây
        // order.setClosedAt(LocalDateTime.now());
        // order.setClosedBy(username);

        orderRepository.save(order);
    }

    private BigDecimal safeBig(BigDecimal v) {
        return (v == null) ? BigDecimal.ZERO : v;
    }
}
