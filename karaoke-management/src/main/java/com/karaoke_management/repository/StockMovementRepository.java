package com.karaoke_management.repository;

import com.karaoke_management.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    @Query("select m from StockMovement m join fetch m.product p left join fetch p.category c order by m.createdAt desc")
    List<StockMovement> findAllWithProduct();

    @Query("select m from StockMovement m join fetch m.product p left join fetch p.category c where p.id = ?1 order by m.createdAt desc")
    List<StockMovement> findByProductIdWithProduct(Long productId);
}
