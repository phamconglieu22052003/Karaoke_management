package com.karaoke_management.repository;

import com.karaoke_management.entity.Order;
import com.karaoke_management.entity.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"items", "items.product", "items.product.category", "roomSession", "roomSession.room"})
    Optional<Order> findFirstByRoomSession_IdAndStatusOrderByCreatedAtDesc(Long roomSessionId, OrderStatus status);

    @EntityGraph(attributePaths = {"items", "items.product", "items.product.category", "roomSession", "roomSession.room"})
    Optional<Order> findFirstByRoomSession_IdOrderByCreatedAtDesc(Long roomSessionId);

    @EntityGraph(attributePaths = {"items", "items.product", "items.product.category"})
    List<Order> findAllByRoomSession_IdOrderByCreatedAtDesc(Long roomSessionId);

    @EntityGraph(attributePaths = {"items", "items.product", "items.product.category"})
    Optional<Order> findTopByRoomSession_IdOrderByCreatedAtDesc(Long roomSessionId);
    
}
