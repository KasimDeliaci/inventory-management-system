package com.petek.inventoryService.entity;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "stock_movements")
public class StockMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movement_id")
    private Long movementId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_kind", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private MovementKind movementKind;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_source", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private MovementSource movementSource;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "source_item_id")
    private Long sourceItemId;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "movement_date", nullable = false)
    private Instant movementDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public enum MovementKind {
        PURCHASE_RECEIPT,
        SALE_SHIPMENT,
        ADJUSTMENT_IN,
        ADJUSTMENT_OUT
    }

    public enum MovementSource {
        PURCHASE_ORDER,
        SALES_ORDER,
        ADJUSTMENT
    }
}
