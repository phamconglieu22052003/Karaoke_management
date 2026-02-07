package com.karaoke_management.service;

import com.karaoke_management.entity.*;
import com.karaoke_management.enums.OrderStatus;
import com.karaoke_management.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final RoomSessionRepository roomSessionRepository; // bạn đã có

    @Transactional
    public Order getOrCreateOpenOrder(Long roomSessionId, String username) {
        return orderRepository
                .findFirstByRoomSession_IdAndStatusOrderByCreatedAtDesc(roomSessionId, OrderStatus.OPEN)
                .orElseGet(() -> {
                    RoomSession session = roomSessionRepository.findById(roomSessionId)
                            .orElseThrow(() -> new IllegalArgumentException("RoomSession not found: " + roomSessionId));

                    Order o = new Order();
                    o.setRoomSession(session);
                    o.setStatus(OrderStatus.OPEN);
                    o.setCreatedBy(username);
                    return orderRepository.save(o);
                });
    }

    @Transactional
    public void addItem(Long roomSessionId, Long productId, int quantity, String note, String username) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be > 0");
        Order order = getOrCreateOpenOrder(roomSessionId, username);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        if (!product.isActive()) throw new IllegalStateException("Product is inactive");

        // gộp dòng nếu cùng product
        OrderItem existing = order.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);

        if (existing == null) {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setUnitPrice(product.getPrice() == null ? BigDecimal.ZERO : product.getPrice());
            item.setNote(note);

            order.getItems().add(item);
            orderRepository.save(order); // cascade lưu item
        } else {
            existing.setQuantity(existing.getQuantity() + quantity);
            if (note != null && !note.isBlank()) existing.setNote(note);
            orderRepository.save(order);
        }
    }

    @Transactional
    public void updateItemQuantity(Long itemId, int newQty) {
        if (newQty <= 0) throw new IllegalArgumentException("Quantity must be > 0");
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("OrderItem not found: " + itemId));

        if (item.getOrder().getStatus() != OrderStatus.OPEN) {
            throw new IllegalStateException("Order is not OPEN");
        }

        item.setQuantity(newQty);
        orderItemRepository.save(item);
    }

    @Transactional
    public void removeItem(Long itemId) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("OrderItem not found: " + itemId));

        if (item.getOrder().getStatus() != OrderStatus.OPEN) {
            throw new IllegalStateException("Order is not OPEN");
        }

        orderItemRepository.delete(item);
    }

    @Transactional
    public void closeOrder(Long roomSessionId) {
        Order order = orderRepository
                .findFirstByRoomSession_IdAndStatusOrderByCreatedAtDesc(roomSessionId, OrderStatus.OPEN)
                .orElseThrow(() -> new IllegalArgumentException("No OPEN order for session: " + roomSessionId));

        order.setStatus(OrderStatus.CLOSED);
        orderRepository.save(order);
    }
}
