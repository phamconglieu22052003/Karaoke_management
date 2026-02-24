package com.karaoke_management.repository;

import com.karaoke_management.entity.InventoryReceipt;
import com.karaoke_management.enums.InventoryReceiptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InventoryReceiptRepository extends JpaRepository<InventoryReceipt, Long> {

    List<InventoryReceipt> findByStatusOrderByCreatedAtDesc(InventoryReceiptStatus status);

    List<InventoryReceipt> findAllByOrderByCreatedAtDesc();

    @Query("select r from InventoryReceipt r left join fetch r.createdBy cb left join fetch r.approvedBy ab where r.id = ?1")
    InventoryReceipt findHeaderById(Long id);
}
