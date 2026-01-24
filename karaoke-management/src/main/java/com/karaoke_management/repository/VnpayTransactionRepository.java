package com.karaoke_management.repository;

import com.karaoke_management.entity.VnpayTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VnpayTransactionRepository extends JpaRepository<VnpayTransaction, Long> {
}
