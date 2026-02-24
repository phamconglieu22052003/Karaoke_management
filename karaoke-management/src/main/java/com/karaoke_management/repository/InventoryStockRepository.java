package com.karaoke_management.repository;

import com.karaoke_management.entity.InventoryStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InventoryStockRepository extends JpaRepository<InventoryStock, Long> {

    Optional<InventoryStock> findByProduct_Id(Long productId);

    @Query("select s from InventoryStock s join fetch s.product p left join fetch p.category c order by p.name asc")
    List<InventoryStock> findAllWithProduct();
}
