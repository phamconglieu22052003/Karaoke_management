package com.karaoke_management.entity;

import com.karaoke_management.enums.InventoryReceiptStatus;
import com.karaoke_management.enums.InventoryReceiptType;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inventory_receipts")
public class InventoryReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private InventoryReceiptType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private InventoryReceiptStatus status = InventoryReceiptStatus.DRAFT;

    @Column(length = 500)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_reason", length = 500)
    private String rejectedReason;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InventoryReceiptLine> lines = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = InventoryReceiptStatus.DRAFT;
    }

    public void addLine(InventoryReceiptLine line) {
        line.setReceipt(this);
        this.lines.add(line);
    }

    public void clearLines() {
        for (InventoryReceiptLine l : lines) {
            l.setReceipt(null);
        }
        lines.clear();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public InventoryReceiptType getType() {
        return type;
    }

    public void setType(InventoryReceiptType type) {
        this.type = type;
    }

    public InventoryReceiptStatus getStatus() {
        return status;
    }

    public void setStatus(InventoryReceiptStatus status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public User getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(User approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getRejectedReason() {
        return rejectedReason;
    }

    public void setRejectedReason(String rejectedReason) {
        this.rejectedReason = rejectedReason;
    }

    public List<InventoryReceiptLine> getLines() {
        return lines;
    }

    public void setLines(List<InventoryReceiptLine> lines) {
        this.lines = lines;
    }
}
