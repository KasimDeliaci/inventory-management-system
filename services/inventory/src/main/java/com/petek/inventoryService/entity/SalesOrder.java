package com.petek.inventoryService.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "sales_orders")
public class SalesOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sales_order_id")
    private Long salesOrderId;
    
    @Column(name = "customer_id", nullable = false)
    private Long customerId;
    
    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;
    
    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;
    
    @Column(name = "delivered_at")
    private Instant deliveredAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private SalesOrderStatus status;
    
    @Column(name = "customer_special_offer_id")
    private Long customerSpecialOfferId;
    
    @Column(name = "customer_discount_pct_applied", precision = 5, scale = 2)
    private BigDecimal customerDiscountPctApplied;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum SalesOrderStatus {
        PENDING,
        ALLOCATED,
        IN_TRANSIT,
        DELIVERED,
        CANCELLED
    }
}