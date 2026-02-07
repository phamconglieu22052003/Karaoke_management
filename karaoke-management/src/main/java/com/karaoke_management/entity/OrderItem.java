package com.karaoke_management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @Column(length = 255)
    private String note;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal lineAmount;

    @PrePersist @PreUpdate
    public void calc() {
        if (quantity == null) quantity = 0;
        if (unitPrice == null) unitPrice = BigDecimal.ZERO;
        lineAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
