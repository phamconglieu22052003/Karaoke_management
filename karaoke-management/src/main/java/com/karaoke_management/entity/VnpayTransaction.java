package com.karaoke_management.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vnpay_transactions")
public class VnpayTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="invoice_id", nullable = false)
    private Long invoiceId;

    @Column(name="txn_ref", nullable = false, length = 80)
    private String txnRef;

    @Column(name="amount_vnd", nullable = false)
    private Long amountVnd;

    @Column(name="stage", nullable = false, length = 30)
    private String stage; // CREATED / RETURN / IPN_SUCCESS / IPN_FAILED / INVALID_SIGNATURE

    @Column(name="response_code", length = 10)
    private String responseCode;

    @Column(name="transaction_no", length = 30)
    private String transactionNo;

    @Lob
    @org.hibernate.annotations.Nationalized
    @Column(name = "raw_params", columnDefinition = "NVARCHAR(MAX)")
    private String rawParams;


    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ===== getter/setter =====
    public Long getId() { return id; }

    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }

    public String getTxnRef() { return txnRef; }
    public void setTxnRef(String txnRef) { this.txnRef = txnRef; }

    public Long getAmountVnd() { return amountVnd; }
    public void setAmountVnd(Long amountVnd) { this.amountVnd = amountVnd; }

    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }

    public String getResponseCode() { return responseCode; }
    public void setResponseCode(String responseCode) { this.responseCode = responseCode; }

    public String getTransactionNo() { return transactionNo; }
    public void setTransactionNo(String transactionNo) { this.transactionNo = transactionNo; }

    public String getRawParams() { return rawParams; }
    public void setRawParams(String rawParams) { this.rawParams = rawParams; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
