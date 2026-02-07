package com.karaoke_management.repository;

import com.karaoke_management.entity.Order;
import com.karaoke_management.enums.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {


    Optional<Order> findFirstByRoomSession_IdAndStatusOrderByCreatedAtDesc(Long roomSessionId, OrderStatus status);
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findTopByRoomSession_IdOrderByCreatedAtDesc(Long roomSessionId);
}
