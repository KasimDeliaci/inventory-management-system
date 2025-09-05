package com.petek.inventoryService.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "product_suppliers")
public class ProductSupplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_supplier_id")
    private Long productSupplierId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "min_order_quantity", nullable = false)
    private BigDecimal minOrderQuantity;

    @Column(name = "is_preferred", nullable = false)
    private Boolean isPreferred;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "avg_lead_time_days")
    private BigDecimal avgLeadTimeDays;

    @Column(name = "avg_delay_days")
    private BigDecimal avgDelayDays;

    @Column(name = "total_orders_count", nullable = false)
    private Integer totalOrdersCount;

    @Column(name = "delayed_orders_count", nullable = false)
    private Integer delayedOrdersCount;

    @Column(name = "last_delivery_date")
    private LocalDate lastDeliveryDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}