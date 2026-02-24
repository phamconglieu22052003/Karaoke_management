package com.karaoke_management.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "inventory_stocks", uniqueConstraints = {
        @UniqueConstraint(name = "uk_inventory_stock_product", columnNames = {"product_id"})
})
public class InventoryStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "qty_on_hand", nullable = false)
    private Integer qtyOnHand = 0;

    @Column(name = "min_qty", nullable = false)
    private Integer minQty = 0;

    @PrePersist
    public void prePersist() {
        if (qtyOnHand == null) qtyOnHand = 0;
        if (minQty == null) minQty = 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Integer getQtyOnHand() {
        return qtyOnHand;
    }

    public void setQtyOnHand(Integer qtyOnHand) {
        this.qtyOnHand = qtyOnHand;
    }

    public Integer getMinQty() {
        return minQty;
    }

    public void setMinQty(Integer minQty) {
        this.minQty = minQty;
    }
}
