package com.karaoke_management.repository;

import com.karaoke_management.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByActiveTrueOrderByNameAsc();

    @Query("select p from Product p left join fetch p.category c where p.active = true order by p.name asc")
    List<Product> findActiveWithCategory();
}
