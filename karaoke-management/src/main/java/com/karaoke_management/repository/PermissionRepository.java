package com.karaoke_management.repository;

import com.karaoke_management.entity.Permission;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByPermCode(String permCode);
}
