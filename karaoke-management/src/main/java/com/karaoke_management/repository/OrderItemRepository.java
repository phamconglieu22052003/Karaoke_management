package com.karaoke_management.repository;

import com.karaoke_management.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    Optional<OrderItem> findById(Long id);
}
